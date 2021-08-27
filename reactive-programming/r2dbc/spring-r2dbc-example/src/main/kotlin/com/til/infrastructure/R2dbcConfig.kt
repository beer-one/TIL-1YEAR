package com.til.infrastructure

import dev.miku.r2dbc.mysql.MySqlConnectionConfiguration
import dev.miku.r2dbc.mysql.MySqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories
class R2dbcConfig(
    private val properties: R2dbcProperties
) {

    @Bean
    fun connectionFactory(): ConnectionFactory {
        return MySqlConnectionFactory.from(
            MySqlConnectionConfiguration.builder()
                .host("192.168.0.4")
                .port(3306)
                .database("testdb")
                .username(properties.username)
                .password(properties.password)
                .build()
        )
    }

    @Bean
    fun r2dbcEntityTemplate(): R2dbcEntityTemplate {
        val databaseClient = DatabaseClient.create(connectionFactory())

        return R2dbcEntityTemplate(databaseClient)
    }
}