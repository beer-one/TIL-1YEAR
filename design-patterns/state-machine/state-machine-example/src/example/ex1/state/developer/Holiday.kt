package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 휴일 -> HOLIDAY
 * 평일 -> GOING_OFFICE
 *
 * FINAL STATE
 */
class Holiday: State {

    companion object {
        private val current = DeveloperState.HOLIDAY
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("와 휴일이다 ㅜㅜ 누워자자.")

        // END
    }
}