package com.sumeet.insta_fire.models

import com.google.firebase.firestore.PropertyName

data class Post(
    @PropertyName("postId") val postId:String = "",
    @PropertyName("postEdit") val postEdit:String = "",
    @PropertyName("description") val description:String = "",
    @PropertyName("createdBy") val createdBy:User = User(),
    @PropertyName("createdAt") val createdAt:Long = 0L,
    @PropertyName("imageUrlPost") val imageUrlPost:String = "",
    @PropertyName("likedBy") val likedBy:ArrayList<String> = ArrayList()
)
