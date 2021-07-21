package com.til.presentation.handler

import com.til.domain.company.CompanyRepository
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class TestHandler(
    private val companyRepository: CompanyRepository
) {
    suspend fun getCompany(request: ServerRequest): ServerResponse {
        val companyNo = request.pathVariable("companyNo").toInt()

        return companyRepository.findById(companyNo).awaitFirst()
            .let { ok().bodyValueAndAwait(it) }
    }
}