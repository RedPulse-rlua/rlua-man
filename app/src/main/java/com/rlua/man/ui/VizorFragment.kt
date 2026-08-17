package com.rlua.man.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.FragmentVizorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VizorFragment : Fragment() {
    private var _b: FragmentVizorBinding? = null
    private val b get() = _b!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { _b = FragmentVizorBinding.inflate(inflater, container, false); return b.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = SessionManager.token(requireContext()) ?: return
        b.btnVizorCreate.setOnClickListener {
            lifecycleScope.launch {
                val res = withContext(Dispatchers.IO) { ApiClient.vizorCreate(token) }
                if (res.optBoolean("ok")) { b.vizorCodeInput.setText(res.optString("code", "")); b.vizorStatus.text = "Комната: ${res.optString("code", "")}"; b.vizorStatus.visibility = View.VISIBLE }
                else { b.vizorStatus.text = res.optString("error", "Ошибка"); b.vizorStatus.visibility = View.VISIBLE }
            }
        }
        b.btnVizorJoin.setOnClickListener {
            val code = b.vizorCodeInput.text.toString().trim()
            if (code.isEmpty()) { Toast.makeText(requireContext(), "Введите код", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            lifecycleScope.launch {
                val res = withContext(Dispatchers.IO) { ApiClient.vizorJoin(token, code) }
                if (res.optBoolean("ok")) { b.vizorStatus.text = "Подключено к $code"; b.vizorStatus.visibility = View.VISIBLE }
                else { b.vizorStatus.text = res.optString("error", "Ошибка"); b.vizorStatus.visibility = View.VISIBLE }
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
