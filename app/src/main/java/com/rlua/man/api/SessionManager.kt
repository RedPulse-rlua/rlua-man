package com.rlua.man.api

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS = "rlua_session"
    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun save(ctx: Context, token: String, username: String, role: String, id: Int) { prefs(ctx).edit().apply { putString("token", token); putString("username", username); putString("role", role); putInt("user_id", id); apply() } }
    fun token(ctx: Context): String? = prefs(ctx).getString("token", null)
    fun username(ctx: Context): String? = prefs(ctx).getString("username", null)
    fun role(ctx: Context): String? = prefs(ctx).getString("role", null)
    fun userId(ctx: Context): Int = prefs(ctx).getInt("user_id", 0)
    fun clear(ctx: Context) { prefs(ctx).edit().clear().apply() }
    fun isLoggedIn(ctx: Context): Boolean = token(ctx) != null
}
