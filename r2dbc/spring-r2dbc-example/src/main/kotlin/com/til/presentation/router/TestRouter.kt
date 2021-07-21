package com.til.presentation.router

import com.til.presentation.handler.TestHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class TestRouter(
    private val testHandler: TestHandler
) {

    @Bean
    fun memberRoute(): RouterFunction<ServerResponse> {
        return coRouter {
            "/company".nest {
                accept(MediaType.APPLICATION_JSON).nest {
                    GET("/{companyNo}", testHandler::getCompany)
                }
            }
        }
    }
}