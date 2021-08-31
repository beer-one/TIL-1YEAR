package com.floidea.example

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun main() {
    val alcohols = Flux.just("beer", "soju", "wine")

    val sportList = listOf("Soccer", "Baseball", "Basketball")
    val sportFlux = Flux.fromIterable(sportList)

    val rangeFlux = Flux.range(5, 10).subscribe {
        println("$it")
    }

    val emptyMono = Mono.empty<String>()
    val languageMono = Mono.just("Kotlin")
}