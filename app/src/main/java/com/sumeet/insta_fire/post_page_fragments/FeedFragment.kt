package com.sumeet.insta_fire.post_page_fragments


import android.content.Context
import android.os.Bundle
import android.os.Parcelable
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.adapter.FeedAdapter
import com.sumeet.insta_fire.adapter.onItemClick
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.databinding.FragmentFeedBinding
import com.sumeet.insta_fire.models.Post
import com.sumeet.insta_fire.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class FeedFragment : Fragment(), onItemClick {


    lateinit var feedBinding: FragmentFeedBinding

    private var fragmentContext: Context? = null

    private lateinit var auth: FirebaseAuth

    private lateinit var feedAdapter: FeedAdapter
    private lateinit var postDao: PostDao

    val postList = ArrayList<Post>()
    private var emails = ArrayList<String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        feedBinding = FragmentFeedBinding.inflate(layoutInflater)
        val view = feedBinding.root
        val toolbar = (activity as AppCompatActivity).findViewById<MaterialToolbar>(R.id.toolBar)
        toolbar.title = "Feed"

        auth = Firebase.auth

        Log.e("FeedFragment","View Refresh-1")
        feedRecyclerView()
        Log.e("FeedFragment","View Refresh-2")

        feedBinding.fabCreate.setOnClickListener {
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val ft: FragmentTransaction = fm.beginTransaction()
            val createPostFragment = CreatePostFragment()
            ft.replace(R.id.postFrame, createPostFragment)
            ft.addToBackStack(null)
            ft.commit()
        }

        Log.e("FeedFragment","View Refresh-3")

        return view
    }



    fun feedRecyclerView() {
        Log.e("FeedFragment","View Refresh-4")
        postDao = PostDao()
        val postsCollection = postDao.postsCollection

        // Assuming the current user's UID is available (replace "currentUserUid" with the actual UID)
        val currentUserUid = Firebase.auth.currentUser?.uid

        // Get the user's friend list

        val userDocument = Firebase.firestore.collection("users").document(currentUserUid!!)
        userDocument.get().addOnSuccessListener { documentSnapshot ->
            Log.e("FeedFragment","View Refresh-5")
            if (documentSnapshot.exists()) {
                Log.e("FeedFragment","View Refresh-6")
                val user = documentSnapshot.toObject(User::class.java)
                val friendList = user?.friendList ?: emptyList()
                Log.e("FeedFragment", "User -> $user")

                if (friendList.isNotEmpty()) {
                    Log.e("FeedFragment","View Refresh-7")
                    // Use the friendList to filter posts
                    emails = friendList as ArrayList<String>
                    val signInUser = Firebase.auth.currentUser?.email
                    // Include the current user's email in the filter
                    emails.add(signInUser!!)

                    // Create a query to filter posts where the email is in the list
                    val query = postsCollection.whereIn("createdBy.email", emails)
                        .orderBy("createdAt", Query.Direction.DESCENDING)

                    // Observe the query results
                    query.addSnapshotListener { value, error ->
                        Log.e("FeedFragment","View Refresh-8")
                        if (error != null) {
                            // Handle the error
                            Log.e("FeedFragment", "Error getting friend list users: $error")
                            return@addSnapshotListener
                        }
                        postList.clear()
                        // Iterate through the query results and add them to the postList
                        for (document in value!!) {
                            val post = document.toObject(Post::class.java)
                            postList.add(post)
                            Log.e("FeedFragment", "Error Adapter:- ${postList}")
                        }
                        Log.e("FeedFragment","View Refresh-9")
                        // Update the adapter
                        if (fragmentContext != null) {
                            feedAdapter = FeedAdapter(fragmentContext!!, postList, this)
                            feedBinding.recyclerViewFeed.adapter = feedAdapter
                            feedBinding.recyclerViewFeed.layoutManager = LinearLayoutManager(fragmentContext!!)
                        } else {
                            Log.e("FeedFragment", "Fragment context is null")
                            // Handle the case when fragmentContext is null, such as displaying an error message or taking appropriate action.
                        }
                        Log.e("FeedFragment","View Refresh-10")
                    }

                } else {
                    // Use the friendList to filter posts
                    Log.e("FeedFragment","View Refresh-11")
                    emails = friendList as ArrayList<String>
                    val signInUser = Firebase.auth.currentUser?.email
                    // Include the current user's email in the filter
                    emails.add(signInUser!!)

                    // Create a query to filter posts where the email is in the list
                    val query = postsCollection.whereIn("createdBy.email", emails)
                        .orderBy("createdAt", Query.Direction.DESCENDING)

                    // Observe the query results
                    query.addSnapshotListener { value, error ->
                        Log.e("FeedFragment","View Refresh-12")
                        if (error != null) {
                            // Handle the error
                            Log.e("FeedFragment", "Error getting friend list users: $error")
                            return@addSnapshotListener
                        }
                        postList.clear()
                        // Iterate through the query results and add them to the postList
                        Log.e("FeedFragment","View Refresh-13")
                        for (document in value!!) {
                            Log.e("FeedFragment","View Refresh-14")
                            val post = document.toObject(Post::class.java)
                            postList.add(post)
                            Log.e("FeedFragment", "Error Adapter:- ${postList}")
                        }
                        Log.e("FeedFragment","View Refresh-15")
                        if (fragmentContext != null) {
                            feedAdapter = FeedAdapter(fragmentContext!!, postList, this)
                            feedBinding.recyclerViewFeed.adapter = feedAdapter
                            feedBinding.recyclerViewFeed.layoutManager = LinearLayoutManager(fragmentContext!!)
                        } else {

                            Log.e("FeedFragment", "Fragment context is null")
                            // Handle the case when fragmentContext is null, such as displaying an error message or taking appropriate action.
                        }
                        Log.e("FeedFragment","View Refresh-16")

                    }
                }
            }
        }

//        // Update the adapter
//        if (fragmentContext != null) {
//            feedAdapter = FeedAdapter(fragmentContext!!, postList, this)
//            feedBinding.recyclerViewFeed.adapter = feedAdapter
//            feedBinding.recyclerViewFeed.layoutManager = LinearLayoutManager(fragmentContext!!)
//        } else {
//            Log.e("FeedFragment", "Fragment context is null")
//            // Handle the case when fragmentContext is null, such as displaying an error message or taking appropriate action.
//        }
        //feedBinding.recyclerViewFeed.adapter = feedAdapter
        Log.e("FeedFragment","View Refresh-17")

    }




    override fun onUpdateClicked(postId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val postElement = postDao.getPostId(postId).await().toObject(Post::class.java)
            if (auth.currentUser!!.uid == postElement!!.createdBy.uid) {
                val fm: FragmentManager = requireActivity().supportFragmentManager
                val ft: FragmentTransaction = fm.beginTransaction()
                val bundle = Bundle()
                bundle.putString("postId", postId)
                val updatePostFragment = UpdatePostFragment()
                updatePostFragment.arguments = bundle
                ft.replace(R.id.postFrame, updatePostFragment)
                ft.addToBackStack(null)
                ft.commit()

            } else {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Access Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e("FeedFragment","View Refresh-19")
        fragmentContext = context
    }

    override fun onDetach() {
        super.onDetach()
        Log.e("FeedFragment","View Refresh-20")
        fragmentContext = null
    }
}

