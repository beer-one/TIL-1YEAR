package com.til.domain.company

import org.springframework.data.r2dbc.repository.R2dbcRepository

interface CompanyRepository : R2dbcRepository<Company, Int>