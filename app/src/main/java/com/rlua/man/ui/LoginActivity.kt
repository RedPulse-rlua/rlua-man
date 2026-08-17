package com.rlua.man.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rlua.man.R
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var authSection: View
    private lateinit var verifySection: View
    private lateinit var inputUsername: EditText
    private lateinit var inputWord: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputCode: EditText
    private lateinit var errorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogin: TextView
    private lateinit var btnRegister: TextView
    private lateinit var btnSubmitCode: TextView
    private lateinit var verifyStatus: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pollRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authSection = findViewById(R.id.authSection)
        verifySection = findViewById(R.id.verifySection)
        inputUsername = findViewById(R.id.inputUsername)
        inputWord = findViewById(R.id.inputWord)
        inputPassword = findViewById(R.id.inputPassword)
        inputCode = findViewById(R.id.inputCode)
        errorText = findViewById(R.id.errorText)
        progressBar = findViewById(R.id.progressBar)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnSubmitCode = findViewById(R.id.btnSubmitCode)
        verifyStatus = findViewById(R.id.verifyStatus)

        btnLogin.setOnClickListener { doLogin() }
        btnRegister.setOnClickListener { doRegister() }

        findViewById<TextView>(R.id.btnVerifyCancel).setOnClickListener {
            pollRunning = false
            verifySection.visibility = View.GONE
            authSection.visibility = View.VISIBLE
        }

        btnSubmitCode.setOnClickListener {
            val code = inputCode.text.toString().trim()
            if (code.length != 6) { showError("Введите 6-значный код"); return@setOnClickListener }
            val vid = verifySection.tag as? String ?: return@setOnClickListener
            submitCode(vid, code)
        }
    }

    private fun doLogin() {
        val u = inputUsername.text.toString().trim()
        val w = inputWord.text.toString()
        val p = inputPassword.text.toString()
        if (u.isEmpty() || w.isEmpty() || p.isEmpty()) { showError("Заполните все поля"); return }
        setLoading(true); hideError()
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.login(u, w, p) }
                setLoading(false)
                if (!res.optBoolean("ok")) { showError(res.optString("error", "Ошибка")); return@launch }
                if (res.optBoolean("needsVerification")) { showVerifyScreen(res.optString("verifyId")); return@launch }
                authSucceeded(res)
            } catch (e: Exception) { setLoading(false); showError("Ошибка: ${e.message}") }
        }
    }

    private fun doRegister() {
        val u = inputUsername.text.toString().trim()
        val w = inputWord.text.toString()
        val p = inputPassword.text.toString()
        if (u.isEmpty() || w.isEmpty() || p.isEmpty()) { showError("Заполните все поля"); return }
        setLoading(true); hideError()
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.register(u, w, p) }
                setLoading(false)
                if (!res.optBoolean("ok")) { showError(res.optString("error", "Ошибка")); return@launch }
                authSucceeded(res)
            } catch (e: Exception) { setLoading(false); showError("Ошибка: ${e.message}") }
        }
    }

    private fun showVerifyScreen(verifyId: String) {
        authSection.visibility = View.GONE
        verifySection.visibility = View.VISIBLE
        verifySection.tag = verifyId
        verifyStatus.text = "Ожидание подтверждения..."
        inputCode.visibility = View.GONE
        btnSubmitCode.visibility = View.GONE
        inputCode.text.clear()
        pollRunning = true
        pollVerify(verifyId)
    }

    private fun pollVerify(verifyId: String) {
        if (!pollRunning) return
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.verifyComplete(verifyId, "") }
                if (!pollRunning) return@launch
                if (res.optBoolean("ok") && res.has("token")) {
                    pollRunning = false
                    SessionManager.save(this@LoginActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                    goMain(); return@launch
                }
                if (res.optString("status") == "confirmed") {
                    pollRunning = false
                    verifyStatus.text = "Подтверждено! Введи 6-значный код из MAIL"
                    inputCode.visibility = View.VISIBLE
                    btnSubmitCode.visibility = View.VISIBLE; return@launch
                }
                val err = res.optString("error", "")
                if (err.contains("отклонён") || err.contains("заблокирован")) { pollRunning = false; verifyStatus.text = "Вход отклонён"; return@launch }
            } catch (_: Exception) {}
            if (pollRunning) mainHandler.postDelayed({ pollVerify(verifyId) }, 2000)
        }
    }

    private fun submitCode(verifyId: String, code: String) {
        btnSubmitCode.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.verifyComplete(verifyId, code) }
                btnSubmitCode.isEnabled = true
                if (res.optBoolean("ok") && res.has("token")) {
                    authSucceeded(res); return@launch
                }
                showError(res.optString("error", "Неверный код"))
            } catch (e: Exception) { btnSubmitCode.isEnabled = true; showError("Ошибка: ${e.message}") }
        }
    }

    private fun goMain() { startActivity(Intent(this, MainActivity::class.java)); finish() }
    private fun showError(msg: String) { errorText.text = msg; errorText.visibility = View.VISIBLE }
    private fun hideError() { errorText.visibility = View.GONE }
    private fun setLoading(on: Boolean) { progressBar.visibility = if (on) View.VISIBLE else View.GONE; btnLogin.isEnabled = !on; btnRegister.isEnabled = !on }

    private fun authSucceeded(res: org.json.JSONObject) {
        SessionManager.save(this, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
        requestGeoAndGo(res.optString("token"))
    }

    private fun requestGeoAndGo(token: String) {
        val prefs = getSharedPreferences("rlua", MODE_PRIVATE)
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (prefs.getBoolean("geo_asked", false)) {
            if (granted) sendGeo(token)
            goMain()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("ВНИМАНИЕ")
            .setMessage("Мы запрашиваем доступ к геопозиции, чтобы в приложении вы могли видеть примерное местоположение незнакомца, пытающегося войти в ваш аккаунт, — для него откроется ссылка на карту.\n\nЕсли вы не используете мобильное приложение для отслеживания входов — можете отказаться.\n\nГеопозиция видна только владельцу аккаунта во вкладке УСТРОЙСТВА: администраторы и другие пользователи не имеют к ней доступа. Данные используются исключительно для защиты аккаунта.")
            .setPositiveButton("Разрешить") { _, _ ->
                prefs.edit().putBoolean("geo_asked", true).apply()
                if (granted) { sendGeo(token); goMain() }
                else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
            }
            .setNegativeButton("Отказаться") { _, _ ->
                prefs.edit().putBoolean("geo_asked", true).apply()
                goMain()
            }
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val token = SessionManager.token(this) ?: run { goMain(); return }
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) sendGeo(token)
            else Toast.makeText(this, "Геопозиция не будет передаваться. Это можно изменить в настройках системы", Toast.LENGTH_LONG).show()
            goMain()
        }
    }

    private fun sendGeo(token: String) {
        lifecycleScope.launch {
            try {
                val location = withContext(Dispatchers.IO) {
                    val lm = getSystemService(LOCATION_SERVICE) as LocationManager
                    val gps = if (ContextCompat.checkSelfPermission(this@LoginActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                    gps ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                    withContext(Dispatchers.IO) { ApiClient.geo(token, location.latitude, location.longitude) }
                } else {
                    mainHandler.post { Toast.makeText(this@LoginActivity, "Геопозиция недоступна — включите GPS и зайдите снова", Toast.LENGTH_LONG).show() }
                }
            } catch (_: Exception) {}
        }
    }
}
