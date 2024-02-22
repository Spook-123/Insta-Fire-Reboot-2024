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
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sumeet.insta_fire.R

import com.sumeet.insta_fire.adapter.UsersAdapter
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentFriendProfileBinding
import com.sumeet.insta_fire.models.Post
import com.sumeet.insta_fire.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendProfileFragment : Fragment() {

    lateinit var friendProfileBinding:FragmentFriendProfileBinding
    private lateinit var usersAdapter:UsersAdapter
    val userList = ArrayList<User>()
    private lateinit var userDao: UserDao
//    private lateinit var user: MutableList<User>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        friendProfileBinding = FragmentFriendProfileBinding.inflate(layoutInflater)
        val view = friendProfileBinding.root
        val toolbar = (activity as AppCompatActivity).findViewById<MaterialToolbar>(R.id.toolBar)
        toolbar.title = "Friends List"
        setUpUsersRecyclerViewNew()


        return view
    }

    fun setUpUsersRecyclerViewNew() {
        userDao = UserDao()
        val currentUserUid = Firebase.auth.currentUser?.uid

        // Get the user's friend list
        val userDocument = Firebase.firestore.collection("users").document(currentUserUid!!)
        userDocument.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val user = documentSnapshot.toObject(User::class.java)
                val friendList = user?.friendList ?: emptyList()

                if (friendList.isNotEmpty()) {
                    // Create a query to get users based on the friend list
                    val query = userDao.usersCollection.whereIn("email", friendList)

                    // Observe the query results
                    query.addSnapshotListener { value, error ->
                        if (error != null) {
                            // Handle the error
                            Log.e("Error Log", "Error getting friend list users: $error")
                            return@addSnapshotListener
                        }

                        // Clear existing data in the userList
                        userList.clear()

                        // Iterate through the query results and add them to the userList
                        for (document in value!!) {
                            val user = document.toObject(User::class.java)
                            userList.add(user)
                        }

                        // Update the adapter
                        usersAdapter.notifyDataSetChanged()
                    }
                } else {
                    // Handle the case where friendList is empty (show a message or use a default query)
                    // Example: Show a Toast
                    Toast.makeText(context, "Friend list is empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Create and set up the RecyclerView with the UsersAdapter
        usersAdapter = UsersAdapter(requireContext(), userList)
        friendProfileBinding.recyclerViewProfile.adapter = usersAdapter
        friendProfileBinding.recyclerViewProfile.layoutManager = LinearLayoutManager(context)
    }



}