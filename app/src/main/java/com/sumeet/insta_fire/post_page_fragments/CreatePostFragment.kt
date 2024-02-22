package com.sumeet.insta_fire.post_page_fragments

import android.app.Activity
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.adapter.UsersAdapter
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentCreatePostBinding
import com.sumeet.insta_fire.models.User
import com.sumeet.insta_fire.push_notification.FcmApi
import com.sumeet.insta_fire.push_notification.NotificationContent
import com.sumeet.insta_fire.push_notification.NotificationData
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class CreatePostFragment : Fragment() {


    lateinit var createPostBinding: FragmentCreatePostBinding

    companion object {
        private const val PICK_PHOTO_CODE = 1234
    }

    private var photoUri: Uri? = null
    private lateinit var storageReference: StorageReference
    private lateinit var postDao: PostDao
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        createPostBinding = FragmentCreatePostBinding.inflate(layoutInflater)
        return createPostBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar =
            (requireActivity() as AppCompatActivity).findViewById<MaterialToolbar>(R.id.toolBar)
        toolbar.title = "Create Post"

        createPostBinding.proBar.visibility = View.GONE
        postDao = PostDao()

        auth = Firebase.auth

        storageReference = FirebaseStorage.getInstance().reference

        createPostBinding.btnChooseImage.setOnClickListener {
            val imagePickerIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerIntent.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
            imagePickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            imagePickerIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (imagePickerIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(imagePickerIntent, PICK_PHOTO_CODE)
            }
        }

        createPostBinding.btnSubmit.setOnClickListener {
            handleSubmitButton()
        }

    }

    private fun handleSubmitButton() {
        if (photoUri == null) {
            Toast.makeText(context, "No Photo Selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (createPostBinding.etDescription.text.isBlank()) {
            Toast.makeText(context, "Description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (auth.currentUser == null) {
            Toast.makeText(context, "Invalid Sign in", Toast.LENGTH_SHORT).show()
            return
        }
        createPostBinding.btnSubmit.isClickable = false
        createPostBinding.proBar.visibility = View.VISIBLE
        val photoUploadUri = photoUri as Uri
        Toast.makeText(context, "Uploading please wait!", Toast.LENGTH_SHORT).show()
        // Upload photo to Firebase
        val imageFileName = "images/${System.currentTimeMillis()}-photo.jpg"
        val photoRef = storageReference.child(imageFileName).putFile(photoUploadUri)
        photoRef.addOnSuccessListener {
            val imageUrl = storageReference.child(imageFileName).downloadUrl

            imageUrl.addOnSuccessListener {
                Toast.makeText(context, "Uploaded Successfully", Toast.LENGTH_SHORT).show()
                createPostBinding.proBar.visibility = View.GONE
                val text = createPostBinding.etDescription.text.toString()
                val image = it.toString()
                postDao.addPost(text, image)
                sendNotification()
                val fm: FragmentManager = requireActivity().supportFragmentManager
                val ft: FragmentTransaction = fm.beginTransaction()
                val feedFragment = FeedFragment()
                ft.replace(R.id.postFrame, feedFragment)
                ft.commit()
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to upload", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed?--------", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {

            PICK_PHOTO_CODE -> {
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
                        createPostBinding.ivUpload.setImageURI(photoUri)
                    }
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Toast.makeText(context, "Failed! to Crop", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1920, 1600)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(requireActivity(), this)
    }


    fun sendNotification() {
        Log.e("CreatePostFragment", "Notification-4")
        val userDao = UserDao()
        val currentUserUid = Firebase.auth.currentUser?.uid

        // Get the user's friend list
        val userDocument = Firebase.firestore.collection("users").document(currentUserUid!!)
        userDocument.get().addOnSuccessListener { documentSnapshot ->
            Log.e("CreatePostFragment", "Notification-3")
            if (documentSnapshot.exists()) {
                val user = documentSnapshot.toObject(User::class.java)
                val friendList = user?.friendList ?: emptyList()

                if (friendList.isNotEmpty()) {
                    // Create a query to get users based on the friend list
                    val query = userDao.usersCollection.whereIn("email", friendList)
                    Log.e("CreatePostFragment", "Notification-2")

                    // Observe the query results
                    query.addSnapshotListener { value, error ->
                        if (error != null) {
                            // Handle the error
                            Log.e("Error Log", "Error getting friend list users: $error")
                            return@addSnapshotListener
                        }
                        val retrofit = Retrofit.Builder()
                            .baseUrl("https://fcm.googleapis.com/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val api = retrofit.create(FcmApi::class.java)
                        Log.e("CreatePostFragment", "Notification-1")

                        // Iterate through the query results and add them to the userList
                        for (document in value!!) {
                            val newUser = document.toObject(User::class.java)
                            for(friend in newUser.friendList) {
                                if(friend == user?.email) {
                                    val notification = NotificationData(
                                        to = newUser.fcmToken,
                                        notification = NotificationContent(
                                            "Insta-Fire Notification",
                                            "${newUser.email} has created a new post"
                                        ),
                                    )
                                    GlobalScope.launch(Dispatchers.IO) {
                                        val response = api.sendNotification(notification)
                                        if (response.isSuccessful) {
                                            Log.e(
                                                "CreatePostFragment",
                                                "Notification sent successfully to ${newUser.fcmToken}"
                                            )
                                            println("Notification sent successfully to ${newUser.fcmToken}")
                                        } else {
                                            println(
                                                "Failed to send notification to ${newUser.fcmToken}. Error: ${
                                                    response.errorBody()?.string()
                                                }"
                                            )
                                            Log.e(
                                                "CreatePostFragment",
                                                "Failed to send notification to ${newUser.fcmToken}. Error: ${
                                                    response.errorBody()?.string()
                                                }"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Handle the case where friendList is empty (show a message or use a default query)
                    // Example: Show a Toast
                    //Toast.makeText(context, "Friend list is empty", Toast.LENGTH_SHORT).show()
                    Log.e("CreatePostFragment", "Friend list is empty")
                }
            }
        }
    }


}