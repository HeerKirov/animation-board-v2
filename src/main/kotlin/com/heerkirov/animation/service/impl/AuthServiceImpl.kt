package com.heerkirov.animation.service.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.exception.AuthenticationException
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.InternalException
import com.heerkirov.animation.service.AuthService
import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toJSONString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthServiceImpl(@Autowired private val client: OkHttpClient,
                      @Value("\${basic-service.url}") private val bsURL: String,
                      @Value("\${basic-service.app-id}}") private val bsAppId: String,
                      @Value("\${basic-service.app-secret}") private val bsAppSecret: String) : AuthService {

    override fun authenticate(token: String): String {
        val url = "$bsURL/api/interface/verify/"
        val body = TokenReq(bsAppSecret, token).toJSONString().toRequestBody("application/json".toMediaType())
        val res = try { client.newCall(Request.Builder().url(url).post(body).build()).execute() }catch (e: Throwable) {
            throw InternalException(e.message)
        }
        if(!res.isSuccessful) {
            throw AuthenticationException(ErrCode.AUTHENTICATION_FAILED, "Authentication failed: ${res.body?.string() ?: ""}")
        }
        return res.body?.string()?.parseJSONObject<TokenRes>()?.username ?: throw InternalException("No authentication message.")
    }

    override fun getInfo(username: String): Info {
        val url = "$bsURL/api/interface/info/get/"
        val body = InfoReq(bsAppSecret, username).toJSONString().toRequestBody("application/json".toMediaType())
        val res = try { client.newCall(Request.Builder().url(url).post(body).build()).execute() }catch (e: Throwable) {
            throw InternalException(e.message)
        }
        if(!res.isSuccessful) {
            throw InternalException("Cannot find info of user '$username'.")
        }
        return res.body?.string()?.parseJSONObject<Info>() ?: throw InternalException("No info content.")
    }

    private data class TokenReq(val secret: String, val token: String)

    private data class TokenRes(val username: String)

    private data class InfoReq(val secret: String, val username: String)

    data class Info(val username: String, val name: String, @JsonProperty("is_staff") val isStaff: Boolean, val info: String?)
}