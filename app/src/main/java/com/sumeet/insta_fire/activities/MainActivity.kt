package com.sumeet.insta_fire.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.databinding.ActivityMainBinding
import com.sumeet.insta_fire.main_page_fragments.MainFragment

class MainActivity : AppCompatActivity() {
    lateinit var mainBinding: ActivityMainBinding

    // Firebase
    val auth: FirebaseAuth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = mainBinding.root
        setContentView(view)
        val fragmentManager:FragmentManager = supportFragmentManager
        val fragmentTransaction:FragmentTransaction = fragmentManager.beginTransaction()
        val mainFragment = MainFragment()
        fragmentTransaction.add(R.id.frame,mainFragment)
        fragmentTransaction.commit()

    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if(user != null ){
            Toast.makeText(applicationContext,"Successfully Login", Toast.LENGTH_SHORT).show()
            val intent = Intent(applicationContext, PostActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

}
