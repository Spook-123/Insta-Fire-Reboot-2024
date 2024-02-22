package com.sumeet.insta_fire.push_notification

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface FcmApi {

    @Headers("Content-Type: application/json", "Authorization: Bearer")
    @POST("fcm/send")
    suspend fun sendNotification(@Body notification: NotificationData): Response<Void>
}
