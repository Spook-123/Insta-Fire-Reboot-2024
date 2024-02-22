package com.sumeet.insta_fire.push_notification

import com.google.gson.annotations.SerializedName

data class NotificationData(
    @SerializedName("to") val to: String,
    @SerializedName("notification") val notification: NotificationContent,
)

data class NotificationContent(
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String
)


