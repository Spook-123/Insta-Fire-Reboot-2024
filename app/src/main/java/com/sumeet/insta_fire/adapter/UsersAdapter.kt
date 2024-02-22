package com.sumeet.insta_fire.adapter

import android.R
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.sumeet.insta_fire.daos.UserDao
import com.sumeet.insta_fire.databinding.ItemProfileFriendBinding
import com.sumeet.insta_fire.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UsersAdapter (val context: Context,val userList:ArrayList<User>):RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    inner class UsersViewHolder(val adapterBinding:ItemProfileFriendBinding):RecyclerView.ViewHolder(adapterBinding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val binding = ItemProfileFriendBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return UsersViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        holder.adapterBinding.tvEmail.text = userList[position].email
        Glide.with(holder.adapterBinding.ivProfileImage.context).load(userList[position].imageUrl)
            .apply(
                RequestOptions().transform(
                    CenterCrop(), RoundedCorners(100)

                )
            ).into(holder.adapterBinding.ivProfileImage)

        holder.adapterBinding.ivProfileImage.setOnClickListener {
            openFullScreenImage(userList[position].imageUrl)
        }

        holder.adapterBinding.ivDelete.setOnClickListener {
            val currentUserUid = Firebase.auth.currentUser?.uid
            GlobalScope.launch {
                val userDao = UserDao()
                val user = userDao.getUserId(currentUserUid!!).await().toObject(User::class.java)
                val email = userList[position].email
                val db = FirebaseFirestore.getInstance()
                val usersCollection = db.collection("users")
                // Remove the email from the local userList
                userList.removeAt(position)

                // Switch to the main thread before notifying the adapter
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }

                // Remove the email from the Firestore user's friend list
                user?.friendList?.remove(email)
                usersCollection.document(currentUserUid).set(user!!)
                //userDao.updateUser(user!!)
                //user!!.friendList.remove(email)
            }
        }
    }

    private fun openFullScreenImage(imageUrl:String) {
        val dialog = Dialog(context, R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(com.sumeet.insta_fire.R.layout.dialog_full_screen_image)

        val photoView = dialog.findViewById<PhotoView>(com.sumeet.insta_fire.R.id.photoView)
        //photoView.setImageDrawable(drawable)
        // Load the full-size image into PhotoView without any transformations
        Glide.with(context)
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



}