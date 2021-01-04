package example.ex1.state

import example.ex1.context.Context

/**
 * State 인터페이스
 */
interface State {

    /**
     * 현재 State에 대한 행동을 실행하고 다음 State로 변경해야 한다면 변경한다.
     */
    fun operate(context: Context)
}