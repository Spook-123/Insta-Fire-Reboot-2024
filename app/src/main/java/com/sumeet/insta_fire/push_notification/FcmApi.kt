package com.sumeet.insta_fire.push_notification

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface FcmApi {

    @Headers("Content-Type: application/json", "Authorization: Bearer AAAAtZkXM3I:APA91bHNU2AtZ8q8-uyluydjHuQixpC4jSLqyGbWxXoEC_WprWJx6T3iTRywz9n-3WHieuqFLd3NFx9AivnhGMRTjlBFBaC6nsr4zwkhCJKGHpLmHimTiGmhWavh7erGjeG-PDLRj_zR")
    @POST("fcm/send")
    suspend fun sendNotification(@Body notification: NotificationData): Response<Void>
}