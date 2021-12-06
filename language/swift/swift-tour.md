# Swift Tour

공식문서로만 한번 공부해보자. https://docs.swift.org/swift-book/GuidedTour/GuidedTour.html



## Hello, world!!

언어를 배우면 당연히 먼저 Hello, world! 부터 찍어봐야지. 스위프트 특징 중 하나인데 글로벌 스코프에서 작성된 코드는 프로그램의 entry point로 사용되기 때문에 C언어 처럼 `main()` 함수를 사용할 필요는 없다. 그래서 Hello, world 찍는 프로그램은 한 문장으로 끝난다.

```swift
print("Hello, world!")
```



## 변수 / 상수

`let` 키워드로 상수를 선언할 수 있고 `var` 키워드로 변수를 선언할 수 있다. 상수는 컴파일 타임에 값을 알 필요는 없지만 정확히 한 번 값을 할당해줘야 한다. (상수는 불변이다.)

```swift
var variable = 10
let constant = 8
variable = 30
```

변수와 상수 모두 타입을 가지지만 명시적으로 타입을 선언할 필요는 없다. 타입 추론에 의해 컴파일러가 처음 할당된 값과 같은 타입으로 타입을 정해주기 때문이다.

물론 타입을 명시적으로 지정할 수도 있다.

```swift
let explicitDouble: Double = 70
```



변수와 상수 모두 다른 타입으로 암묵적으로 형 변환이 일어나지 않는다. 즉, 다른 타입과 함께 연산(덧셈, 뺄셈 등) 할 수 없다. 대신 명시적으로 형 변환은 가능하다. 그래서 다른 타입과 연산하려면 명시적인 형 변환이 이루어져야 한다.

```swift
let label = "The width is "
let width = 100
let widthLabel = label + String(width) // The width is 100
```



String 타입 안에 값을 삽입하는 더 간단한 방법도 있다. 괄호안에 값을 입력하고 괄호 앞에 백슬래시를 사용하면 된다.

```swift
let apples = 3
let oranges = 5
let appleSummary = "I have \(apples) apples."
let fruitSummary = "I have \(apples + oranges) pieces of fruit."
```

`"""` 을 이용하면 개행을 포함한 String을 선언할 수 있다.

```swift
let quotation = """
I said "I have \(apples) apples."
And then I said "I have \(apples + oranges) pieces of fruit."
"""
```



`[]` 을 사용하여 배열과 딕셔너리를 생성할 수 있다. 

**배열**

```swift
var list = ["beer", "soju", "wine"]
list[1] = "cocktail"
list.append("vodka")

print(list)
```

**딕셔너리**

```swift
var occupations = [
  "Malcolm": "Captain",
  "Kaylee": "Mechanic"
]

occupations["Jayne"] = "Public Relations"
```



배열과 딕셔너리 타입을 명시적으로 주려면 다음과 같이 선언하면 된다.

```swift
let emptyArray: [String] = []
let emptyDictionary: [String: Float]: [:]
```



## 제어문

조건문에는 `if` 문과 `switch` 문이 있다. 그리고 반복문에는 `for-in` 과 `while`, `repeat-while` 문이 있다. 조건문과 제어문 변수 주변의 괄호는 선택사항이다. 대신 조건문과 제어문의 바디부분의 중괄호는 필수이다.



### for-in

```swift
let individualScores = [75, 43, 103, 87, 12]
var teamScore = 0

for score in individualScores {
  if score > 50 {
    teamScore += 3
  } else {
    teamScore += 1
  }
}

print(teamScore) // 11
```



`for-in`은 딕셔너리에서도 사용 가능하다. 딕셔너리에서 반복문 변수는 `(key, value)`로 된 pair를 제공한다. 참고로 딕셔너리는 **unordered collection**이므로 (실행)순서가 보장되지 않는다.

```swift
let interestingNumbers = [
    "Prime": [2, 3, 5, 7, 11, 13],
    "Fibonacci": [1, 1, 2, 3, 5, 8],
    "Square": [1, 4, 9, 16, 25],
]
var largest = 0
for (_, numbers) in interestingNumbers {
    for number in numbers {
        if number > largest {
            largest = number
        }
    }
}
print(largest)
```



`for-in` 을 배열 없이 사용하고 싶으면 `..<` 를 아용하여 반복 범위를 설정할 수 있다.

```swift
var total = 0
for i in 0..<4 { // [0, 4)
    total += i
}
print(total)
// Prints "6"
```





### if (if let)

if문에 대해서는 조건문 변수는 무조건 Boolean 타입이어야 한다. 암묵적 형 변환이 이루어지지 않기 때문(0 -> false)에 명시적으로 형 변환을 해야 한다. (`if score == 0 { ... }` 과 같이..)

`if`문 중에는 `if let`이 있는데, `if let` 은 조건문 변수가 nil일 가능성이 있을 때 nil인지 체크도 하는 조건문이다. nil 가능성이 있는 변수를 swift에서는 **Optional(옵셔널)** 이라고 한다. 옵셔널 변수를 선언할 때는 타입 뒤에 ?를 달면 된다. (`type?` )

```swift
var optionalString: String? = "Hello"
print(optionalString == nil) // false

var optionalName: String? = "John Applessed"
var greeting = "Hello!"

if let name = optionalName {
  greeting = "Hello, \(name)"
}
```



옵셔널 변수를 다루는 방법 중 `??`를 사용하는 방법도 있다. 이 연산자는 `??` 앞에 있는 변수가 nil일 때 `??` 뒤의 변수를 사용한다는 의미이다. (`optional ?? replacement`)

```swift
let nickname: String? = nil
let fullName: String = "John Appleseed"

let informalGreeting = "Hi \(nickname ?? fullName)"
```



### Switch

`switch` 문은 모든 종류의 데이터와 다양한 비교 연산을 제공한다. 

```swift
let vegetable = "red pepper"
switch vegetable {
case "celery": // Equal 연산
    print("Add some raisins and make ants on a log.")
case "cucumber", "watercress": // In 연산
    print("That would make a good tea sandwich.")
case let x where x.hasSuffix("pepper"): // 접두어 일치 연산 (Custom 연산이라고 할 수 있음.)
    print("Is it a spicy \(x)?")
default:
    print("Everything tastes good in soup.")
}
```

`case let x where` 연산자에서는 switch 문에서 받은 변수를 x에 할당하여 where 뒤의 결과가 true이면 case body를 연산한다고 생각하면 된다.

switch문 중 하나의 case에 일치하면 일치하는 것에 대응하는 명령어만 실행하고 switch문을 빠져나간다. 모든 case에 일치하지 않는다면 default에 선언된 명령어를 실행한다.



### while, repeat

`while` 문은 조건이 false가 될 때 까지 루프 조건을 확인한 후 반복문을 실행하는 것이고, `repeat`은 먼저 반복문을 한번 실행한 후 조건이 false가 될 때 까지 루프 조건을 확인한 후 반복문을 실행한다. 즉, repeat은 무조건 최소 한번은 실행되는 반복문이다.

```swift
var n = 2
while n < 100 {
    n *= 2
}
print(n)
// Prints "128"

var m = 2
repeat {
    m *= 2
} while m < 100
print(m)
// Prints "128"
```



## 함수와 클로저

`func` 키워드를 사용하여 함수를 정의할 수 있다. 함수는 다음 형식으로 정의할 수 있다.

```swift
func greet(person: String, day: String) -> String {
    return "Hello \(person), today is \(day)"
}

// 호출
greet(person: "Bob", day: "Tuesday")
```



가본적으로 함수는 파라미터 이름을 가지며, 호출할 때 파라미터 이름을 명시해야 하는데, 파라미터 인자에 레이블을 선언할 수도 있다. 레이블을 선언하면 파라미터 이름 대신 레이블로 명시할 수 있는데, 레이블이 `_` 이면 생략이 가능하다.

```swift
func greet(_ person: String, on day: String) -> String {
    return "Hello \(person), today is \(day)."
}

greet("John", on: "Wednesday")
```



Swift는 리턴값을 여러 개 리턴할 수 있다. 리턴값을 여러 개 선언하기 위해 튜플을 사용한다. 튜플의 요소는 숫자 또는 이름을 이용하여 참조할 수 있다.

```swift
func calculateStatistics(scores: [Int]) -> (min: Int, max: Int, sum: Int) {
    var min = scores[0]
    var max = scores[0]
    var sum = 0

    for score in scores {
        if score > max {
            max = score
        } else if score < min {
            min = score
        }
        sum += score
    }

    return (min, max, sum)
}
let statistics = calculateStatistics(scores: [5, 3, 100, 3, 9])
print(statistics.sum)
// Prints "120"
print(statistics.2)
// Prints "120"
```



함수를 함수 내에 선언할 수 있다. (Nested function) Nested function은 바깥 함수에 선언되어있는 변수에 접근이 가능하다. Nested function을 사용하여 길거나 복잡한 함수의 코드를 구성할 수 있다.

```swift
func returnFifteen() -> Int {
    var y = 10
    func add() {
        y += 5
    }
    add()
    return y
}
returnFifteen()
```



함수는 일급 객체이므로 함수의 리턴값으로 함수를 리턴할 수 있다.

```swift
func makeIncrementer() -> ((Int) -> Int) {
    func addOne(number: Int) -> Int {
        return 1 + number
    }
    return addOne
}
var increment = makeIncrementer()
increment(7)
```



그리고 함수의 인자로 함수를 받을 수도 있다.

```swift
func hasAnyMatches(list: [Int], condition: (Int) -> Bool) -> Bool {
    for item in list {
        if condition(item) {
            return true
        }
    }
    return false
}
func lessThanTen(number: Int) -> Bool {
    return number < 10
}
var numbers = [20, 19, 7, 12]
hasAnyMatches(list: numbers, condition: lessThanTen)
```



함수는 사실은 클로저의 특별한 경우이다. 클로저는 나중에 호출할 수 있는 코드 블록이라고 생각하면 된다. 클로저 안의 코드는 클로저가 실행될 때 다른 범위에 있더라도 클로저가 생성된 범위에서 사용 가능한 변수 및 함수 등에 접근할 수 있다. 함수 타입의 인자 대신 클로저를 사용하여 별도의 함수 선언 없이 사용할 수 있다.

```swift
numbers.map({ (number: Int) -> Int in
    let result = 3 * number
    return result
})
```



Swift에서는 클로저를 간단히 선안할 수 있도록 몇 가지 옵션을 제공한다. 위임을 위한 콜백과 같이 클로저의 타입을 이미 알고 있다면, 파라미터, 리턴 타입을 생략할 수 있다. 위에 있는 클로저는 아래 코드로 간결하게 작성할 수 있다.

```swift
let numbers = [20, 19, 7, 12]
let mappedNumbers = numbers.map({ number in 3 * number }) // numbers: [Int] 임을 알고있다.
print(mappedNumbers)
// Prints "[60, 57, 21, 36]"
```



그리고 클로저에서는 파라미터를 이름 대신 숫자로 참조할 수 있다. 그리고 함수에 대한 마지막 인수로 전달된 클로저는 괄호 바로 뒤에 나타날 수 있다. 이러한 방식은 클로저를 더 짧게 사용할 수 있는 접근방법이다.

```swift
let mappedNumbers = numbers.map { $0 * 3 }

let sortedNumbers = numbers.sorted { $0 > $1 }
print(sortedNumbers) // [20, 19, 12, 7]
```

