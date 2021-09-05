package com.floidea.example

import com.floidea.example.subscriber.TestSubscriber
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

fun main() {
    var i = 1
    val upstream = Flux.generate<Int> {
        runBlocking { delay(500L) }
        it.next(i++)
    }.log()

    val downstream = upstream
        .publishOn(Schedulers.boundedElastic(), 4)

    downstream
        .subscribe(TestSubscriber<Int>())


}
