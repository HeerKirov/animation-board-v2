package com.heerkirov.animation.configuration

import com.heerkirov.animation.aspect.authorization.AuthorizationResolver
import com.heerkirov.animation.aspect.filter.FilterResolver
import com.heerkirov.animation.aspect.validation.ValidationResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport

@Configuration
class SpringConfiguration : WebMvcConfigurationSupport() {
    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.apply {
            add(AuthorizationResolver())
            add(ValidationResolver())
            add(FilterResolver())
        }
    }
}