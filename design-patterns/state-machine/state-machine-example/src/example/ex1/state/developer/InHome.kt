package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 휴일 -> HOLIDAY
 * 평일 -> GOING_OFFICE
 */
class InHome: State {

    companion object {
        private val current = DeveloperState.IN_HOME
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("하 몇시지,,? 8시네")

        context.state = when {
            Random.nextInt() % 7 < 2 -> Holiday()
            else -> GoingOffice()
        }
    }
}