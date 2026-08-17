package com.rlua.man.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesFragment : Fragment() {
    private var _b: FragmentFilesBinding? = null
    private val b get() = _b!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { _b = FragmentFilesBinding.inflate(inflater, container, false); return b.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = SessionManager.token(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { ApiClient.fs(token) }
                val files = res.optJSONArray("files")
                if (files == null || files.length() == 0) { b.filesEmpty.visibility = View.VISIBLE; return@launch }
                val sb = StringBuilder()
                for (i in 0 until files.length()) { val f = files.getJSONObject(i); sb.appendLine("${f.optString("name", "?")}  (${f.optLong("size", 0) / 1024}KB)") }
                b.filesEmpty.text = sb.toString(); b.filesEmpty.visibility = View.VISIBLE
            } catch (_: Exception) { b.filesEmpty.visibility = View.VISIBLE }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
