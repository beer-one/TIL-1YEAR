package com.til.domain.company

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "COMPANY")
@Entity
data class Company(
    @Id
    val companyNo: Int,

    val name: String
)