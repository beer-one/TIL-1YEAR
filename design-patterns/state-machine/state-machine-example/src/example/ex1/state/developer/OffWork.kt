package example.ex1.state.developer

import example.ex1.context.Context
import example.ex1.state.State
import kotlin.random.Random

/**
 * 퇴근 -> 집
 */
class OffWork: State {

    companion object {
        private val current = DeveloperState.OFF_WORK
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("퇴근 후 집가서 맥주 마시면서 넷플릭스 보는게 국룰! 호다닥!")

        context.state = InHome()
    }
}