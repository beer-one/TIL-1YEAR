package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 점심
 */
class NightShift: State {

    companion object {
        private val current = DeveloperState.NIGHT_SHIFT
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("야근이라니!!")

        context.state = OffWork()
    }
}