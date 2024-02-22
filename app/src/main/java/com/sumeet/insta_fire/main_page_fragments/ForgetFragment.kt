package com.sumeet.insta_fire.main_page_fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.databinding.FragmentForgetBinding


class ForgetFragment : Fragment() {

    lateinit var forgetBinding: FragmentForgetBinding

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        forgetBinding = FragmentForgetBinding.inflate(layoutInflater)
        val view = forgetBinding.root

        forgetBinding.resetPasswordButton.setOnClickListener {
            val email = forgetBinding.editTextEmailForget.text.toString()
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "We Sent a Password reset mail to your mail address",
                        Toast.LENGTH_LONG
                    ).show()
                    val fm: FragmentManager = requireActivity().supportFragmentManager
                    fm.popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        task.exception?.message ?: "Reset password failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return view
    }
}
