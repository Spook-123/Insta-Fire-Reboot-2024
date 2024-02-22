package com.sumeet.insta_fire.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.daos.PostDao
import com.sumeet.insta_fire.databinding.ItemPostBinding
import com.sumeet.insta_fire.databinding.ItemProfileFriendBinding
import com.sumeet.insta_fire.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FeedAdapter(val context: Context, val feedList: ArrayList<Post>,val listener:onItemClick) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {


    inner class FeedViewHolder(val adapterBinding: ItemPostBinding) :
        RecyclerView.ViewHolder(adapterBinding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return feedList.size
    }


    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.adapterBinding.tvUserName.text = feedList[position].createdBy.email
        holder.adapterBinding.tvDescription.text = feedList[position].description
        holder.adapterBinding.tvCreatedAt.text =
            DateUtils.getRelativeTimeSpanString(feedList[position].createdAt)
        holder.adapterBinding.tvEdited.text = feedList[position].postEdit
        holder.adapterBinding.tvLikeCount.text = feedList[position].likedBy.size.toString()
        Glide.with(holder.adapterBinding.ivProfileImage.context)
            .load(feedList[position].createdBy.imageUrl)
            .apply(
                RequestOptions().transform(
                    CenterCrop(), RoundedCorners(100)

                )
            ).into(holder.adapterBinding.ivProfileImage)

        Glide.with(holder.adapterBinding.ivImageUploaded.context)
            .load(feedList[position].imageUrlPost).apply(
                RequestOptions().transform(
                    CenterCrop(), RoundedCorners(20)

                )
            ).into(holder.adapterBinding.ivImageUploaded)

        holder.adapterBinding.ivProfileImage.setOnClickListener {

            //openFullScreenImage(holder.adapterBinding.ivProfileImage.drawable)
            openFullScreenImage(feedList[position].createdBy.imageUrl)
        }

        holder.adapterBinding.ivImageUploaded.setOnClickListener {

            //openFullScreenImage(holder.adapterBinding.ivImageUploaded.drawable)
            openFullScreenImage(feedList[position].imageUrlPost)

        }
        val auth = Firebase.auth
        val currentUserId = auth.currentUser!!.uid
        val isLiked = feedList[position].likedBy.contains(currentUserId)
        Log.e("Feed Adapter","View Refresh-16")
        if (isLiked) {
            holder.adapterBinding.ivLikeButton.setImageDrawable(
                ContextCompat.getDrawable(
                    holder.adapterBinding.ivLikeButton.context,
                    R.drawable.ic_liked
                )
            )
        } else {
            holder.adapterBinding.ivLikeButton.setImageDrawable(
                ContextCompat.getDrawable(
                    holder.adapterBinding.ivLikeButton.context,
                    R.drawable.ic_unliked
                )
            )
        }

        holder.adapterBinding.ivDelete.setOnClickListener {
            val auth = Firebase.auth
            GlobalScope.launch {
                val postDao = PostDao()
                val post =
                    postDao.getPostId(feedList[position].postId).await().toObject(Post::class.java)

                if(auth.currentUser!!.uid == post!!.createdBy.uid) {
                    val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(post!!.imageUrlPost)
                    fileRef.delete()

                    // Delete the post from Firestore
                    val db = FirebaseFirestore.getInstance()
                    val postsCollection = db.collection("posts")
                    postsCollection.document(feedList[position].postId).delete().await()

                    feedList.removeAt(position)

                    // Notify the adapter that the data set has changed
                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                        Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                        Toast.makeText(context, "Access Denied", Toast.LENGTH_SHORT).show()
                    }

                }

            }
        }



        holder.adapterBinding.ivLikeButton.setOnClickListener {
            Log.e("FeedFragment","View Refresh-17")
            GlobalScope.launch(Dispatchers.IO) {
                val postDao = PostDao()
                val auth = Firebase.auth
                val currentId = auth.currentUser!!.uid
                val post =
                    postDao.getPostId(feedList[position].postId).await().toObject(Post::class.java)
                val isLikedNew = post!!.likedBy.contains(currentId)
                Log.e("FeedFragment","View Refresh-18")
                if (isLikedNew) {
                    post.likedBy.remove(currentId)
                } else {
                    post.likedBy.add(currentId)
                }
                val db = FirebaseFirestore.getInstance()
                val postsCollection = db.collection("posts")
                Log.e("FeedFragment","View Refresh-19")
                postsCollection.document(feedList[position].postId).set(post)
                //postsCollection.document(postId).set(post)
                Log.e("FeedFragment","View Refresh-20")
                feedList[position] = post
                Log.e("FeedFragment","View Refresh-21")
                withContext(Dispatchers.Main) {
                    Log.e("FeedFragment","View Refresh-22")
                    notifyDataSetChanged()
                    Log.e("FeedFragment","View Refresh-23")
                }
                Log.e("FeedFragment","View Refresh-24")
            }
            Log.e("FeedFragment","View Refresh-25")

        }

        holder.adapterBinding.ivEdit.setOnClickListener {
            listener.onUpdateClicked(feedList[position].postId)
        }

    }

    private suspend fun updatePostInFirestore(post: Post) {
        withContext(Dispatchers.IO) {
            val db = FirebaseFirestore.getInstance()
            val postsCollection = db.collection("posts")
            postsCollection.document(post.postId).set(post).await()
        }
    }


    private fun openFullScreenImage(imageUrl:String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_screen_image)

        val photoView = dialog.findViewById<PhotoView>(R.id.photoView)
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



interface onItemClick {

    fun onUpdateClicked(postId:String)
}




