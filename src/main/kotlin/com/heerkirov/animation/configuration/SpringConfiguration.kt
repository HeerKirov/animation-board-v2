package com.heerkirov.animation.configuration

import com.heerkirov.animation.aspect.HttpLogInterceptor
import com.heerkirov.animation.aspect.authorization.AuthorizationResolver
import com.heerkirov.animation.aspect.filter.FilterResolver
import com.heerkirov.animation.aspect.validation.ValidationResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.*

@Configuration
class SpringConfiguration : WebMvcConfigurationSupport() {
    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.apply {
            add(AuthorizationResolver())
            add(ValidationResolver())
            add(FilterResolver())
        }
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(HttpLogInterceptor()).addPathPatterns("/api/**")
    }

    @Bean
    fun corsFilter(): CorsFilter {
        return CorsFilter(UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", CorsConfiguration().apply {
                allowCredentials = true
                allowedOrigins = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
            })
        })
    }
}