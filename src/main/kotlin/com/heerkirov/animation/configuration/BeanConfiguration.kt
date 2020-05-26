package com.heerkirov.animation.configuration

import com.aliyun.oss.OSS
import com.aliyun.oss.OSSClientBuilder
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanConfiguration {
    @Bean
    fun getOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Bean
    fun getOSSClient(@Value("\${oss.endpoint}") endpoint: String,
                     @Value("\${oss.access-key-id}") accessKeyId: String,
                     @Value("\${oss.access-key-secret}") accessKeySecret: String): OSS {
        return OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret)
    }
}