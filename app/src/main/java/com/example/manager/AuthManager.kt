package com.example.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

  private val auth: FirebaseAuth by lazy {
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
      runCatching { com.google.firebase.FirebaseApp.initializeApp(context) }
    }
    FirebaseAuth.getInstance()
  }
  private val credentialManager: CredentialManager by lazy {
    CredentialManager.create(context)
  }

  companion object {
    private const val TAG = "AppPulseAuth"
    private const val FALLBACK_WEB_CLIENT_ID = "597177743295-mkpf8rludkuk0a8eu89glv1l89p2bac8.apps.googleusercontent.com"
  }

  /**
   * Safely retrieves the Google Web Client ID:
   * 1. Resolves dynamically from Google Services plugin generated resources (default_web_client_id) if present.
   * 2. Falls back gracefully to the project client ID if google-services.json is absent (e.g. in GitHub Actions CI).
   */
  fun getWebClientId(): String {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId != 0) {
      val resString = runCatching { context.getString(resId) }.getOrNull()
      if (!resString.isNullOrBlank()) {
        return resString
      }
    }
    return FALLBACK_WEB_CLIENT_ID
  }

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
    return runCatching {
      val webClientId = getWebClientId()

      // Strict requirement: single option per request for reliable presentation on API 36
      val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

      val result = credentialManager.getCredential(request = request, context = activity)
      val credential = result.credential

      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val idToken = googleIdTokenCredential.idToken

      val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = auth.signInWithCredential(firebaseCredential).await()
      val user = authResult.user ?: error("Firebase user is null after authentication")

      user
    }.recoverCatching { exception ->
      if (exception is GetCredentialCancellationException) {
        Log.w(TAG, "Interactive Google Sign-In was cancelled by user: ${exception.localizedMessage}")
      } else {
        Log.e(TAG, "Google Sign-In failed: ${exception.localizedMessage}", exception)
      }
      throw exception
    }
  }

  suspend fun trySilentSignIn(): Result<FirebaseUser> {
    return runCatching {
      val webClientId = getWebClientId()

      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

      val result = credentialManager.getCredential(request = request, context = context)
      val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
      val idToken = googleIdTokenCredential.idToken

      val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = auth.signInWithCredential(firebaseCredential).await()
      authResult.user ?: error("Silent user is null")
    }
  }

  suspend fun signOut() {
    runCatching {
      auth.signOut()
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
  }
}
