package com.picobyte.flantern

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.picobyte.flantern.databinding.FragmentSettingsBinding
import com.picobyte.flantern.types.User
import com.picobyte.flantern.utils.ONE_MEGABYTE

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.confirmBtn.visibility = View.INVISIBLE
        val adapter = ArrayAdapter.createFromResource(
            context as MainActivity,
            R.array.status_array,
            android.R.layout.simple_spinner_item
        )
        (context as MainActivity).requests.getUser(
            (context as MainActivity).authGoogle.getUID(),
            {
                var imageUID = it.profile
                binding.settingsName.setText(it.name)
                binding.settingsDescription.setText(it.description)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.settingsStatus.adapter = adapter
                binding.settingsStatus.setSelection(it.status!!)
                binding.settingsName.addTextChangedListener {
                    binding.confirmBtn.visibility = View.VISIBLE
                }
                binding.settingsDescription.addTextChangedListener {
                    binding.confirmBtn.visibility = View.VISIBLE
                }
                binding.settingsStatus.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            p0: AdapterView<*>?,
                            p1: View?,
                            p2: Int,
                            p3: Long
                        ) {
                            binding.confirmBtn.visibility = View.VISIBLE
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            return
                        }
                    }
                binding.settingsProfile.setOnClickListener {
                    //todo: add gallery intent
                    binding.confirmBtn.visibility = View.VISIBLE
                }
                (context as MainActivity).requests.getUserProfileBitmap(it.profile!!,
                    { bitmap ->
                        binding.topBarIcon.setImageBitmap(bitmap)
                        binding.settingsProfile.setImageBitmap(bitmap)
                    }
                )
                binding.confirmBtn.setOnClickListener {
                    (context as MainActivity).requests.setUserData(
                        User(
                            binding.settingsName.text.toString(),
                            null,
                            binding.settingsDescription.text.toString(),
                            binding.settingsStatus.selectedItemPosition,
                            imageUID
                        )
                    )
                }
            }
        )
        /*(requireActivity() as MainActivity).rtDatabase.getReference("/user/${(context as MainActivity).authGoogle.getUID()}")
            .get().addOnCompleteListener {
                val user = it.result.getValue(User::class.java)!!
                binding.settingsName.setText(user.name)
                binding.settingsDescription.setText(user.description)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.settingsStatus.adapter = adapter
                binding.settingsStatus.setSelection(user.status!!)
                var imageUid = user.profile
                (context as MainActivity).storage.getReference("users/${imageUid}.jpg")
                    .getBytes(ONE_MEGABYTE).addOnCompleteListener { image ->
                        val bitmap = BitmapFactory.decodeByteArray(
                            image.result,
                            0,
                            image.result.size
                        )
                        binding.topBarIcon.setImageBitmap(bitmap)
                        binding.settingsProfile.setImageBitmap(bitmap)
                    }

                binding.settingsName.addTextChangedListener {
                    binding.confirmBtn.visibility = View.VISIBLE
                }
                binding.settingsDescription.addTextChangedListener {
                    binding.confirmBtn.visibility = View.VISIBLE
                }
                binding.settingsStatus.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            p0: AdapterView<*>?,
                            p1: View?,
                            p2: Int,
                            p3: Long
                        ) {
                            binding.confirmBtn.visibility = View.VISIBLE
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            return
                        }
                    }
                binding.settingsProfile.setOnClickListener {
                    binding.confirmBtn.visibility = View.VISIBLE
                }
                binding.confirmBtn.setOnClickListener {
                    val ref =
                        (requireActivity() as MainActivity).rtDatabase.getReference("/user/${(context as MainActivity).authGoogle.getUID()}")
                    ref.child("name").setValue(binding.settingsName.text)
                    ref.child("description").setValue(binding.settingsDescription.text)
                    ref.child("profile").setValue(imageUid)
                    ref.child("status").setValue(binding.settingsStatus.selectedItemPosition)
                }
            }*/
        return binding.root
    }
}