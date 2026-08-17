package com.rlua.man.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.FragmentAdminBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminFragment : Fragment() {
    private var _b: FragmentAdminBinding? = null
    private val b get() = _b!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { _b = FragmentAdminBinding.inflate(inflater, container, false); return b.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = SessionManager.token(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val notifs = withContext(Dispatchers.IO) { ApiClient.adminNotifications(token) }
                val arr = notifs.optJSONArray("notifications")
                if (arr != null && arr.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until arr.length()) { val n = arr.getJSONObject(i); sb.appendLine("[${n.optString("time")}] ${n.optString("type")}: ${n.optString("message", n.optString("details", ""))}") }
                    b.adminNotifications.text = sb.toString(); b.adminCard.visibility = View.VISIBLE
                }
            } catch (_: Exception) {}
        }
        lifecycleScope.launch {
            try {
                val users = withContext(Dispatchers.IO) { ApiClient.adminUsers(token) }
                val arr = users.optJSONArray("users")
                if (arr != null && arr.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until arr.length()) { val u = arr.getJSONObject(i); sb.appendLine("${u.optString("username")} (${u.optString("role", "user")})") }
                    b.adminUsers.text = sb.toString(); b.adminUsersCard.visibility = View.VISIBLE
                }
            } catch (_: Exception) {}
        }
        lifecycleScope.launch {
            try {
                val logs = withContext(Dispatchers.IO) { ApiClient.adminLogs(token) }
                val arr = logs.optJSONArray("logs")
                if (arr != null && arr.length() > 0) {
                    val sb = StringBuilder()
                    val start = maxOf(0, arr.length() - 20)
                    for (i in start until arr.length()) { val l = arr.getJSONObject(i); sb.appendLine("[${l.optString("time")}] ${l.optString("type")}: ${l.optString("details", "")}") }
                    b.adminLogs.text = sb.toString(); b.adminLogsCard.visibility = View.VISIBLE
                }
            } catch (_: Exception) {}
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
