package com.example.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
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

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  private val credentialManager = CredentialManager.create(context)

  companion object {
    private const val TAG = "AppPulseAuth"
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
      val webClientId = context.getString(R.string.default_web_client_id)

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
      val webClientId = context.getString(R.string.default_web_client_id)

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
