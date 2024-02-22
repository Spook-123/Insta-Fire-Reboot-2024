package com.sumeet.insta_fire.post_page_fragments

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentSearchFriendBinding
import com.sumeet.insta_fire.models.User


class SearchFriendFragment : Fragment() {

    lateinit var searchFriendBinding:FragmentSearchFriendBinding
    val auth:FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        searchFriendBinding = FragmentSearchFriendBinding.inflate(layoutInflater)
        val view = searchFriendBinding.root

        val email = arguments?.getString("email")
        val imageUrl = arguments?.getString("imageUrl")
        searchFriendBinding.tvEmail.text = email
        if(imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions().transform(CircleCrop()))
                .placeholder(R.drawable.profile_image) // Placeholder image while loading
                .error(R.drawable.profile_image) // Error image if loading fails
                .into(searchFriendBinding.profileImage)
        }

        searchFriendBinding.btnAddFriend.setOnClickListener {
            val currentUserUid = auth.currentUser!!.uid
            if (currentUserUid != null) {
                addFriendToFirestore(currentUserUid, email)
            }
        }

        searchFriendBinding.profileImage.setOnClickListener {
            imageUrl?.let { it1 -> openFullScreenImage(it1) }
        }

        return view
    }

    private fun openFullScreenImage(imageUrl:String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_screen_image)

        val photoView = dialog.findViewById<PhotoView>(R.id.photoView)
        //photoView.setImageDrawable(drawable)
        // Load the full-size image into PhotoView without any transformations
        Glide.with(requireContext())
            .load(imageUrl)
            .into(photoView)

        // Enable zoom and pan
        val attacher = PhotoViewAttacher(photoView)
        attacher.update()

        // Close the dialog when tapped
        photoView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addFriendToFirestore(currentUserUid: String, friendEmail: String?) {
        if (friendEmail != null) {
            val firestore = FirebaseFirestore.getInstance()
            val userReference = firestore.collection("users").document(currentUserUid)

            // Check if the friendEmail is already in the friendList
            userReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    val friendList = documentSnapshot.toObject(User::class.java)?.friendList
                    if (friendList != null && friendList.contains(friendEmail)) {
                        // Friend already exists
                        Toast.makeText(context, "Friend already added", Toast.LENGTH_SHORT).show()
                    } else {
                        // Friend not found, add to friendList
                        userReference.update("friendList", FieldValue.arrayUnion(friendEmail))
                            .addOnSuccessListener {
                                // Handle success, e.g., show a toast or perform any other action
                                Toast.makeText(context, "Friend added successfully", Toast.LENGTH_SHORT).show()
                                val fm:FragmentManager = requireActivity().supportFragmentManager
                                val ft:FragmentTransaction = fm.beginTransaction()
                                val feedFragment = FeedFragment()
                                ft.replace(R.id.postFrame,feedFragment)
                                ft.commit()
                            }
                            .addOnFailureListener {
                                // Handle failure, e.g., show an error message
                                Toast.makeText(context, "Failed to add friend", Toast.LENGTH_SHORT).show()
                                val fm:FragmentManager = requireActivity().supportFragmentManager
                                val ft:FragmentTransaction = fm.beginTransaction()
                                val feedFragment = FeedFragment()
                                ft.replace(R.id.postFrame,feedFragment)
                                ft.commit()
                            }
                    }
                }
                .addOnFailureListener {
                    // Handle failure, e.g., show an error message
                    Toast.makeText(context, "Failed to check friend status", Toast.LENGTH_SHORT).show()
                }
        }
    }




}

