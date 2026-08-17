package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rlua.man.R
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LobbyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        val headerUsername = findViewById<TextView>(R.id.headerUsername)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val lobbyName = findViewById<TextView>(R.id.lobbyName)
        val lobbyRole = findViewById<TextView>(R.id.lobbyRole)
        val lobbyId = findViewById<TextView>(R.id.lobbyId)
        val lobbyScripts = findViewById<TextView>(R.id.lobbyScripts)
        val lobbyDays = findViewById<TextView>(R.id.lobbyDays)
        val lobbyAvatar = findViewById<TextView>(R.id.lobbyAvatar)
        val lobbyDate = findViewById<TextView>(R.id.lobbyDate)
        val adminBadge = findViewById<TextView>(R.id.adminBadge)

        val username = SessionManager.username(this) ?: "?"
        headerUsername.text = username
        lobbyName.text = username
        lobbyAvatar.text = username.take(2).uppercase()

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { SessionManager.token(this@LobbyActivity)?.let { ApiClient.logout(it) } } } catch (_: Exception) {}
                SessionManager.clear(this@LobbyActivity)
                startActivity(Intent(this@LobbyActivity, LoginActivity::class.java)); finish()
            }
        }

        loadProfile(lobbyName, lobbyRole, lobbyId, lobbyScripts, lobbyDays, lobbyDate, adminBadge)
    }

    private fun loadProfile(name: TextView, role: TextView, id: TextView, scripts: TextView, days: TextView, date: TextView, badge: TextView) {
        val token = SessionManager.token(this) ?: return
        role.text = "..."
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.me(token) }
                if (!res.optBoolean("ok") || res.isNull("user")) {
                    SessionManager.clear(this@LobbyActivity)
                    startActivity(Intent(this@LobbyActivity, LoginActivity::class.java)); finish(); return@launch
                }
                val user = res.getJSONObject("user")
                name.text = user.optString("username", "?")
                val roleStr = if (user.optString("role") == "admin") "Админ" else "User"
                role.text = roleStr
                id.text = "#${user.optInt("id", 0)}"
                scripts.text = "${user.optInt("scripts", 0)}"
                badge.visibility = if (user.optString("role") == "admin") android.view.View.VISIBLE else android.view.View.GONE
                if (user.has("createdAt")) {
                    val d = java.util.Date(user.optLong("createdAt", 0))
                    val now = System.currentTimeMillis()
                    val dayCount = ((now - user.optLong("createdAt", 0)) / 86400000).toInt()
                    days.text = when {
                        dayCount == 0 -> "Сегодня"
                        dayCount == 1 -> "1 день"
                        dayCount in 2..4 -> "$dayCount дня"
                        else -> "$dayCount дней"
                    }
                    date.text = "на rlua с " + java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru")).format(d)
                }
            } catch (_: Exception) {}
        }
    }
}
