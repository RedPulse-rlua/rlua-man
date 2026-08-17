package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.ActivityLobbyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LobbyActivity : AppCompatActivity() {
    private lateinit var b: ActivityLobbyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(b.root)

        loadProfile()

        b.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { SessionManager.token(this@LobbyActivity)?.let { ApiClient.logout(it) } } } catch (_: Exception) {}
                SessionManager.clear(this@LobbyActivity)
                startActivity(Intent(this@LobbyActivity, LoginActivity::class.java)); finish()
            }
        }
    }

    private fun loadProfile() {
        val token = SessionManager.token(this) ?: return
        b.headerUsername.text = SessionManager.username(this) ?: "?"
        b.lobbyName.text = SessionManager.username(this) ?: "?"
        b.lobbyRole.text = "Загрузка..."
        b.lobbyId.text = ""

        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.me(token) }
                if (!res.optBoolean("ok") || res.isNull("user")) {
                    SessionManager.clear(this@LobbyActivity)
                    startActivity(Intent(this@LobbyActivity, LoginActivity::class.java)); finish(); return@launch
                }
                val user = res.getJSONObject("user")
                b.lobbyName.text = user.optString("username", "?")
                b.lobbyRole.text = if (user.optString("role") == "admin") "Администратор" else "Пользователь"
                b.lobbyId.text = "ID: ${user.optInt("id", 0)}"
                if (user.has("createdAt")) {
                    val days = ((System.currentTimeMillis() - user.optLong("createdAt", 0)) / 86400000).toInt()
                    b.lobbyDays.text = if (days == 0) "Сегодня" else "$days дн."
                }
                b.lobbyScripts.text = "${user.optInt("scripts", 0)}"
            } catch (_: Exception) {}
        }
    }
}
