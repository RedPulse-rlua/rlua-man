package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.R
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LobbyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_lobby_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        val username = SessionManager.username(ctx) ?: return
        view.findViewById<TextView>(R.id.lobbyName).text = username
        view.findViewById<TextView>(R.id.lobbyAvatar).text = username.take(2).uppercase()

        view.findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { SessionManager.token(ctx)?.let { ApiClient.logout(it) } } } catch (_: Exception) {}
                SessionManager.clear(ctx)
                startActivity(Intent(ctx, LoginActivity::class.java))
                requireActivity().finish()
            }
        }

        val token = SessionManager.token(ctx) ?: return
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.me(token) }
                if (!res.optBoolean("ok") || res.isNull("user")) {
                    SessionManager.clear(ctx)
                    startActivity(Intent(ctx, LoginActivity::class.java))
                    requireActivity().finish(); return@launch
                }
                val user = res.getJSONObject("user")
                view.findViewById<TextView>(R.id.lobbyName).text = user.optString("username", "?")
                val role = if (user.optString("role") == "admin") "Админ" else "User"
                view.findViewById<TextView>(R.id.lobbyRole).text = role
                view.findViewById<TextView>(R.id.lobbyId).text = "#${user.optInt("id", 0)}"
                view.findViewById<TextView>(R.id.lobbyScripts).text = "${user.optInt("scripts", 0)}"
                if (user.optString("role") == "admin") {
                    view.findViewById<TextView>(R.id.adminBadge).visibility = View.VISIBLE
                }
                if (user.has("createdAt")) {
                    val d = java.util.Date(user.optLong("createdAt", 0))
                    val days = ((System.currentTimeMillis() - user.optLong("createdAt", 0)) / 86400000).toInt()
                    val daysStr = when {
                        days == 0 -> "Сегодня"
                        days == 1 -> "1 день"
                        days in 2..4 -> "$days дня"
                        else -> "$days дней"
                    }
                    view.findViewById<TextView>(R.id.lobbyDays).text = daysStr
                    view.findViewById<TextView>(R.id.lobbyDate).text =
                        "на rlua с " + java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru")).format(d)
                }
            } catch (_: Exception) {}
        }
    }
}
