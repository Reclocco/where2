package com.example.where2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.where2.databinding.ActivityWelcomeBinding
import com.google.android.gms.tasks.Task

import androidx.annotation.NonNull

import com.google.android.gms.tasks.OnCompleteListener

import com.firebase.ui.auth.AuthUI

import android.R




class WelcomeActivity : AppCompatActivity()  {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.pickFromMap.setOnClickListener{
            openMaps()
        }

        binding.signOut.setOnClickListener{
            logout()
        }
    }

    private fun openMaps() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener { // user is now signed out
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

}