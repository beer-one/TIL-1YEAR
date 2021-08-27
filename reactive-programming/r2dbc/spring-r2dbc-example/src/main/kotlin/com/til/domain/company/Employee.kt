package com.til.domain.company

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "EMPLOYEE")
@Entity
data class Employee(
    @Id
    val employeeNo: Int,

    val name: String,

    val companyNo: Int
)