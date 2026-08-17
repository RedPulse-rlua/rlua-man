package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.rlua.man.R
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.ActivityLobbyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LobbyActivity : AppCompatActivity() {
    private lateinit var b: ActivityLobbyBinding
    private var userRole = "user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.headerUsername.text = SessionManager.username(this)
        userRole = SessionManager.role(this) ?: "user"
        b.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { SessionManager.token(this@LobbyActivity)?.let { ApiClient.logout(it) } } } catch (_: Exception) {}
                SessionManager.clear(this@LobbyActivity)
                startActivity(Intent(this@LobbyActivity, LoginActivity::class.java)); finish()
            }
        }
        b.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_lobby -> { loadFragment(LobbyFragment()); true }
                R.id.nav_files -> { loadFragment(FilesFragment()); true }
                R.id.nav_vizor -> { loadFragment(VizorFragment()); true }
                R.id.nav_profile -> { loadFragment(ProfileFragment()); true }
                R.id.nav_admin -> { if (userRole == "admin") { loadFragment(AdminFragment()); true } else { Toast.makeText(this, "Нет доступа", Toast.LENGTH_SHORT).show(); false } }
                else -> false
            }
        }
        if (savedInstanceState == null) b.bottomNav.selectedItemId = R.id.nav_lobby
    }

    private fun loadFragment(f: Fragment) { supportFragmentManager.beginTransaction().replace(R.id.contentFrame, f).commit() }
}
