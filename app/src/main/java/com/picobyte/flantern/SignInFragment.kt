package com.picobyte.flantern

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.picobyte.flantern.databinding.FragmentSignInBinding
import com.picobyte.flantern.utils.navigateTo

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
                navigateTo(binding.root, R.id.action_global_HomeFragment)
            }
        }
        binding.firebaseSignInButton.setOnClickListener {
            (binding.root.context as MainActivity).authFirebase.signIn(
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