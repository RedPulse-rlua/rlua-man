package com.rlua.man.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE = "https://rlua.pages.dev"
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun get(path: String, token: String? = null): JSONObject {
        val builder = Request.Builder().url(BASE + path).get()
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        val resp = client.newCall(builder.build()).execute()
        return JSONObject(resp.body?.string() ?: "{}")
    }

    fun post(path: String, json: JSONObject, token: String? = null): JSONObject {
        val builder = Request.Builder().url(BASE + path).post(json.toString().toRequestBody(JSON_TYPE))
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        val resp = client.newCall(builder.build()).execute()
        return JSONObject(resp.body?.string() ?: "{}")
    }

    fun login(username: String, word: String, password: String) = post("/api/login", JSONObject().apply { put("username", username); put("word", word); put("password", password) })
    fun register(username: String, word: String, password: String) = post("/api/register", JSONObject().apply { put("username", username); put("word", word); put("password", password) })
    fun me(token: String) = get("/api/me", token)
    fun logout(token: String) = post("/api/logout", JSONObject(), token)
    fun verifyPending(token: String) = get("/api/verify/pending", token)
    fun verifyCode(token: String, id: String) = post("/api/verify/code", JSONObject().put("id", id), token)
    fun verifyConfirm(token: String, id: String, action: String, code: String? = null) = post("/api/verify/confirm", JSONObject().apply { put("id", id); put("action", action); if (code != null) put("code", code) }, token)
    fun verifyComplete(id: String, code: String) = post("/api/verify/complete", JSONObject().apply { put("id", id); put("code", code) })
    fun adminUsers(token: String) = get("/api/admin/users", token)
    fun adminLogs(token: String) = get("/api/admin/logs", token)
    fun adminNotifications(token: String) = get("/api/admin/notifications", token)
    fun fs(token: String) = get("/api/fs", token)
    fun vizorCreate(token: String) = post("/api/vizor/create", JSONObject(), token)
    fun vizorJoin(token: String, code: String) = post("/api/vizor/join", JSONObject().put("code", code), token)
    fun vizorRoom(token: String, code: String) = get("/api/vizor/room?code=$code", token)
    fun obfuscate(token: String, code: String, mode: String = "heavy") = post("/api/obfuscate", JSONObject().apply { put("code", code); put("mode", mode) }, token)
}
