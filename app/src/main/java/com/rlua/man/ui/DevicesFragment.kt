package com.rlua.man.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.R
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DevicesFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_devices, container, false)
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
        val list = v.findViewById<LinearLayout>(R.id.deviceList)
        val emptyText = v.findViewById<TextView>(R.id.deviceEmpty)
        poll(v, token, list, emptyText)
    }

    private fun poll(v: View, token: String, list: LinearLayout, emptyText: TextView) {
        if (!running) return
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.devices(token) }
                if (isAdded && res.optBoolean("ok")) {
                    val devices = res.optJSONArray("devices")
                    list.removeAllViews()
                    if (devices == null || devices.length() == 0) {
                        emptyText.visibility = View.VISIBLE
                    } else {
                        emptyText.visibility = View.GONE
                        var first = true
                        for (i in 0 until devices.length()) {
                            val item = devices.getJSONObject(i)
                            list.addView(createDeviceCard(item, token, list, first))
                            first = false
                        }
                    }
                }
            } catch (_: Exception) {}
            if (running && isAdded) handler.postDelayed({ poll(v, token, list, emptyText) }, 5000)
        }
    }

    private fun createDeviceCard(item: JSONObject, token: String, list: LinearLayout, isFirst: Boolean): View {
        val ctx = requireContext()
        val id = item.optString("id", "")
        val device = item.optString("device", "Неизвестно")
        val ip = item.optString("ip", "")
        val isCurrent = item.optBoolean("current")
        val isMaster = item.optBoolean("master")
        val lat = if (item.has("lat") && !item.isNull("lat")) item.optDouble("lat", Double.NaN) else Double.NaN
        val lon = if (item.has("lon") && !item.isNull("lon")) item.optDouble("lon", Double.NaN) else Double.NaN
        val ipLat = if (item.has("ipLat") && !item.isNull("ipLat")) item.optDouble("ipLat", Double.NaN) else Double.NaN
        val ipLon = if (item.has("ipLon") && !item.isNull("ipLon")) item.optDouble("ipLon", Double.NaN) else Double.NaN
        val city = item.optString("city", "")
        val region = item.optString("region", "")
        val country = item.optString("country", "")
        val createdAt = item.optLong("createdAt", 0)
        val ago = ((System.currentTimeMillis() - createdAt) / 1000).toInt()
        val agoStr = if (ago < 60) "${ago}с назад" else if (ago < 3600) "${ago / 60}м назад" else "${ago / 3600}ч назад"

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(Color.parseColor("#1A0B0B"))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 16
            layoutParams = lp
        }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val icon = TextView(ctx).apply {
            text = device.take(2).uppercase()
            setTextColor(Color.parseColor("#F4E8E8"))
            setBackgroundColor(Color.parseColor("#FF2D2D"))
            setPadding(14, 14, 14, 14)
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(88, 88)
            lp.marginEnd = 20
            layoutParams = lp
        }

        val headInfo = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        val title = TextView(ctx).apply {
            text = device
            setTextColor(Color.parseColor("#F4E8E8"))
            textSize = 15f
        }

        val badges = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        if (isCurrent) badges.addView(badge(ctx, "ЭТО УСТРОЙСТВО", "#FF2D2D"))
        if (isMaster) badges.addView(badge(ctx, "ГЛАВНОЕ", "#7A2D2D"))
        badges.addView(badge(ctx, agoStr, "#3A2222"))

        headInfo.addView(title)
        headInfo.addView(badges)
        header.addView(icon)
        header.addView(headInfo)
        card.addView(header)

        val ipRow = TextView(ctx).apply {
            text = "IP: $ip"
            setTextColor(Color.parseColor("#A37C7C"))
            textSize = 12f
            setPadding(0, 14, 0, 2)
            setOnClickListener {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("ip", ip))
                Toast.makeText(ctx, "IP скопирован", Toast.LENGTH_SHORT).show()
            }
        }
        card.addView(ipRow)

        val geoText = buildGeoLabel(lat, lon, ipLat, ipLon, city, region, country)
        if (geoText != null) {
            val geoRow = TextView(ctx).apply {
                text = "Приблизительно: $geoText"
                setTextColor(Color.parseColor("#A37C7C"))
                textSize = 12f
                setPadding(0, 6, 0, 2)
            }
            card.addView(geoRow)
        }

        val geo = geoQuery(lat, lon, ipLat, ipLon, city, region)

        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            setPadding(0, 14, 0, 0)
        }

        if (geo != null) {
            val mapBtn = actionBtn(ctx, "КАРТА", "#FF2D2D").apply {
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geo)))
                }
            }
            actions.addView(mapBtn)
        }

        if (!isCurrent) {
            val kickBtn = actionBtn(ctx, "ВЫГНАТЬ", "#3A2222")
            kickBtn.setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Выгнать устройство")
                    .setMessage("Сессия «$device» будет завершена. Устройству придётся входить заново. Продолжить?")
                    .setPositiveButton("ВЫГНАТЬ") { _, _ ->
                        kickBtn.isEnabled = false
                        lifecycleScope.launch {
                            try {
                                val r = withContext(Dispatchers.IO) { ApiClient.deviceKick(token, id) }
                                if (r.optBoolean("ok")) list.removeView(card)
                                else Toast.makeText(ctx, r.optString("error", "Ошибка"), Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            actions.addView(kickBtn)

            val transferBtn = actionBtn(ctx, "ПЕРЕДАТЬ ПРАВА", "#FF2D2D")
            transferBtn.setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Передать главное устройство")
                    .setMessage("Устройство «$device» станет единственным, которое входит без кода. Все остальные, включая это, будут требовать код при входе. Продолжить?")
                    .setPositiveButton("ПЕРЕДАТЬ") { _, _ ->
                        transferBtn.isEnabled = false
                        lifecycleScope.launch {
                            try {
                                val r = withContext(Dispatchers.IO) { ApiClient.deviceTransfer(token, id) }
                                if (!r.optBoolean("ok")) Toast.makeText(ctx, r.optString("error", "Ошибка"), Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            actions.addView(transferBtn)
        } else {
            val meBtn = actionBtn(ctx, "ЭТО ВЫ", "#3A2222")
            actions.addView(meBtn)
        }

        card.addView(actions)
        return card
    }

    private fun badge(ctx: Context, text: String, color: String) = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.parseColor("#F4E8E8"))
        setBackgroundColor(Color.parseColor(color))
        setPadding(10, 4, 10, 4)
        textSize = 9f
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = 6
        layoutParams = lp
    }

    private fun actionBtn(ctx: Context, text: String, color: String) = TextView(ctx).apply {
        this.text = text
        setTextColor(Color.parseColor("#F4E8E8"))
        setBackgroundColor(Color.parseColor(color))
        setPadding(20, 10, 20, 10)
        textSize = 12f
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginStart = 8
        layoutParams = lp
    }

    private fun buildGeoLabel(lat: Double, lon: Double, ipLat: Double, ipLon: Double, city: String, region: String, country: String): String? {
        if (lat.isNaN() || lon.isNaN()) {
            if (ipLat.isNaN() || ipLon.isNaN()) {
                val place = listOf(city, region, country).filter { it.isNotBlank() }.joinToString(", ")
                return if (place.isBlank()) null else place
            }
            return "по IP: ${fmt(ipLat)}, ${fmt(ipLon)}"
        }
        return "${fmt(lat)}, ${fmt(lon)}"
    }

    private fun geoQuery(lat: Double, lon: Double, ipLat: Double, ipLon: Double, city: String, region: String): String? {
        if (!lat.isNaN() && !lon.isNaN()) return "https://www.google.com/maps?q=${fmt(lat)},${fmt(lon)}"
        if (!ipLat.isNaN() && !ipLon.isNaN()) return "https://www.google.com/maps?q=${fmt(ipLat)},${fmt(ipLon)}"
        val place = listOf(city, region).filter { it.isNotBlank() }.joinToString(", ")
        if (place.isBlank()) return null
        return "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(place)
    }

    private fun fmt(v: Double) = String.format(java.util.Locale.US, "%.5f", v)
}