package com.rlua.man.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rlua.man.api.ApiClient
import com.rlua.man.api.SessionManager
import com.rlua.man.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View { _b = FragmentProfileBinding.inflate(inflater, container, false); return b.root }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.profileUsername.text = SessionManager.username(requireContext()) ?: "?"
        b.profileRole.text = SessionManager.role(requireContext()) ?: "user"
        b.profileId.text = "${SessionManager.userId(requireContext())}"
        b.btnProfileLogout.setOnClickListener {
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { SessionManager.token(requireContext())?.let { ApiClient.logout(it) } } } catch (_: Exception) {}
                SessionManager.clear(requireContext())
                startActivity(Intent(requireContext(), LoginActivity::class.java)); activity?.finish()
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
