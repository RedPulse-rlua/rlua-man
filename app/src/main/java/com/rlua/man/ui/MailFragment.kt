package com.rlua.man.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.R
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MailFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_mail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        running = true
        startPoll(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun startPoll(v: View) {
        if (!running) return
        val ctx = requireContext()
        val token = SessionManager.token(ctx) ?: return
        val list = v.findViewById<LinearLayout>(R.id.mailList)
        val emptyText = v.findViewById<TextView>(R.id.mailEmpty)
        poll(v, token, list, emptyText)
    }

    private fun poll(v: View, token: String, list: LinearLayout, emptyText: TextView) {
        if (!running) return
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.verifyPending(token) }
                if (isAdded && res.optBoolean("ok")) {
                    val pending = res.optJSONArray("pending")
                    list.removeAllViews()
                    if (pending == null || pending.length() == 0) {
                        emptyText.visibility = View.VISIBLE
                    } else {
                        emptyText.visibility = View.GONE
                        for (i in 0 until pending.length()) {
                            val item = pending.getJSONObject(i)
                            list.addView(createMailCard(item, token, list))
                        }
                    }
                }
            } catch (_: Exception) {}
            if (running && isAdded) handler.postDelayed({ poll(v, token, list, emptyText) }, 5000)
        }
    }

    private fun createMailCard(item: org.json.JSONObject, token: String, list: LinearLayout): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(Color.parseColor("#1A0B0B"))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 16
            layoutParams = lp
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val id = item.optString("id", "")
        val username = item.optString("username", "?")
        val ip = item.optString("ip", "?")
        val device = item.optString("device", "Устройство")
        val status = item.optString("status", "pending")
        val code = item.optString("code", "")
        val attempts = item.optInt("attempts", 0)
        val createdAt = item.optLong("createdAt", 0)
        val ago = ((System.currentTimeMillis() - createdAt) / 1000).toInt()
        val agoStr = if (ago < 60) "${ago}с" else "${ago / 60}м"
        val isConfirmed = status == "confirmed"
        val isRejected = status == "rejected"
        val statusText = when {
            isConfirmed && attempts > 0 -> "Код введён неверно, осталось ${3 - attempts} попытки"
            isConfirmed -> "Код действует 5 минут"
            isRejected && attempts >= 3 -> "Код введён неверно 3 раза — вход заблокирован"
            isRejected -> "Запрос отклонён"
            else -> "Ждёт подтверждения"
        }

        val avatar = TextView(ctx).apply {
            text = username.take(2).uppercase()
            setTextColor(Color.parseColor("#F4E8E8"))
            setBackgroundColor(Color.parseColor("#FF2D2D"))
            setPadding(16, 16, 16, 16)
            textSize = 14f
            val lp = LinearLayout.LayoutParams(96, 96)
            lp.marginEnd = 24
            layoutParams = lp
            gravity = android.view.Gravity.CENTER
        }

        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        val nameView = TextView(ctx).apply {
            text = username
            setTextColor(Color.parseColor("#F4E8E8"))
            textSize = 16f
        }

        val metaView = TextView(ctx).apply {
            text = "$ip · $device · $agoStr"
            setTextColor(Color.parseColor("#A37C7C"))
            textSize = 12f
        }

        info.addView(nameView)
        info.addView(metaView)
        val statusView = TextView(ctx).apply {
            text = statusText
            setTextColor(if (isConfirmed) Color.parseColor("#FF2D2D") else if (isRejected) Color.parseColor("#FF4B4B") else Color.parseColor("#A37C7C"))
            textSize = 11f
        }
        info.addView(statusView)

        val codeView = TextView(ctx).apply {
            text = if (isConfirmed) code else "••••••"
            setTextColor(Color.parseColor("#FF2D2D"))
            textSize = if (isConfirmed) 22f else 18f
            typeface = android.graphics.Typeface.MONOSPACE
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = 16
            lp.marginEnd = 16
            layoutParams = lp
        }

        val confirmBtn = TextView(ctx).apply {
            text = "OK"
            setTextColor(Color.parseColor("#F4E8E8"))
            setBackgroundColor(Color.parseColor("#FF2D2D"))
            setPadding(24, 12, 24, 12)
            textSize = 14f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = 8
            layoutParams = lp
        }

        val rejectBtn = TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.parseColor("#A37C7C"))
            setBackgroundColor(Color.parseColor("#1A0B0B"))
            setPadding(24, 12, 24, 12)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        if (isConfirmed || isRejected) {
            confirmBtn.visibility = View.GONE
        }
        if (isRejected) {
            rejectBtn.visibility = View.GONE
            card.alpha = 0.6f
        }

        confirmBtn.setOnClickListener {
            confirmBtn.isEnabled = false
            rejectBtn.isEnabled = false
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) { ApiClient.verifyConfirm(token, id, "confirm") }
                    if (res.optBoolean("ok")) {
                        codeView.text = res.optString("code", code).ifEmpty { code }
                        codeView.textSize = 22f
                        statusView.text = "Код действует 5 минут"
                        statusView.setTextColor(Color.parseColor("#FF2D2D"))
                        confirmBtn.visibility = View.GONE
                    }
                } catch (_: Exception) {}
            }
        }

        rejectBtn.setOnClickListener {
            confirmBtn.isEnabled = false
            rejectBtn.isEnabled = false
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { ApiClient.verifyConfirm(token, id, "reject") }
                    list.removeView(card)
                } catch (_: Exception) {}
            }
        }

        card.addView(avatar)
        card.addView(info)
        card.addView(codeView)
        card.addView(confirmBtn)
        card.addView(rejectBtn)

        return card
    }
}
