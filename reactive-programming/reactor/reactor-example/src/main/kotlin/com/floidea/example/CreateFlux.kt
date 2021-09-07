package com.floidea.example

import reactor.core.publisher.Flux

fun main() {
    val primeFlux = Flux.generate<Int, MutableSet<Int>>(
        { mutableSetOf() },
        { primeSet, sink ->
            var next = primeSet.maxOrNull()?.plus(1) ?: 0

            if (next == 0) {
                sink.next(2)
                primeSet.add(2)
            } else {
                while (primeSet.any { next % it == 0 }) {
                    next++
                }
                primeSet.add(next)
                sink.next(next);
            }
            primeSet
        },
    )

    val flux = Flux.create<Int> { sink ->
        
    }


    primeFlux.subscribe {
        println(it)
    }
}

internal interface MyEventListener<T> {
    fun onDataChunk(chunk: List<T>?)
    fun processComplete()
}