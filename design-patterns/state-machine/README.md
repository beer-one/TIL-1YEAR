# State Machine Pattern



## 목적

State 디자인 패턴의 목적은 *"내부 상태가 변할 때 객체가 동작을 변경하도록 하기 위함"* 이다.

State 패턴으로 해결할 수 있는 문제는 아래와 같다.

*  상태와 상태에 대한 행동이 주어지고, 상태가 변할 때 그 상태에 대한 행동을 실행하고자 할 때
* 객체의 상태와 상태 별 행동을 정의하고 새로운 상태가 추가되거나 기존의 상태에 대한 행동이 다른 상태와는 독립적으로 변경하고자 할 때



## 이 패턴을 왜 생각했냐?

![스크린샷 2021-01-05 오전 12 39 36](https://user-images.githubusercontent.com/35602698/103554291-00f46180-4ef2-11eb-9457-0efae99578e2.png)

위와 같이 상태와 상태에 대한 행동이 주어지고, 상태가 변할 때 그 상태에 대한 행동을 실행하도록 하는 구조를 구현하는 방법 중 하나는 상태별 행동을 직접 내부 상태에 의존하는 클래스에 정의하는 것이다. 상태와 상태별 행동을 클래스 내부에 정의할 때는 조건문이나 switch문 등을 사용하면 된다. 각 조건 브랜치는 상태에 따른 행동들을 구현한다. 

하지만 이런 방법은 클래스를 특정 상태별 행동을 정의하고 나중에 클래스로부터 **독립적**으로 새로운 상태를 추가하거나 기존 상태의 행동을 변경하는 것을 불가능하게 한다 (조건문 추가 / 변경을 해야 하므로). 그리고 상태별 동작을 포함하는 클래스들은 구현, 변경, 테스트, 재사용 등을 하기에 더 어려울 것이다. 이러한 문제 때문에 GoF는 새로운 상태를 추가하고 기존 상태에  대한 행동이 독립적으로 변할 수 있게 하기 위해 여러 접근법을 생각하였다. 



## 해결법

위와 같은 문제를 해결하기 위해 State 디자인 패턴이 탄생하게 된다. 

![스크린샷 2021-01-05 오전 12 18 54](https://user-images.githubusercontent.com/35602698/103554326-136e9b00-4ef2-11eb-80f7-cb4292feaa3e.png)

State 디자인 패턴은 상태별 행동을 상태별로 State 객체로 분리시킨 후 캡슐화 한다. 

상태를 가지는 클래스는 직접 상태별 행동을 자신의 클래스 내부에 구현하지 않고 상태별 행동을 현재 State 객체에게 위임한다. 



* 이 패턴의 핵심 아이디어는 객체의 상태별 행동들을 State 객체로 캡슐화 하는 것이다. 이렇게 캡슐화 했을 때 이점은 객체의 상태를 별도의 State 객체로 관리하여 다른 State 객체와 독립적으로 변경할 수 있도록 할 수 있다.
* State 객체의 구현은 다음과 같다.
  * 모든 상태에 대해서, 일단 State interface를 정의한다. State interface에는 상태에 대한 행동을 정의하도록 하는 operation() 메서드를 정의한다.
  * 각 상태들은 State interface를 구현하는 Concrete class를 정의하면 된다.
  * 현재 상태가 변경되기만 하면 행동이 변경되기 때문에 별도의 조건문이 필요 없다.
  * "상속" 때문에 compile-time에 대한 유연성을 가진다. 모든 상태 별 코드는 State 서브클래스로 살아있고, 새로운 상태를 추가하는 것은 새로운 State 서브클래스를 정의함으로써 쉽게 가능하다.
* 상태를 가지는 클래스는 상태 별 행동을 실행하는 책임을 현재 State 객체로 위임한다.
  * "Object Composition" 때문에 run-time에 대한 유연성을 가진다. 런타임 시점에서 현재 상태를 변경함으로써 그의 행동을 변화시킬 수 있다.





## 구현에 대한 고민

### 클래스 구조

![스크린샷 2021-01-05 오전 12 18 54](https://user-images.githubusercontent.com/35602698/103554326-136e9b00-4ef2-11eb-80f7-cb4292feaa3e.png)

* Context (상태를 가지는 클래스)
  * 상태 별 행동을 실행하기 위해 State 인터페이스를 참조한다. 그리고 Context 클래스는 상태가 어떻게 구현되어 있는지 모른다. (몰라도 된다. / 독립적이다.)
* State (상태 클래스)
  * 모든 상태에 대해 상태 별 행동을 정의할 수 있도록 하는 interface를 정의한다.
* State 구현 클래스
  * 각 State에 대해 State 인터페이스를 구현한다.



### Dynamic Object Collaboration

* Context 객체는 상태 별 행동을 현재 상태 객체에 위임한다.
* 상호작용은 현재 상태 객체에 대한 행동을 호출하는 Context 객체에서 시작된다.
* Context 객체는 자기 자신을 State 객체에게 전달한다.
* 각 State는 자신의 행동을 실행한다. 그리고 행동이 실행되고 상태가 변경되어야 한다면 context.setState()를 호출하여 context 객체를 다른 상태로 변경한다.



## State Pattern 장단점

### 장점

* 새로운 상태를 쉽게 추가할 수 있다.
* 조건문을 사용하지 않아도 된다.
* 일관된 상태를 보장한다.
  * Context의 상태는 **현재 State 객체**에 의해서만 변경될 수 있기 때문에 상태에 대한 일괄성이 보장된다.
* 상태 변경이 암묵적으로 이루어진다.



### 단점

* Context 인터페이스를 확장해야 (만들어야) 할 수도 있다. 
  * State 객체가 Context의 상태를 변경하도록 Context 인터페이스를 확장해야 할 수도 있다.

> * Introduces an additional level of indirection.
>   *  State achieves flexibility by introducing an additional level of indirection (clients delegate to separate State objects), which makes clients dependent on a State object.





## 구현

먼저 Context와 State를 정의하는 인터페이스부터 작성하였다.

```kotlin
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
```

```kotlin
/**
 * State 인터페이스
 */
interface State {

    /**
     * 현재 State에 대한 행동을 실행하고 다음 State로 변경해야 한다면 변경한다.
     */
    fun operate(context: Context)
}
```



그리고 이 디자인 패턴에 대한 적당한 예시를 생각하다가 **개발자(직장인)에 대한 상태** 를 정의해보았다. 아래 그림은 개발자에 대한 상태를 Final State Machine으로 나타낸 그림이다.

![IMG_6A6B604BF556-1](https://user-images.githubusercontent.com/35602698/103562294-2dae7600-4efe-11eb-951e-c15dd86ce325.jpeg)


직장인은 평일에 출근하고 휴일에 쉰다. 일단 개발의 편의성을 위해 InHome 상태를 시작 상태로 두고, 휴일(Holiday) 상태가 되면 종료가 되도록 Holiday를 Final State로 두었다. 

평일이면 집에서 나와 출근을 할 것이다. (코로나 때문에 재택근무를 하지만 일단 출근(GoingOffice) 상태를 두었다.) 출근을 하다보면 지각(Late)할 수도 있고 정시 출근(OnTime) 할 수도 있다. 여기서는 10%확률로 지각하도록 구현하였다. 어쨌든 지각이든 정시 출근이든 왔으면 일을 한다. 일 하는 것은 편의상 오전(WorkingMorning) / 오후(WorkingAfternoon) / 야근(NigitShift) 타임으로 쪼갰다. (사실 야근은 무조건 별개로 쪼개는게.. 맞다..)

오전에 일을하고 나면 무조건 점심은 먹어야 한다. 점심을 먹고 또 일을 한다. (참고로 이 state machine은 `휴가`가 없다는 전제하에 만들어졌다.)

오후에 일을 하고 나면 이제 갈림길이 생긴다. 회사에서 저녁을 해결할지, 아니면 빠르게 퇴근해서 집에서 저녁을 먹을지 고민한다. 사실 회사에서 저녁을 먹는다면 **거의 야근 확정이다.** 그래서 저녁 상태가 되면 높은 확률로 야근을 하도록 구현하였다.

일단 오후 업무를 마치고 바로 퇴근하거나 저녁을 먹고 퇴근 하거나 심지어 야근을 하고 나서 퇴근하거나 어쨌든 퇴근을 하면 곧장 집으로 가는게 맞다. (지금은 코로나기 때문,,, 코로나가 아니라도 아무것도 하기 싫다.)

집에서는 자고 다음날 휴일인지 평일인지에 따라 일과가 달라진다. 



### DeveloperState 클래스

일단 FSM에 정의되어 있는 상태들을 State 인터페이스를 구현하는 클래스로 정의한다.

나는 먼저 Enum 클래스로 모든 state를 정의해보았다.

```kotlin
enum class DeveloperState {
    IN_HOME, 
    GOING_OFFICE, 
    ON_TIME, 
    LATE,
    WORKING_MORNING,
    LUNCH, 
    WORKING_AFTERNOON,
    OFF_WORK, 
    DINNER, 
    NIGHT_SHIFT,
    HOLIDAY;
}
```

그리고 각 State와 그에 맞는 행동들을 정의하는 클래스를 구현한다.

```kotlin
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


/**
 * 지각이면 혼나고 일해야지
 */
class Late: State {

    companion object {
        private val current = DeveloperState.LATE
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("죄송합니다,, (지각이다..)")
        println("일단 혼남..")

        context.state = WorkingMorning()
    }
}



/**
 * 왔으면 일해야지
 */
class OnTime: State {

    companion object {
        private val current = DeveloperState.ON_TIME
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("안녕하세욤~")

        context.state = WorkingMorning()
    }
}



/**
 * 일 -> 점심
 */
class WorkingMorning: State {

    companion object {
        private val current = DeveloperState.WORKING_MORNING
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("하 피곤해 점심 뭐지,,?")

        context.state = Lunch()
    }
}




/**
 * 점심 -> 다시 일
 */
class Lunch: State {

    companion object {
        private val current = DeveloperState.LUNCH
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("점심 꿀맛!")

        context.state = WorkingAfternoon()
    }
}



/**
 * 반은 회사에서 저녁 (야근 가능성)
 * 반은 퇴근
 */
class WorkingAfternoon: State {

    companion object {
        private val current = DeveloperState.WORKING_AFTERNOON
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("설마 오늘 야근..?")

        context.state = when (Random.nextInt(2)) {
            0 -> OffWork()
            else -> Dinner()
        }
    }
}



/**
 * 밥 -> 퇴근 or 야근 (저녁을 먹었다면 야근 가능성 상당히 높음)
 */
class Dinner: State {

    companion object {
        private val current = DeveloperState.DINNER
    }

    override fun operate(context: Context) {
        println("CURRENT STATE: $current")
        println("저녁먹고 퇴근 아니면 야근!")

        context.state = when(Random.nextInt(5)) {
            0 -> OffWork()
            else -> NightShift()
        }
    }
}



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
```

여기서 Holiday 상태만 다음 상태로 변경되지 않는다. Holiday는 Final state이기 때문이다.



### Developer 클래스

이제 개발자(Developer) 클래스를 정의한다. setter를 단순 세팅만 하는 것이 아니라 상태가 변경되면 operate() 메서드를 호출하도록 변경하였다. 이로써 state에서 operate 메서드만 호출해도 다음 상태로 변경과 동시에 그 상태에 맞는 행동을 실행시킬 수 있다.

```kotlin
class Developer: Context {

    override var state: State = InHome()
        set(value) {
            field = value
            this.operate()
        }

    override fun operate() {
        state.operate(this)
    }
}
```





### 실행 결과

대충 main 메서드를 작성하고 실행해보자.

```kotlin
fun main() {
    val seowonYun = Developer()

    seowonYun.operate()
}
```



실행하면 콘솔창에서 여러 확률로 달라지겠지만, 어쨌든 마지막엔 휴일이 찍힐 것이고 state machine에서 설계했던 대로 상태의 흐름이 진행될 것이다. 나는 첫 실행이 이렇게 나왔는데 바로 야근이 걸렸다..

```
CURRENT STATE: IN_HOME
하 몇시지,,? 8시네
CURRENT STATE: GOING_OFFICE
하 늦을라나..
CURRENT STATE: ON_TIME
안녕하세욤~
CURRENT STATE: WORKING_MORNING
하 피곤해 점심 뭐지,,?
CURRENT STATE: LUNCH
점심 꿀맛!
CURRENT STATE: WORKING_AFTERNOON
설마 오늘 야근..?
CURRENT STATE: DINNER
저녁먹고 퇴근 아니면 야근!
CURRENT STATE: NIGHT_SHIFT
야근이라니!!
CURRENT STATE: OFF_WORK
퇴근 후 집가서 맥주 마시면서 넷플릭스 보는게 국룰! 호다닥!
CURRENT STATE: IN_HOME
하 몇시지,,? 8시네
CURRENT STATE: HOLIDAY
와 휴일이다 ㅜㅜ 누워자자.
```

