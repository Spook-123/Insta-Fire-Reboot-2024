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
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.adapter.FeedAdapter
import com.sumeet.insta_fire.adapter.onItemClick
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.databinding.FragmentProfilePostBinding
import com.sumeet.insta_fire.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class ProfilePostFragment : Fragment(), onItemClick {


    lateinit var profilePostBinding:FragmentProfilePostBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var postDao: PostDao
    private lateinit var feedAdapter:FeedAdapter
    val postList = ArrayList<Post>()
    private val signInUser = Firebase.auth.currentUser!!.email


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        profilePostBinding = FragmentProfilePostBinding.inflate(layoutInflater)
        val view = profilePostBinding.root
        val toolbar = (activity as AppCompatActivity).findViewById<MaterialToolbar>(R.id.toolBar)
        toolbar.title = "Profile Posts"


        auth = Firebase.auth

        setUpRecylerView()

        profilePostBinding.fabCreate.setOnClickListener {
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val ft: FragmentTransaction = fm.beginTransaction()
            val createPostFragment = CreatePostFragment()
            ft.replace(R.id.postFrame, createPostFragment)
            ft.addToBackStack(null)
            ft.commit()
        }
        return view
    }

    fun setUpRecylerView() {
//        post = mutableListOf()
        postDao = PostDao()
        val postsCollection = postDao.postsCollection
        var query = postsCollection.orderBy("createdAt", Query.Direction.DESCENDING)
        val email = signInUser
        if(email != null) {
            query = query.whereEqualTo("createdBy.email",email)
        }
        query.addSnapshotListener { value, error ->
            if (error != null) {
                // Handle the error
                Log.e("Error Log", "Error getting friend list users: $error")
                return@addSnapshotListener
            }
            postList.clear()

            // Iterate through the query results and add them to the postList
            for (document in value!!) {
                val post = document.toObject(Post::class.java)
                postList.add(post)
                Log.e("Adapter", "Error Adapter:- ${postList}")
            }
            // Update the adapter
            // Update the adapter
            feedAdapter = FeedAdapter(requireContext(), postList, this)
            profilePostBinding.recyclerViewFeed.adapter = feedAdapter
            profilePostBinding.recyclerViewFeed.layoutManager =
                LinearLayoutManager(requireContext())
        }
    }




    override fun onUpdateClicked(postId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val postElement = postDao.getPostId(postId).await().toObject(Post::class.java)
            if(auth.currentUser!!.uid == postElement!!.createdBy.uid) {
                val fm:FragmentManager = requireActivity().supportFragmentManager
                val ft:FragmentTransaction = fm.beginTransaction()
                val bundle = Bundle()
                bundle.putString("postId",postId)
                val updatePostFragment = UpdatePostFragment()
                updatePostFragment.arguments = bundle
                ft.replace(R.id.postFrame,updatePostFragment)
                ft.addToBackStack(null)
                ft.commit()

            }
            else {
                Toast.makeText(context,"Access Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }


}