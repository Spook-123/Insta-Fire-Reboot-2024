package com.sumeet.insta_fire.main_page_fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.activities.PostActivity
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentLoginBinding
import com.sumeet.insta_fire.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

class LoginFragment : Fragment() {

    lateinit var loginBinding:FragmentLoginBinding

    // Firebase
    val auth:FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        loginBinding = FragmentLoginBinding.inflate(layoutInflater)
        val view = loginBinding.root



        loginBinding.btnLogin.setOnClickListener {
            val userEmail = loginBinding.etEmail.text.toString()
            val userPassword = loginBinding.etPassword.text.toString()
            signinWithFirebase(userEmail,userPassword)
        }



        loginBinding.btnForgetPassword.setOnClickListener {
            val fm:FragmentManager = requireActivity().supportFragmentManager
            val ft:FragmentTransaction = fm.beginTransaction()
            val forgetFragment = ForgetFragment()
            ft.replace(R.id.frame,forgetFragment)
            ft.addToBackStack(null)
            ft.commit()
        }



        return view
    }


    fun signinWithFirebase(userEmail: String, userPassword: String) {
        auth.signInWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.uid!!

                // Check if the user with the given UID already exists
                val userDao = UserDao()
                userDao.getUserId(uid).addOnCompleteListener { userTask ->
                    if (userTask.isSuccessful) {
                        val existingUser = userTask.result?.toObject(User::class.java)
                        if (existingUser == null) {
                            // User does not exist, proceed to create a new user
                            val uuid = UUID.randomUUID()
                            val uniqueId = uuid.toString().substring(0, 6)
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token:String = task.getResult()
                                    Log.i("Main Activity", token)
                                    val user = User(token,uid, userEmail, "", uniqueId, arrayListOf())
                                    userDao.addUser(user)
                                    Toast.makeText(context, "Login Successfully", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(context, PostActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)

                                }
                                else {
                                    Toast.makeText(activity, "FCM Token Failed to Generate", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // User already exists, do something (e.g., show a message)
                            val intent = Intent(context, PostActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            Toast.makeText(context, "User already exists", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Handle the case where querying the user fails
                        Toast.makeText(context, "Error querying user: ${userTask.exception}", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                Toast.makeText(context, "Credentials are incorrect", Toast.LENGTH_SHORT).show()
            }

        }
    }




}


