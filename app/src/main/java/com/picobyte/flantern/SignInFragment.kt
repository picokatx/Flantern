package com.picobyte.flantern

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.picobyte.flantern.databinding.FragmentSignInBinding
import com.picobyte.flantern.utils.navigateTo
import androidx.core.content.ContextCompat.getSystemService

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat


class SignInFragment : Fragment() {
    lateinit var binding: FragmentSignInBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        (binding.root.context as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = getString(R.string.sign_in_title)
        binding.googleSignInButton.setOnClickListener {
            (binding.root.context as MainActivity).authGoogle.signIn {
                Toast.makeText(binding.root.context, "Signed in with Google", Toast.LENGTH_SHORT)
                    .show()
                (context as MainActivity).requests.getUser((context as MainActivity).authGoogle.getUID(),
                    {
                        Log.e("Flantern", (context as MainActivity).isServiceRunning.toString())
                        if (!(context as MainActivity).isServiceRunning) {
                            val service = Intent(context, FeedService::class.java)
                            service.putExtra("user_uid", (context as MainActivity).authGoogle.getUID())
                            context!!.startService(service)
                        }
                        navigateTo(binding.root, R.id.action_global_HomeFragment)
                    },
                    {
                        if (!(context as MainActivity).isServiceRunning) {
                            val service = Intent(context, FeedService::class.java)
                            service.putExtra("user_uid", (context as MainActivity).authGoogle.getUID())
                            context!!.startService(service)
                        }
                        (context as MainActivity).requests.createNewUser {
                            navigateTo(binding.root, R.id.action_global_HomeFragment)
                        }
                    }
                )
                /*(context as MainActivity).rtDatabase.getReference("user").get()
                    .addOnCompleteListener {
                        if (it.result.hasChild((context as MainActivity).authGoogle.getUID())) {
                            navigateTo(binding.root, R.id.action_global_HomeFragment)
                        } else {
                            (context as MainActivity).rtDatabase.getReference("user/${(context as MainActivity).authGoogle.getUID()}/static")
                                .apply {
                                    child("description").setValue("Hello Flantern!")
                                    child("email").setValue((context as MainActivity).authGoogle.getEmail())
                                    child("name").setValue((context as MainActivity).authGoogle.getDisplayName())
                                    child("status").setValue(0)
                                }
                            (context as MainActivity).rtDatabase.getReference("user/${(context as MainActivity).authGoogle.getUID()}/live")
                                .push().setValue(0)
                            navigateTo(binding.root, R.id.action_global_HomeFragment)
                        }
                    }*/
            }
        }
        binding.firebaseSignInButton.setOnClickListener {
            (binding.root.context as MainActivity).authGoogle.signInWithFirebase(
                binding.emailField.text.toString(),
                binding.passwordField.text.toString()
            ) { task ->
                run {
                    if (task.isSuccessful) {
                        Toast.makeText(
                            binding.root.context,
                            "Signed in with Firebase",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateTo(binding.root, R.id.action_global_HomeFragment)
                    } else {
                        Toast.makeText(
                            binding.root.context,
                            "Sign in to Firebase Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        (binding.root.context as AppCompatActivity).menuInflater.inflate(R.menu.menu_main, menu)
    }
}