package com.rlua.man.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.ActivityVerifyBinding
import com.rlua.man.databinding.ItemPendingRequestBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class VerifyActivity : AppCompatActivity() {
    private lateinit var b: ActivityVerifyBinding
    private val handler = Handler(Looper.getMainLooper())
    private var verifyId = ""
    private var isNewDevice = false
    private var codeValue = ""
    private var polling = false

    private val pollRunnable = object : Runnable {
        override fun run() { if (!polling) return; poll(); handler.postDelayed(this, 3000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityVerifyBinding.inflate(layoutInflater)
        setContentView(b.root)
        verifyId = intent.getStringExtra("verifyId") ?: ""
        isNewDevice = intent.getBooleanExtra("is_new_device", false)
        if (verifyId.isEmpty()) { finish(); return }
        b.btnCopyCode.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("code", codeValue))
            Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show()
        }
        startPolling()
    }

    override fun onResume() { super.onResume(); startPolling() }
    override fun onPause() { super.onPause(); stopPolling() }
    private fun startPolling() { if (polling) return; polling = true; handler.post(pollRunnable) }
    private fun stopPolling() { polling = false; handler.removeCallbacks(pollRunnable) }

    private fun poll() {
        if (isNewDevice) {
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) { ApiClient.verifyComplete(verifyId, "") }
                    if (res.optBoolean("ok")) {
                        stopPolling()
                        SessionManager.save(this@VerifyActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                        startActivity(Intent(this@VerifyActivity, LobbyActivity::class.java)); finish(); return@launch
                    }
                    if (res.optString("error", "").contains("отклонён")) {
                        stopPolling()
                        b.verifySubtitle.text = "Вход отклонён"; b.progressVerify.visibility = View.GONE
                        handler.postDelayed({ finish() }, 3000)
                    }
                } catch (_: Exception) {}
            }
        } else {
            val token = SessionManager.token(this) ?: return
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) { ApiClient.verifyPending(token) }
                    if (!res.optBoolean("ok")) return@launch
                    val pending = res.optJSONArray("pending") ?: JSONArray()
                    if (pending.length() > 0) {
                        b.progressVerify.visibility = View.GONE
                        b.pendingList.visibility = View.VISIBLE
                        b.pendingList.layoutManager = LinearLayoutManager(this@VerifyActivity)
                        b.pendingList.adapter = PendingAdapter(pending, token)
                    } else {
                        b.verifySubtitle.text = "Ожидание нового входа..."
                        b.progressVerify.visibility = View.VISIBLE
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private inner class PendingAdapter(private val data: JSONArray, private val token: String) : RecyclerView.Adapter<PendingAdapter.VH>() {
        inner class VH(val b: ItemPendingRequestBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemPendingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun getItemCount() = data.length()
        override fun onBindViewHolder(holder: VH, pos: Int) {
            val item = data.getJSONObject(pos)
            val id = item.optString("id", "")
            val ip = item.optString("ip", "?")
            val device = item.optString("device", "Unknown device")
            holder.b.pendingDevice.text = device
            holder.b.pendingIp.text = "IP: $ip"
            holder.b.btnShowCode.setOnClickListener {
                lifecycleScope.launch {
                    val res = withContext(Dispatchers.IO) { ApiClient.verifyCode(token, id) }
                    if (res.optBoolean("ok")) { codeValue = res.optString("code", "------"); b.codeCard.visibility = View.VISIBLE; b.codeValue.text = codeValue }
                }
            }
            holder.b.btnConfirm.setOnClickListener {
                AlertDialog.Builder(this@VerifyActivity).setTitle("Подтвердить вход?").setMessage("IP: $ip\nУстройство: $device").setPositiveButton("Показать код") { _, _ ->
                    lifecycleScope.launch {
                        val res = withContext(Dispatchers.IO) { ApiClient.verifyCode(token, id) }
                        if (res.optBoolean("ok")) { codeValue = res.optString("code", "------"); b.codeCard.visibility = View.VISIBLE; b.codeValue.text = codeValue }
                    }
                }.setNegativeButton("Отмена", null).show()
            }
            holder.b.btnReject.setOnClickListener {
                AlertDialog.Builder(this@VerifyActivity).setTitle("Отклонить вход?").setMessage("Устройство будет заблокировано").setPositiveButton("Отклонить") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { ApiClient.verifyConfirm(token, id, "reject") }
                        Toast.makeText(this@VerifyActivity, "Вход отклонён, IP заблокирован", Toast.LENGTH_SHORT).show()
                        holder.itemView.visibility = View.GONE
                    }
                }.setNegativeButton("Отмена", null).show()
            }
        }
    }
}
