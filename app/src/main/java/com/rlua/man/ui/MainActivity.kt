package com.rlua.man.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.rlua.man.R
import com.rlua.man.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (savedInstanceState == null) loadFragment(MailFragment())

        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_mail -> { loadFragment(MailFragment()); true }
                R.id.nav_devices -> { loadFragment(DevicesFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, f).commit()
    }
}