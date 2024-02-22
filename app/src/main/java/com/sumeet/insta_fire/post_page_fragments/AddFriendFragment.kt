package com.sumeet.insta_fire.post_page_fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.databinding.FragmentAddFriendBinding
import com.sumeet.insta_fire.models.Post
import com.sumeet.insta_fire.models.User


class AddFriendFragment : Fragment() {

    lateinit var addFriendBinding:FragmentAddFriendBinding
    private lateinit var postDao: PostDao
    private lateinit var post: MutableList<Post>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        addFriendBinding = FragmentAddFriendBinding.inflate(layoutInflater)
        val view = addFriendBinding.root

        val toolbar = (activity as AppCompatActivity).findViewById<MaterialToolbar>(R.id.toolBar)
        toolbar.title = "Add Friends"

        addFriendBinding.progressBar.visibility = View.GONE

        addFriendBinding.btnSearch.setOnClickListener {
        addFriendBinding.progressBar.visibility = View.VISIBLE
            val searchTerm = addFriendBinding.etSearch.text.toString()
//            val searchTerm = "b1f753"
            post = mutableListOf()
            postDao = PostDao()
            val currentUserUid = Firebase.auth.currentUser?.uid

            // Assuming the current user's UID is available (replace "currentUserUid" with the actual UID)
            val usersCollection = Firebase.firestore.collection("users")

            // Query for the user with the provided uniqueId
            usersCollection.whereEqualTo("uniqueId", searchTerm).get().addOnSuccessListener { querySnapshot ->
                addFriendBinding.progressBar.visibility = View.GONE
                if (!querySnapshot.isEmpty) {
                    // User with the specified uniqueId found
                    addFriendBinding.etSearch.setText("")
                    val bundle = Bundle()
                    for(att in querySnapshot.documents) {
                        bundle.putString("email", att.data?.get("email").toString())
                        bundle.putString("imageUrl",att.data?.get("imageUrl").toString())
                    }
                    Log.e("Error Log", "Found -> ${querySnapshot}")
                    val fm:FragmentManager = requireActivity().supportFragmentManager
                    val ft:FragmentTransaction = fm.beginTransaction()
                    val searchedFriendFragment = SearchFriendFragment()
                    searchedFriendFragment.arguments = bundle
                    ft.replace(R.id.postFrame,searchedFriendFragment)
                    ft.addToBackStack(null)
                    ft.commit()

                } else {
                    // No user found with the specified uniqueId
                    addFriendBinding.etSearch.setText("")
                    Toast.makeText(context, "Not Found", Toast.LENGTH_SHORT).show()
                    //package:com.sumeet.insta_fire
                }
            }
        }


        return view
    }




}