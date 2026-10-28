package com.example.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
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

  private val auth: FirebaseAuth?
    get() = runCatching {
      if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
        com.google.firebase.FirebaseApp.initializeApp(context)
      }
      FirebaseAuth.getInstance()
    }.getOrNull()

  private val credentialManager: CredentialManager by lazy {
    CredentialManager.create(context)
  }

  companion object {
    private const val TAG = "AppPulseAuth"
  }

  /**
   * Safely retrieves the Google Web Client ID:
   * 1. Resolves dynamically from Google Services plugin generated resources (default_web_client_id).
   * 2. Reflectively looks up R.string.default_web_client_id if needed.
   */
  fun getWebClientId(): String {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId != 0) {
      val resString = runCatching { context.getString(resId) }.getOrNull()
      if (!resString.isNullOrBlank()) {
        return resString
      }
    }
    return runCatching {
      val rField = com.example.R.string::class.java.getField("default_web_client_id")
      val id = rField.getInt(null)
      context.getString(id)
    }.getOrDefault("")
  }

  val currentUser: FirebaseUser?
    get() = auth?.currentUser

  fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val firebaseAuth = auth
    if (firebaseAuth == null) {
      trySend(null)
      awaitClose { }
      return@callbackFlow
    }
    val listener = FirebaseAuth.AuthStateListener { fa ->
      trySend(fa.currentUser)
    }
    firebaseAuth.addAuthStateListener(listener)
    trySend(firebaseAuth.currentUser)
    awaitClose {
      runCatching { firebaseAuth.removeAuthStateListener(listener) }
    }
  }

  suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
    return runCatching {
      val firebaseAuth = auth ?: error("Firebase Auth is not available on this device")
      val webClientId = getWebClientId()
      if (webClientId.isBlank()) {
        error("Google Web Client ID is not configured in google-services.json")
      }

      // 1. Try GetGoogleIdOption with filterByAuthorizedAccounts=false to prompt account picker
      val credential = try {
        val googleIdOption = GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(false)
          .setServerClientId(webClientId)
          .setAutoSelectEnabled(false)
          .build()

        val request = GetCredentialRequest.Builder()
          .addCredentialOption(googleIdOption)
          .build()

        credentialManager.getCredential(request = request, context = activity).credential
      } catch (e: Exception) {
        if (e is GetCredentialCancellationException) throw e
        Log.w(TAG, "GetGoogleIdOption exception (${e.localizedMessage}), attempting fallback with GetSignInWithGoogleOption")

        // 2. Fallback to GetSignInWithGoogleOption
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
          .build()
        val fallbackRequest = GetCredentialRequest.Builder()
          .addCredentialOption(signInOption)
          .build()

        credentialManager.getCredential(request = fallbackRequest, context = activity).credential
      }

      val idToken = try {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
      } catch (e: Exception) {
        credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
          ?: credential.data.getString("id_token")
          ?: error("Failed to extract ID token from Google Credential: ${e.localizedMessage}")
      }

      val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
      val user = authResult.user ?: error("Firebase user is null after authentication")

      user
    }.recoverCatching { exception ->
      when (exception) {
        is GetCredentialCancellationException -> {
          Log.i(TAG, "Interactive Google Sign-In was cancelled by user")
          throw Exception("Sign-in was cancelled by user")
        }
        is NoCredentialException -> {
          Log.w(TAG, "No Google accounts available on device: ${exception.localizedMessage}")
          throw Exception("No Google account found on device. Please sign in to Google in device Settings.")
        }
        else -> {
          Log.e(TAG, "Google Sign-In failed: ${exception.localizedMessage}", exception)
          throw exception
        }
      }
    }
  }

  suspend fun trySilentSignIn(): Result<FirebaseUser> {
    return runCatching {
      val firebaseAuth = auth ?: error("Firebase Auth is not available on this device")
      val webClientId = getWebClientId()
      if (webClientId.isBlank()) error("Web Client ID is empty")

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
      val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
      authResult.user ?: error("Silent user is null")
    }
  }

  suspend fun signOut() {
    runCatching {
      auth?.signOut()
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
  }
}
