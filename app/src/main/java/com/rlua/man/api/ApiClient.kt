package com.rlua.man.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE = "https://rlua.pages.dev"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .build()
            chain.proceed(req)
        }
        .build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun get(path: String, token: String? = null): JSONObject {
        val builder = Request.Builder().url(BASE + path).get()
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        return JSONObject(client.newCall(builder.build()).execute().body?.string() ?: "{}")
    }

    fun post(path: String, json: JSONObject, token: String? = null): JSONObject {
        val builder = Request.Builder().url(BASE + path).post(json.toString().toRequestBody(JSON_TYPE))
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        return JSONObject(client.newCall(builder.build()).execute().body?.string() ?: "{}")
    }

    fun login(username: String, word: String, password: String, lat: Double? = null, lon: Double? = null) =
        post("/api/login", JSONObject().apply {
            put("username", username); put("word", word); put("password", password)
            if (lat != null && lon != null) { put("lat", lat); put("lon", lon) }
        })

    fun register(username: String, word: String, password: String, lat: Double? = null, lon: Double? = null) =
        post("/api/register", JSONObject().apply {
            put("username", username); put("word", word); put("password", password)
            if (lat != null && lon != null) { put("lat", lat); put("lon", lon) }
        })

    fun me(token: String) = get("/api/me", token)
    fun logout(token: String) = post("/api/logout", JSONObject(), token)
    fun verifyPending(token: String) = get("/api/verify/pending", token)
    fun verifyCode(token: String, id: String) = post("/api/verify/code", JSONObject().put("id", id), token)
    fun verifyConfirm(token: String, id: String, action: String, code: String? = null) =
        post("/api/verify/confirm", JSONObject().apply { put("id", id); put("action", action); if (code != null) put("code", code) }, token)
    fun verifyComplete(id: String, code: String) =
        post("/api/verify/complete", JSONObject().apply { put("id", id); put("code", code) })
    fun geo(token: String, lat: Double, lon: Double) =
        post("/api/geo", JSONObject().apply { put("lat", lat); put("lon", lon) }, token)
    fun devices(token: String) = get("/api/devices", token)
    fun deviceKick(token: String, id: String) = post("/api/devices/kick", JSONObject().put("id", id), token)
    fun deviceTransfer(token: String, id: String) = post("/api/devices/transfer", JSONObject().put("id", id), token)
}
