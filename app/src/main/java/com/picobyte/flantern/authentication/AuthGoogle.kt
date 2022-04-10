package com.picobyte.flantern.authentication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.picobyte.flantern.R

class AuthGoogle(private val context: AppCompatActivity, private val auth: FirebaseAuth) {
    private val gClient: GoogleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id)).requestEmail().build()
    )
    private lateinit var user: FirebaseUser
    private lateinit var signInHandler: () -> Unit
    private val resultLauncher =
        context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val gtask = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = gtask.getResult(ApiException::class.java)!!
                    Log.e("Flantern", "firebaseAuthWithGoogle:" + account.id)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val signInTask = auth.signInWithCredential(credential)
                    signInTask.addOnCompleteListener(context) { task ->
                        if (task.isSuccessful) {
                            Log.e("Flantern", "signInWithCredential:success")
                            user = auth.currentUser!!
                            signInHandler()
                            Log.e("Flantern", user.email!!)
                            Log.e("Flantern", user.displayName!!)
                            Log.e("Flantern", user.photoUrl.toString())
                        } else {
                            Log.e(
                                "Flantern",
                                "signInWithCredential:failure",
                                task.exception
                            )
                        }
                    }
                } catch (e: ApiException) {
                    Log.w("Flantern", "Google sign in failed", e)
                }
            }
        }
    fun signInWithFirebase(email: String, password: String, onSignIn: (result: Task<AuthResult>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(context) { task ->
                if (task.isSuccessful) {
                    Log.e("Flantern", "signInWithEmail:success")
                    user = auth.currentUser!!
                } else {
                    Log.e("Flantern", "signInWithEmail:failure", task.exception)
                }
            }.addOnCompleteListener(onSignIn)
    }

    fun createFirebaseAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(context) { task ->
                if (task.isSuccessful) {
                    Log.d("Flantern", "createUserWithEmail:success")
                    user = auth.currentUser!!
                } else {
                    Log.w("Flantern", "createUserWithEmail:failure", task.exception)
                }
            }
    }

    fun signOutWithFirebase() {
        auth.signOut()
    }
    fun getSignInClient(): GoogleSignInClient {
        return gClient
    }

    fun getUser(): FirebaseUser {
        return user
    }

    fun getEmail(): String {
        return user.email!!
    }

    fun getDisplayName(): String {
        return user.displayName!!

    }

    fun getPhotoURL(): Uri {
        return user.photoUrl!!

    }

    fun getUID(): String {
        return user.uid

    }

    fun getToken() {
        //return user.getIdToken(false)
    }

    fun onSignIn(signInHandler: () -> Unit) {
        this.signInHandler = signInHandler
    }

    fun signIn(signInHandler: () -> Unit) {
        this.signInHandler = signInHandler
        val signInIntent = gClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    fun signOut() {
        gClient.signOut()
        gClient.revokeAccess()
    }
}