package com.picobyte.flantern.authentication

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class AuthFirebase(private val context: AppCompatActivity, private val auth: FirebaseAuth) {
    fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(context) { task ->
                if (task.isSuccessful) {
                    Log.d("Flantern", "createUserWithEmail:success")
                    val user = auth.currentUser
                } else {
                    Log.w("Flantern", "createUserWithEmail:failure", task.exception)
                }
            }
    }

    fun signIn(email: String, password: String, onSignIn: (result: Task<AuthResult>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(context) { task ->
                if (task.isSuccessful) {
                    Log.e("Flantern", "signInWithEmail:success")
                    val user = auth.currentUser
                } else {
                    Log.e("Flantern", "signInWithEmail:failure", task.exception)
                }
            }.addOnCompleteListener(onSignIn)
    }

    fun signOut() {
        auth.signOut()
    }
}