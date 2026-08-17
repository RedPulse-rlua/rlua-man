package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
                SessionManager.save(this@LoginActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                goMain()
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
                SessionManager.save(this@LoginActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                goMain()
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
                val err = res.optString("error", "")
                if (err.contains("отклонён")) { pollRunning = false; verifyStatus.text = "Вход отклонён"; return@launch }
                if (err.contains("нужен код") || err.contains("code")) {
                    pollRunning = false
                    verifyStatus.text = "Введите код из основного устройства"
                    inputCode.visibility = View.VISIBLE
                    btnSubmitCode.visibility = View.VISIBLE; return@launch
                }
            } catch (_: Exception) {}
            if (pollRunning) mainHandler.postDelayed({ pollVerify(verifyId) }, 3000)
        }
    }

    private fun submitCode(verifyId: String, code: String) {
        btnSubmitCode.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.verifyComplete(verifyId, code) }
                btnSubmitCode.isEnabled = true
                if (res.optBoolean("ok") && res.has("token")) {
                    SessionManager.save(this@LoginActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                    goMain(); return@launch
                }
                showError(res.optString("error", "Неверный код"))
            } catch (e: Exception) { btnSubmitCode.isEnabled = true; showError("Ошибка: ${e.message}") }
        }
    }

    private fun goMain() { startActivity(Intent(this, MainActivity::class.java)); finish() }
    private fun showError(msg: String) { errorText.text = msg; errorText.visibility = View.VISIBLE }
    private fun hideError() { errorText.visibility = View.GONE }
    private fun setLoading(on: Boolean) { progressBar.visibility = if (on) View.VISIBLE else View.GONE; btnLogin.isEnabled = !on; btnRegister.isEnabled = !on }
}
