package example.ex1.context

import example.ex1.state.State

/**
 * State를 가지는 클래스 인터페이스
 */
interface Context {

    var state: State

    /**
     * 실행
     */
    fun operate()
}