package com.til.domain.company

import javax.persistence.*

@Table(name = "COMPANY")
@Entity
data class Company(
    @Id
    val companyNo: Int,

    val name: String
) {

    @OneToMany
    @JoinColumn(name = "companyNo")
    val employees: List<Employee> = emptyList()
}