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
import com.sumeet.insta_fire.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    lateinit var signupbinding:FragmentSignupBinding

    // Firebase
    val auth:FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        signupbinding = FragmentSignupBinding.inflate(layoutInflater)
        val view = signupbinding.root

        signupbinding.btnSignup.setOnClickListener {
            val userEmail = signupbinding.etEmail.text.toString()
            val userPassword = signupbinding.etPassword.text.toString()
            signupWithFirebase(userEmail,userPassword)
        }

        return  view
    }

    fun signupWithFirebase(userEmail:String,userPassword:String) {
        auth.createUserWithEmailAndPassword(userEmail,userPassword).addOnCompleteListener { task ->
            if(task.isSuccessful) {
                Toast.makeText(context,"Your Account has been created",Toast.LENGTH_SHORT).show()
                val fm:FragmentManager = requireActivity().supportFragmentManager
                fm.popBackStack()
            }
            else {
                Toast.makeText(context,"Sign in Failed",Toast.LENGTH_SHORT).show()
            }
        }
    }

}