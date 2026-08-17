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
    private var pollRunnable: Runnable? = null

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
        pollRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startPoll(v: View) {
        if (!running) return
        val ctx = requireContext()
        val token = SessionManager.token(ctx) ?: return
        val list = v.findViewById<LinearLayout>(R.id.mailList)
        val emptyText = v.findViewById<TextView>(R.id.mailEmpty)

        pollRunnable = Runnable {
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) { ApiClient.verifyPending(token) }
                    if (res.optBoolean("ok")) {
                        val pending = res.optJSONArray("pending")
                        list.removeAllViews()
                        if (pending == null || pending.length() == 0) {
                            emptyText.visibility = View.VISIBLE
                        } else {
                            emptyText.visibility = View.GONE
                            for (i in 0 until pending.length()) {
                                val item = pending.getJSONObject(i)
                                val card = createMailCard(item, token, list)
                                list.addView(card)
                            }
                        }
                    }
                } catch (_: Exception) {}
                if (running) handler.postDelayed(this@Runnable, 5000)
            }
        }
        handler.post(pollRunnable!!)
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
        val createdAt = item.optLong("createdAt", 0)
        val ago = ((System.currentTimeMillis() - createdAt) / 1000).toInt()
        val agoStr = if (ago < 60) "${ago}с" else "${ago / 60}м"

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

        val codeView = TextView(ctx).apply {
            text = "••••••"
            setTextColor(Color.parseColor("#FF2D2D"))
            textSize = 18f
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

        confirmBtn.setOnClickListener {
            confirmBtn.isEnabled = false
            rejectBtn.isEnabled = false
            lifecycleScope.launch {
                try {
                    val codeRes = withContext(Dispatchers.IO) { ApiClient.verifyCode(token, id) }
                    if (codeRes.optBoolean("ok")) {
                        codeView.text = codeRes.optString("code", "???")
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(1500)
                try {
                    val code = codeView.text.toString()
                    withContext(Dispatchers.IO) { ApiClient.verifyConfirm(token, id, "confirm", code) }
                    list.removeView(card)
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
