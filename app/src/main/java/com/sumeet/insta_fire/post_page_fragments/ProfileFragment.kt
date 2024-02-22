package com.sumeet.insta_fire.post_page_fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentProfileBinding
import com.sumeet.insta_fire.main_page_fragments.MainFragment
import com.sumeet.insta_fire.models.Post
import com.sumeet.insta_fire.models.User
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment() {

    lateinit var profileBinding: FragmentProfileBinding
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val PROFILE_PHOTO_CODE = 1234
    }

    private var photoUri: Uri? = null
    private lateinit var storageRef: StorageReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        profileBinding = FragmentProfileBinding.inflate(layoutInflater)
        val view = profileBinding.root
        val toolbar = (activity as AppCompatActivity).findViewById<MaterialToolbar>(R.id.toolBar)
        toolbar.title = "Profile"

        storageRef = FirebaseStorage.getInstance().reference

        GlobalScope.launch(Dispatchers.IO) {
            val userDao = UserDao()
            val user = userDao.getUserId(auth.currentUser!!.uid).await().toObject(User::class.java)
            // Switch to the main thread before using Glide
            withContext(Dispatchers.Main) {
                Glide.with(requireActivity())
                    .load(user?.imageUrl)
                    .apply(RequestOptions().transform(CircleCrop()))
                    .into(profileBinding.profileImage)

                profileBinding.tvEmail.text = user!!.email
                profileBinding.tvUniqueId.text = "Unique Id:- ${user!!.uniqueId}"

                profileBinding.profileProgressBar.visibility = View.GONE
                profileBinding.proBarProfile.visibility = View.GONE
            }


        }


        profileBinding.profileImage.setOnClickListener {
            val imagePickerIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerIntent.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
            imagePickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            imagePickerIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (imagePickerIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(imagePickerIntent, PROFILE_PHOTO_CODE)
            }
        }

        profileBinding.btnRemoveProfileImage.setOnClickListener {
            removeImage()
        }

        profileBinding.btnFriendsList.setOnClickListener {
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val ft: FragmentTransaction = fm.beginTransaction()
            val friendProfileList = FriendProfileFragment()
            ft.replace(R.id.postFrame, friendProfileList)
            ft.addToBackStack(null)
            ft.commit()
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {

            PROFILE_PHOTO_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        launchImageCrop(it)

                    }
                } else {
                    Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == Activity.RESULT_OK) {
                    result.uri?.let { uri ->
                        Toast.makeText(context, "Cropping!!", Toast.LENGTH_SHORT).show()
                        photoUri = uri
                        if (photoUri == null) {
                            Toast.makeText(context, "No Photo Selected", Toast.LENGTH_SHORT).show()
                            return
                        }
                        // Handing the update image

                        profileBinding.profileImage.isClickable = false
                        profileBinding.proBarProfile.visibility = View.VISIBLE
                        val photoUploadUri = photoUri as Uri
                        Toast.makeText(context, "Setting the Profile Photo", Toast.LENGTH_SHORT)
                            .show()
                        // upload Photo to Firebase
                        val imageFileName = "profile_images/${System.currentTimeMillis()}-photo.jpg"
                        val photoRef = storageRef.child(imageFileName).putFile(photoUploadUri)
                        photoRef.addOnSuccessListener {
                            val imageUrl = storageRef.child(imageFileName).downloadUrl

                            imageUrl.addOnSuccessListener {
                                Toast.makeText(context, "Set Successfully", Toast.LENGTH_SHORT)
                                    .show()
                                updateFirestore(it.toString())

                            }.addOnFailureListener {
                                Toast.makeText(context, "Failed to set", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed!!", Toast.LENGTH_SHORT).show()
                        }

                    }
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Toast.makeText(context, "Failed! to Crop", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun updateFirestore(newUrl: String) {
        val map = mutableMapOf<String, Any?>()
        GlobalScope.launch(Dispatchers.IO) {
            val db = FirebaseFirestore.getInstance()
            val userCollections = db.collection("users")
            val userDao = UserDao()
            val user = userDao.getUserId(auth.currentUser!!.uid).await().toObject(User::class.java)
            Log.e("ProfileFragment", "User image url -> ${user!!.imageUrl}")
            Log.e("IMAGE Removed", "Removing-3")
            if(user.imageUrl != "null" && user.imageUrl != "") {
                val userProfileImageUri = Uri.parse(user!!.imageUrl)
                val fileRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(userProfileImageUri.toString())
                Log.e("IMAGE Removed", "Removing-5")

                fileRef.delete().addOnSuccessListener {
                    // Successfully deleted the image,
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Image Removed!", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("IMAGE Removed", "Removing-6")
                }.addOnFailureListener {
                    // Failed to delete the image
                    Log.e("IMAGE Removed", "Removing-7 failed")
                }
            }
            map["imageUrl"] = newUrl
            userCollections.document(auth.currentUser!!.uid).set(
                map,
                SetOptions.merge()
            ).addOnSuccessListener {
                // Successfully updated in Firestore
                Log.e("Error Message", "newUrl ->${newUrl}")
                if (newUrl != null) {
                    // If newUrl is not null, set the image using Glide
                    Glide.with(requireActivity())
                        .load(newUrl)
                        .apply(RequestOptions().transform(CircleCrop()))
                        .into(profileBinding.profileImage)
                }
                updateImageUrlInPosts(user.email, newUrl)
                profileBinding.proBarProfile.visibility = View.GONE
                val sharedPreferences =
                    requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().remove(MainFragment.SIGN_IN_METHOD_KEY).apply()
            }.addOnFailureListener {
                // Failed to update in Firestore
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Failed to update in Firestore", Toast.LENGTH_SHORT)
                        .show()
                    profileBinding.proBarProfile.visibility = View.GONE
                }
            }

        }
        profileBinding.profileImage.isClickable = true
    }

    fun updateImageUrlInPosts(userEmail: String, newImageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val postsCollection = db.collection("posts")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = postsCollection
                    .whereEqualTo("createdBy.email", userEmail)
                    .get()
                    .await()

                for (document in querySnapshot.documents) {
                    val post = document.toObject(Post::class.java)

                    // Update imageUrl in createdBy
                    post?.createdBy?.imageUrl = newImageUrl

                    // Update the document in Firestore
                    postsCollection.document(document.id).set(post!!, SetOptions.merge())
                        .addOnSuccessListener {
                            // Successfully updated in Firestore
                            Log.e("UpdateImageUrl", "Image URL updated in post: ${document.id}")
                        }
                        .addOnFailureListener { e ->
                            // Failed to update in Firestore
                            Log.e(
                                "UpdateImageUrl",
                                "Failed to update image URL in post: ${document.id}",
                                e
                            )
                        }
                }
            } catch (e: Exception) {
                Log.e("UpdateImageUrl", "Error while updating image URL in posts", e)
            }
        }
    }


    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1920, 1600)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(requireActivity(), this)
    }

    fun removeImage() {
        val userDao = UserDao()
        Log.e("IMAGE Removed", "Removing-1")
        GlobalScope.launch {
            Log.e("IMAGE Removed", "Removing-2")
            val user = userDao.getUserId(auth.currentUser!!.uid).await().toObject(User::class.java)
            Log.e("IMAGE Removed", "Removing-3")
            if (!user?.imageUrl.isNullOrEmpty()) {
                Log.e("IMAGE Removed", "Removing-4")
                val userProfileImageUri = Uri.parse(user!!.imageUrl)
                val fileRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(userProfileImageUri.toString())
                Log.e("IMAGE Removed", "Removing-5")
                fileRef.delete().addOnSuccessListener {
                    // Successfully deleted the image,
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Image Removed!", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("IMAGE Removed", "Removing-6")
                    updateFirestore("")
                    updateImageUrlInPosts(user.email,"")
                }.addOnFailureListener {
                    // Failed to delete the image
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // No image URL, nothing to delete
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "There is no image already!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}