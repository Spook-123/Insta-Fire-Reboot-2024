package com.sumeet.insta_fire.main_page_fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.activities.PostActivity
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentMainBinding
import com.sumeet.insta_fire.models.User
import java.util.UUID


class MainFragment : Fragment() {

    lateinit var mainFragmentBinding: FragmentMainBinding

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 4566
        const val SIGN_IN_METHOD_KEY = "sign_in_method"
    }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mainFragmentBinding = FragmentMainBinding.inflate(layoutInflater)
        val view = mainFragmentBinding.root


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission has not been granted yet, request it
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 99)
            } else {
                // Permission already granted
                // You can perform any necessary actions here
            }
        }



        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        mainFragmentBinding.loginButton.setOnClickListener {
            val loginFragment = LoginFragment()
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val ft: FragmentTransaction = fm.beginTransaction()
            ft.replace(R.id.frame, loginFragment)
            ft.addToBackStack(null)
            ft.commit()
        }

        mainFragmentBinding.btnSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }

        mainFragmentBinding.signupButton.setOnClickListener {
            val signupFragment = SignupFragment()
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val ft: FragmentTransaction = fm.beginTransaction()
            ft.replace(R.id.frame, signupFragment)
            ft.addToBackStack(null)
            ft.commit()
        }


        return view
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(requireContext(), "Allowed", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        //Check if the user is signed in and update th UI accordingly
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            //Google SignIn Failed,update UI Accordingly
            Log.w("TAG", "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mainFragmentBinding.btnSignIn.isClickable = false
        mainFragmentBinding.progressBarGoogle.visibility = View.VISIBLE
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.e("MainFragment", "signInWithCredential:success")
                val user = auth.currentUser
                updateUI(user)

            } else {
                // If sign in fails, display a message to the user.
                Log.e("MainFragment", "signInWithCredential:failure", task.exception)
                updateUI(null)
            }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser == null) {
            mainFragmentBinding.btnSignIn.isClickable = true
            mainFragmentBinding.progressBarGoogle.visibility = View.GONE
            return
        }
        val uid = auth.uid!!

        // Check if the user with the given UID already exists
        val userDao = UserDao()
        userDao.getUserId(uid).addOnCompleteListener { userTask ->
            if (userTask.isSuccessful) {
                val existingUser = userTask.result?.toObject(User::class.java)
                Log.e("User Log","Existing user -> $existingUser")
                if (existingUser == null) {
                    // User does not exist, proceed to create a new user
                    val uuid = UUID.randomUUID()
                    val uniqueId = uuid.toString().substring(0, 6)
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token:String = task.getResult()
                            Log.i("Main Activity", token)
                            val user = User(
                                token,
                                uid,
                                auth.currentUser!!.email!!,
                                "",
                                uniqueId,
                                arrayListOf(),
                            )
                            userDao.addUser(user)
                            Toast.makeText(activity, "Login Successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, PostActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        else {
                            Toast.makeText(activity, "FCM Token Failed to Generate", Toast.LENGTH_SHORT).show()
                        }
                    }


                }
                else {
                    activity?.let { context ->
                        Toast.makeText(context, "Login Successfully", Toast.LENGTH_SHORT).show()
                    }
                    val context = context ?: return@addOnCompleteListener  // Check if the fragment is attached to a context
                    val intent = Intent(context, PostActivity::class.java)
                    startActivity(intent)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                }

            } else {
                // Handle the case where querying the user fails
                Toast.makeText(
                    context,
                    "Error querying user: ${userTask.exception}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }


}



