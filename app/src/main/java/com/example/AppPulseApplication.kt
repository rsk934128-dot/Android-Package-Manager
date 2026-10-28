package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class AppPulseApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    // Ensure FirebaseApp is safely initialized on startup
    runCatching {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this)
      }
    }.onFailure { e ->
      Log.w("AppPulseApplication", "FirebaseApp initialization: ${e.localizedMessage}")
    }
  }
}
