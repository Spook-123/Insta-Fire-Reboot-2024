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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.FragmentUpdatePostBinding
import com.sumeet.insta_fire.models.Post
import com.sumeet.insta_fire.models.User
import com.sumeet.insta_fire.push_notification.FcmApi
import com.sumeet.insta_fire.push_notification.NotificationContent
import com.sumeet.insta_fire.push_notification.NotificationData
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class UpdatePostFragment : Fragment() {

    lateinit var updatePostBinding:FragmentUpdatePostBinding
    companion object {
        private const val PICK_PHOTO_CODE = 4568
    }
    private var photoUri: Uri? = null
    private lateinit var storageRef: StorageReference
    private var postId:String? = null
    private var flag = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        updatePostBinding = FragmentUpdatePostBinding.inflate(layoutInflater)
        val view = updatePostBinding.root

        storageRef = FirebaseStorage.getInstance().reference

        postId = arguments?.getString("postId").toString()

        GlobalScope.launch(Dispatchers.Main) {
            val postDao = PostDao()
            val post = postDao.getPostId(postId!!).await().toObject(Post::class.java)!!
            updatePostBinding.etDescriptionUpdate.setText(post.description)
            Glide.with(requireContext()).load(post.imageUrlPost).apply(
                RequestOptions().transform(
                    CenterCrop(), RoundedCorners(20)
                )
            ).into(updatePostBinding.ivUploadUpdate)
            updatePostBinding.proBarUpdate.visibility = View.GONE
        }


        updatePostBinding.btnChooseImageUpdate.setOnClickListener {
            val imagePickerIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerIntent.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg","image/png","image/jpg")
            imagePickerIntent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes)
            imagePickerIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if(imagePickerIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(imagePickerIntent, PICK_PHOTO_CODE)
            }
        }

        updatePostBinding.btnUpdate.setOnClickListener {
            updatePostBinding.btnUpdate.isEnabled = false
            if(flag == 1) {
                getImageUrl()
                updatePostBinding.proBarUpdate.visibility = View.VISIBLE
            }
            else {
                getNewDataWithoutImageChange()
                updatePostBinding.proBarUpdate.visibility = View.VISIBLE
            }
        }

        return view
    }

    private fun getNewDataWithoutImageChange() {
        val newTime = System.currentTimeMillis()
        val newDescription = updatePostBinding.etDescriptionUpdate.text.toString()
        val map = mutableMapOf<String,Any>()
        map["createdAt"] = newTime
        if(newDescription.isNotEmpty()) {
            map["description"] = newDescription
        }
        val changed = "Edited"
        map["postEdit"] = changed
        updateData(map)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(requestCode) {

            PICK_PHOTO_CODE -> {
                if(resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        launchImageCrop(it)
                    }
                }
                else  {
                    Toast.makeText(context,"Failed", Toast.LENGTH_SHORT).show()
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if(resultCode == Activity.RESULT_OK) {
                    result.uri?.let { uri ->
                        photoUri = uri
                        updatePostBinding.ivUploadUpdate.setImageURI(photoUri)
                        val postDao = PostDao()
                        flag = 1
                        GlobalScope.launch {
                            val post = postDao.getPostId(postId!!).await().toObject(Post::class.java)!!
                            val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(post!!.imageUrlPost)
                            fileRef.delete()
                        }
                    }
                }
                else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Toast.makeText(context,"Failed to Crop", Toast.LENGTH_SHORT).show()
                }
            }


        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1920,1600)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(requireActivity(),this)
    }

    private fun getImageUrl() {
        val photoUploadUri = photoUri as Uri
        val imageFileName = "images/${System.currentTimeMillis()}-photo.jpg"
        val photoRef = storageRef.child(imageFileName).putFile(photoUploadUri)
        photoRef.addOnSuccessListener {
            val image = storageRef.child(imageFileName).downloadUrl
            image.addOnSuccessListener {
                val imageUrl = it.toString()
                getNewData(imageUrl)
            }.addOnFailureListener {
                Toast.makeText(context,"Failed",Toast.LENGTH_SHORT).show()
                val fm:FragmentManager = requireActivity().supportFragmentManager
                fm.popBackStack()
            }
        }.addOnFailureListener {
            Toast.makeText(context,"Failed",Toast.LENGTH_SHORT).show()
            val fm:FragmentManager = requireActivity().supportFragmentManager
            fm.popBackStack()
        }
    }

    private fun getNewData(newUrl: String) {
        val newTime = System.currentTimeMillis()
        val newDescription = updatePostBinding.etDescriptionUpdate.text.toString()
        val map = mutableMapOf<String,Any>()
        map["createdAt"] = newTime
        if(newDescription.isNotEmpty()) {
            map["description"] = newDescription
        }
        map["imageUrlPost"] = newUrl
        val changed = "Edited"
        map["postEdit"] = changed
        updateData(map)
    }

    private fun updateData(newData: MutableMap<String, Any>) {
        GlobalScope.launch(Dispatchers.IO) {
            val db = FirebaseFirestore.getInstance()
            val postsCollections = db.collection("posts")
            postsCollections.document(postId!!).set(
                newData,
                SetOptions.merge()
            ).await()
        }
        updatePostBinding.proBarUpdate.visibility = View.GONE
        Toast.makeText(context,"Updated Successfully",Toast.LENGTH_SHORT).show()
        sendNotification()
        val fm:FragmentManager = requireActivity().supportFragmentManager
        val ft:FragmentTransaction = fm.beginTransaction()
        val feedFragment = FeedFragment()
        ft.replace(R.id.postFrame,feedFragment)
        fm.popBackStack()
        ft.commit()
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
                                            "${user.email} has edited a post"
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

