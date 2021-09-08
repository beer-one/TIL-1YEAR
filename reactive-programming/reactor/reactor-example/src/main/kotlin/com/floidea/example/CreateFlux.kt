package com.floidea.example

import reactor.core.publisher.Flux

fun main() {
//    val primeFlux = Flux.generate<Int, MutableSet<Int>>(
//        { mutableSetOf() },
//        { primeSet, sink ->
//            var next = primeSet.maxOrNull()?.plus(1) ?: 0
//
//            if (next == 0) {
//                sink.next(2)
//                primeSet.add(2)
//            } else {
//                while (primeSet.any { next % it == 0 }) {
//                    next++
//                }
//                primeSet.add(next)
//                sink.next(next);
//            }
//            primeSet
//        },
//    )
//
//    primeFlux.subscribe {
//        println(it)
//    }
//
Flux.fromIterable(1..100)
    .handle<Int> { num, sink ->
        if (num % 5 != 0) sink.next(num)
    }
    .subscribe { println(it) }

}

internal interface MyEventListener<T> {
    fun onDataChunk(chunk: List<T>?)
    fun processComplete()
}