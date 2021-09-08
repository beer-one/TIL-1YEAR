package com.floidea.example

import reactor.core.publisher.Mono

fun main() {
    val mono = Mono.just("Hello")

    val thread = Thread {
        mono.map { "$it thread" }
            .subscribe { println("$it ${Thread.currentThread().name}") }
    }

    thread.start()
    thread.join()
}