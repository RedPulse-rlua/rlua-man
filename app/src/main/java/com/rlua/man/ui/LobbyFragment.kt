package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.FragmentLobbyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LobbyFragment : Fragment() {
    private var _b: FragmentLobbyBinding? = null
    private val b get() = _b!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { _b = FragmentLobbyBinding.inflate(inflater, container, false); return b.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = SessionManager.token(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.me(token) }
                if (!res.optBoolean("ok") || res.isNull("user")) { SessionManager.clear(requireContext()); startActivity(Intent(requireContext(), LoginActivity::class.java)); activity?.finish(); return@launch }
                val user = res.getJSONObject("user")
                b.lobbyName.text = user.optString("username", "?")
                b.lobbyRole.text = "Роль: ${user.optString("role", "user")}"
                b.lobbyId.text = "ID: ${user.optInt("id", 0)}"
            } catch (_: Exception) {}
        }
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.adminNotifications(token) }
                if (res.optBoolean("ok")) {
                    val notifs = res.optJSONArray("notifications")
                    if (notifs != null && notifs.length() > 0) {
                        val sb = StringBuilder()
                        for (i in 0 until notifs.length()) { val n = notifs.getJSONObject(i); sb.appendLine("${n.optString("type")}: ${n.optString("message", n.optString("details", ""))}") }
                        b.lobbyNotifications.text = sb.toString(); b.lobbyNotifications.visibility = View.VISIBLE
                    }
                }
            } catch (_: Exception) {}
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
