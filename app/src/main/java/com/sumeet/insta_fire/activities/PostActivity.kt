package com.sumeet.insta_fire.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.sumeet.insta_fire.R
import com.sumeet.insta_fire.databinding.ActivityPostBinding
import com.sumeet.insta_fire.main_page_fragments.MainFragment.Companion.SIGN_IN_METHOD_KEY
import com.sumeet.insta_fire.post_page_fragments.AddFriendFragment
import com.sumeet.insta_fire.post_page_fragments.FeedFragment
import com.sumeet.insta_fire.post_page_fragments.ProfileFragment
import com.sumeet.insta_fire.post_page_fragments.ProfilePostFragment

class PostActivity : AppCompatActivity() {
    lateinit var postBinding:ActivityPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postBinding = ActivityPostBinding.inflate(layoutInflater)
        val view = postBinding.root
        setContentView(view)

        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        val feedFragment = FeedFragment()
        fragmentTransaction.add(R.id.postFrame,feedFragment)
        fragmentTransaction.commit()


        postBinding.toolBar.setOnMenuItemClickListener { item ->

            when(item.itemId) {
                R.id.profile -> appNavigation("profile")
                R.id.profilePost -> appNavigation("profilePosts")
                R.id.addFriend -> appNavigation("addFriend")
                R.id.feed -> appNavigation("feed")
                R.id.logout -> logout()
            }

            return@setOnMenuItemClickListener true
        }
    }

    fun appNavigation(navigation:String) {
        val fm:FragmentManager = supportFragmentManager
        val ft:FragmentTransaction = fm.beginTransaction()

        if(navigation == "profile") {
            val profileFragment = ProfileFragment()
            ft.replace(R.id.postFrame,profileFragment)
            ft.addToBackStack(null)
            ft.commit()

        }
        else if(navigation == "profilePosts") {
            val profilePostsFragment = ProfilePostFragment()
            ft.replace(R.id.postFrame,profilePostsFragment)
            ft.addToBackStack(null)
            ft.commit()

        }
        else if(navigation == "addFriend") {
            val addFriendFragment = AddFriendFragment()
            ft.replace(R.id.postFrame,addFriendFragment)
            ft.addToBackStack(null)
            ft.commit()
        }
        else if(navigation == "feed") {
            val feedFragment = FeedFragment()
            ft.replace(R.id.postFrame,feedFragment)
            ft.addToBackStack(null)
            ft.commit()
        }

    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this@PostActivity,MainActivity::class.java)
        startActivity(intent)
        finish()
    }


}