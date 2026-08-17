package com.rlua.man.api

import android.content.Context

object SessionManager {
    private const val PREF = "rlua_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"
    private const val KEY_ID = "id"

    fun save(ctx: Context, token: String?, username: String?, role: String?, id: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, username)
            putString(KEY_ROLE, role)
            putInt(KEY_ID, id)
            apply()
        }
    }

    fun token(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
    fun username(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)
    fun role(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ROLE, null)
    fun id(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_ID, 0)

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
