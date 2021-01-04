package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 지각 OR 정시 출근 (10% 확률로 지각)
 */
class GoingOffice: State {

    companion object {
        private val current = DeveloperState.GOING_OFFICE
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("하 늦을라나..")

        context.state = when(Random.nextInt(10)) {
            0 -> Late()
            else -> OnTime()
        }
    }
}