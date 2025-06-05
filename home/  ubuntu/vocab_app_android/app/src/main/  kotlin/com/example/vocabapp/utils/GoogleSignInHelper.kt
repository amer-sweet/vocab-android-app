package com.example.vocabapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task

object GoogleSignInHelper {

    private const val TAG = "GoogleSignInHelper"
    // Request access to the user's ID and basic profile
    // Request access to the AppData folder
    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // Request user's email address
            // Request Drive AppData scope. IMPORTANT: Ensure this scope is enabled in your Google Cloud Console project.
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            // If you need offline access (server-side access or refresh tokens), request server auth code:
            // .requestServerAuthCode(YOUR_SERVER_CLIENT_ID)
            .build()
    }

    fun getClient(context: Context): GoogleSignInClient {
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent {
        return getClient(context).signInIntent
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>): GoogleSignInAccount? {
        return try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful for: ${account?.email}")
            account // Returns the signed-in account
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "Google Sign-In failed, status code: ${e.statusCode}", e)
            null
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        getClient(context).signOut()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Google Sign-Out successful.")
                } else {
                    Log.w(TAG, "Google Sign-Out failed.")
                }
                onComplete()
            }
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun isUserSignedIn(context: Context): Boolean {
        return getLastSignedInAccount(context) != null
    }
}
