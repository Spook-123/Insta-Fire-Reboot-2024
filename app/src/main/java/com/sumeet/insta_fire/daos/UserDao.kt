package com.sumeet.insta_fire.daos

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sumeet.insta_fire.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserDao {

    val db = FirebaseFirestore.getInstance()
    val usersCollection = db.collection("users")

    fun addUser(user: User) {
        user.let { 
            GlobalScope.launch(Dispatchers.IO) {
                usersCollection.document(user.uid).set(it)
            }
        }

    }

    fun getUserId(uId:String): Task<DocumentSnapshot> {
        return usersCollection.document(uId).get()
    }

//    fun deleteUser(userId: String) {
//        GlobalScope.launch {
//            val friend = getUserId(userId).await().toObject(User::class.java)
//            val email = friend?.email
//            val currentUser = FirebaseAuth.getInstance().currentUser?.uid
//            val user = getUserId(currentUser!!).await().toObject(User::class.java)
//
//            // Remove the email from the friendList
//            user?.friendList?.remove(email)
//
//            // Update the user document in Firestore
//            // Update the user document in Firestore
//            usersCollection.document(currentUser).set(user!!)
////            // Notify the adapter that the data set has changed
////            withContext(Dispatchers.Main) {
////                adapter.notifyDataSetChanged()
////            }
//
//        }
//    }
}