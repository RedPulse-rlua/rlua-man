package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn(this)) {
            lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) { ApiClient.me(SessionManager.token(this@LoginActivity)!!) }
                    if (res.optBoolean("ok") && !res.isNull("user")) { navigateToLobby(); return@launch }
                } catch (_: Exception) {}
                SessionManager.clear(this@LoginActivity)
            }
        }
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnLogin.setOnClickListener { doLogin() }
        b.btnRegister.setOnClickListener { doRegister() }
        b.btnSwitchToRegister.setOnClickListener { b.loginForm.visibility = View.GONE; b.registerForm.visibility = View.VISIBLE; hideError(); hideSuccess() }
        b.btnSwitchToLogin.setOnClickListener { b.loginForm.visibility = View.VISIBLE; b.registerForm.visibility = View.GONE; hideError(); hideSuccess() }
    }

    private fun doLogin() {
        val u = b.inputUsername.text.toString().trim()
        val w = b.inputWord.text.toString()
        val p = b.inputPassword.text.toString()
        if (u.isEmpty() || w.isEmpty() || p.isEmpty()) { showError("Заполните все поля"); return }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.login(u, w, p) }
                setLoading(false)
                if (!res.optBoolean("ok")) { showError(res.optString("error", "Ошибка")); return@launch }
                if (res.optBoolean("needsVerification")) {
                    showSuccess(res.optString("message", "Ожидание подтверждения..."))
                    startActivity(Intent(this@LoginActivity, VerifyActivity::class.java).apply {
                        putExtra("verifyId", res.optString("verifyId", ""))
                        putExtra("is_new_device", true)
                    }); return@launch
                }
                SessionManager.save(this@LoginActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                navigateToLobby()
            } catch (e: Exception) { setLoading(false); showError("Ошибка сети: ${e.message}") }
        }
    }

    private fun doRegister() {
        val u = b.regUsername.text.toString().trim()
        val w = b.regWord.text.toString()
        val p = b.regPassword.text.toString()
        if (u.isEmpty() || w.isEmpty() || p.isEmpty()) { showError("Заполните все поля"); return }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.register(u, w, p) }
                setLoading(false)
                if (!res.optBoolean("ok")) { showError(res.optString("error", "Ошибка")); return@launch }
                SessionManager.save(this@LoginActivity, res.optString("token"), res.optString("username"), res.optString("role", "user"), res.optInt("id", 0))
                navigateToLobby()
            } catch (e: Exception) { setLoading(false); showError("Ошибка сети: ${e.message}") }
        }
    }

    private fun navigateToLobby() { startActivity(Intent(this, LobbyActivity::class.java)); finish() }
    private fun showError(msg: String) { b.errorText.text = msg; b.errorText.visibility = View.VISIBLE; b.successText.visibility = View.GONE }
    private fun showSuccess(msg: String) { b.successText.text = msg; b.successText.visibility = View.VISIBLE; b.errorText.visibility = View.GONE }
    private fun hideError() { b.errorText.visibility = View.GONE }
    private fun hideSuccess() { b.successText.visibility = View.GONE }
    private fun setLoading(on: Boolean) { b.progressBar.visibility = if (on) View.VISIBLE else View.GONE; b.btnLogin.isEnabled = !on; b.btnRegister.isEnabled = !on }
}
