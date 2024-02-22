package com.sumeet.insta_fire.models

import com.google.firebase.firestore.PropertyName

data class User(
    @PropertyName("fcmToken") val fcmToken:String = "",
    @PropertyName("uid") val uid:String = "",
    @PropertyName("email") val email:String = "",
    @PropertyName("imageUrl") var imageUrl:String = "",
    @PropertyName("uniqueId") val uniqueId: String = "",
    @PropertyName("friendList") val friendList:ArrayList<String> = ArrayList(),
)
