package com.sumeet.insta_fire.daos

import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.sumeet.insta_fire.models.Post
import com.sumeet.insta_fire.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostDao {
    val db = FirebaseFirestore.getInstance()
    val postsCollection = db.collection("posts")
    val auth = Firebase.auth

    fun addPost(description:String,imageUrlUpload:String) {
        val currentUser = auth.currentUser
        // Change
        GlobalScope.launch(Dispatchers.IO) {
            val userDao = UserDao()
            val user = userDao.getUserId(currentUser!!.uid).await().toObject(User::class.java)!!
            val currentTime = System.currentTimeMillis()
            val postId = UUID.randomUUID()
            val post = Post(postId.toString(),"",description,user,currentTime,imageUrlUpload)
            postsCollection.document(postId.toString()).set(post)

        }
    }

    fun getPostId(postId: String): Task<DocumentSnapshot> {
        return postsCollection.document(postId).get()
    }





}