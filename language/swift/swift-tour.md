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



## 객체와 클래스

`class` 키워드를 사용하여 클래스를 생성할 수 있다. 클래스에서 프로퍼티 정의는 클러스 컨텍스트에 있다는 점을 제외하고는 상수와 변수의 선언과 같은 방식으로 작성된다. 메서드와 함수도 같은 방식으로 작성된다.

```swift
class Shape {
  var numberOfSides = 0
  func simpleDescription() -> String {
    return "A shape with \(numberOfSides) sides."
  }
}
```



클래스 이름 뒤에 괄호를 넣어서 클래스의 인스턴스를 생성할 수 있다. `.` 을 이용하여 인스턴스의 프로퍼티와 메서드에 접근할 수 있다.

```swift
var shape = Shape()
shape.numberOfSides = 7
var shapeDescription = shape.simpleDescription()
```



클래스에 `init` 키워드를 통해 생성자를 선언할 수도 있다. 생성자 내에 파라미터 선언도 가능하다.

```swift
class NamedShape {
  var numberOfSides: Int = 0
  var name: String
  
  init(name: String) {
    self.name = name
  }
  
  func simpleDescription() -> String {
    return "A shape with \(numberOfSides) sides."
  }
}
```

* `self` 키워드를 통해 같은 이름을 가진 객체 프로퍼티와 생성자 파라미터를 구분할 수 있다.
* `deinit` 키워드를 통해 소멸자를 선언할 수 있다. 소멸자는 객체가 할당 해제되기 전에 정리할 자원이 있다면 자원을 정리하기 위해 사용한다.



서브클래스는 해당 클래스 이름 뒤에 `:` 을 붙인 후 슈퍼 클래스 이름이 포함된다. 슈퍼클래스의 구현을 오버라이드하는 서브클래스의 메서드들은 `override` 로 표시한다. `override` 가 없다면 컴파일러는 이를 에러로 간주한다. 컴파일러는 또한 슈퍼클래스의 어떤 메서드도 오버라이드하지않는 메서드를 탐지한다.

```swift
class Square: NamedShape {
  var sideLength: Double
  
  init(sideLength: Double, name: String) {
    self.sideLength = sideLength
    super.init(name: name)
    numberOfSides = 4
  }
  
  func area() -> Double {
    return sideLength * sideLength
  }
  
  override func simpleDescription() -> String {
    return "A Square with sides of length \(sideLength)"
  }
}

let test = Square(sideLength: 5.2, name: "TestSquare")
test.area()
test.simpleDescription()
```







프로퍼티에는 저장되는 단순 프로퍼티 외에도 getter와 setter가 있을 수 있다.

```swift
class EquilateralTriangle: NamedShape {
  var sideLength: Double = 0.0
  
  init(sideLength: Double, name: String) {
    self.sideLength = sideLength
    super.init(name: name)
    numberOfSides = 3
  }
  
  // 모서리 길이 총 합
  var perimeter: Double {
    get {
      return 3.0 * sideLength
    }
    set {
      sideLength = newValue / 3.0
    }
    
    // set (value: Double) { sideLength = value / 3.0 }
  }
  }
  
  override func simpleDescription() -> String {
    return "An equailateral triangle with sides of length \(sideLength)."
  }
}

var triangle = EquailateralTriangle(sideLength: 3.1, name: "triangle")
print(triangle.perimeter) // 9.3
triangle.perimeter = 9.9
print(triangle.sideLength) // 3.3000000000000003
```



`perimeter` 의 setter에서는 새로운 값을 의미하는 `newValue`를 사용한다. `set` 괄호 내에 명시적으로 이름을 제공할 수도 있다.

`EquilateralTriangle` 클래스에서 생성자는 3단계를 거친다.

1. 서브클래스 선언의 프로퍼티 값을 설정한다.
2. superclass의 생성자를 호출한다.
3. superclass에 선언되어있는 프로퍼티 값을 변경한다. 메서드 getter, setter를 사용하는 추가 설정 작업도 이 시점에서 수행할 수 있다.



만약 프로퍼티를 계산할 필요는 없지만 새 값을 설정하기 전과 후에 실행되는 코드를 제공해야 하는 경우 `willSet` 과 `didSet` 을 사용할 수 있다. `willSet` 과 `didSet` 으로 이루어진 코드는 생성자 밖에서 값이 변경될 때 마다 실행된다. 

```swift
class TriangleAndSquare {
  var triangle: EquailateralTriangle {
    willSet {
      square.sideLength = newValue.sideLength
    }
  }
  
  var square: Square {
    willSet {
      triangle.sideLength = newValue.sideLength
    }
  }
  
  init(size: Double, name: String) {
    square = Square(sideLength: size, name: name)
    triangle = EquailateralTriangle(sideLength: size, name: name)
  }
}

var triangleAndSquare = TriangleAndSquare(size: 10, name: "test")

print(triangleAndSquare.square.sideLength) // 10.0
print(triangleAndSquare.triangle.sideLength) // 10.0

triangleAndSquare.square = Square(sideLength: 50, name: "test2")
print(triangleAndSquare.triangle.sideLength) // 50.0
```



옵셔널 값을 사용한다면 메서드, 프로퍼트, 서브스크립팅과 같은 연산자를 사용하기 전에 `?` 키워드를 사용할 것이다. `?` 이전의 값이 `nil` 일 경우, `?` 이후의 값이 무시되고 전체 연산의 결과가 `nil` 이 될 것이다. 반대로, 옵셔널 값이 래핑되어있지 않고, `?` 이후로 오는 모든 값들이 래핑되어있지 않은 값에 작용한다. 이 두 가지 경우 모두 다, 전체 값의 결과는 옵셔널 값이 된다.

```swift
let optionalSquare: Square? = Square(sideLength: 2.5, name: "optional")
let sideLength = optionalSquare?.sideLength
```



## 열거형과 구조체

`enum` 키워드를 사용하여 열거형을 생성할 수 있다. 클래스와 같이 열거형도 메서드를 선언할 수 있다.

```swift
enum Rank: Int {
  case ace = 1
  case two, three, four, five, six, seven, eight, nine, ten
  case jack queen, king
  
  func simpleDescription() -> String {
    swich self {
      case .ace:
        return "ace"
      case .jack:
        return "jack"
      case .queen:
        return "queen"
      case .king
        return "king"
      default:
        return String(self.rawValue)
    }
  }
}

let ace = Rank.ace
let aceRawValue = ace.rawValue
```

기본적으로 스위프트는 raw value를 0부터 시작해서 1씩 증가하여 변수에 할당한다. 위의 예시에서는 처음 변수에 1을 할당했기 때문에 나머지 변수들은 2부터 1씩 증가하여 변수에 값을 할당하게 된다. 



`init?(rawValue:)` 생성자를 사용하여 열거형 인스턴스를 생성할 수 있다. 이 값은 선언된 열거형 변수의 rawValue에 일치하는 경우가 아니라면 `nil` 으로 할당된다.



열거형의 case 에서 사용되는 값은 원시 값을 쓰는 다른 방법이 아닌 실제 값이다. 사실, raw value를 사용할 필요가 없을 경우에는 raw value를 제공하지 않아도 된다.

```swift
enum Suit {
  case spades, hearts, diamonds, clubs
  
  func simpleDescription() -> String {
    switch self {
      case .spades:
        return "spades"
      case .hearts:
        return "hearts"
      case .diamonds:
        return "diamonds"
      case .clubs:
        return "clubs"
    }
  }
}

let hearts = Suit.hearts
let heartsDescription = hearts.simpleDescription()
```



열거형은 열거형과 연관된 값을 가질 수 있다. 이 연관된 값은 열거형 인스턴스를 생성할 때 결정할 수 있고 이 값은 열거형 인스턴스마다 다른 값으로 선언할 수 있다. 이 연관된 값을 열거형 인스턴스의 저장 프로퍼티로 생각하면 된다. 



예를 들어, 서버로부터 일출, 일몰시간을 요청하는 경우를 생각해보자. 서버가 요청된 정보로 응답하거나 무엇이 잘못되었는지에 대한 설명을 응답한다.

```swift
enum ServerResponse {
  case result(String, String)
  case failure(String)
}

let success = ServerResponse.result("6:00 am", "8:09 pm")
let failure = ServerResponse.failure("Out of cheese.")

switch success {
  case let .result(sunrise, sunset):
    print("Sunrise is at \(sunrise) and sunset is at \(sunset)")
  case let .failure(message):
    print("Failure.... \(message)")
}
```





`struct` 키워드를 통해 구조체를 생성한다. 구조체는 메서드 및 생성자를 포함하여 클래스와 같은 많은 동작을 지원한다. 구조체와 클래스의 가장 큰 차이점 중 하나는 구조체는 코드에서 전달될 때 항상 값이 복사되지만 클래스는 참조에 의해 전달된다는 점이다.

```swift
struct Card {
    var rank: Rank
    var suit: Suit
    func simpleDescription() -> String {
        return "The \(rank.simpleDescription()) of \(suit.simpleDescription())"
    }
}
let threeOfSpades = Card(rank: .three, suit: .spades)
let threeOfSpadesDescription = threeOfSpades.simpleDescription()
```



## Protocol 과 Extensions

`protocol` 키워드를 통해 프로토콜을 선언할 수 있다. 프로토콜은 자바/코틀린의 `interface` 와 비슷한 것 같다.

```swift
protocol ExampleProtocol {
  var simpleDescription: String { get }
  mutating func adjust()
}
```



클래스, 열거형, 구조체는 프로토콜을 채택(adopt)할 수 있다. `implementation?`

```swift
class SimpleClass: ExampleProtocol {
  var simpleDescription: String = "A very simple class"
  var anotherProperty: Int = 12345
  
  func adjust() {
    simpleDescription += " Now 100% adjusted."
  }
}

var a = SimpleClass()
a.adjust()
let aDescription = a.simpleDescription

struct SimpleStructure: ExampleProtocol {
  var simpleDescription: String = "A simple structure"
  
  mutating func adjust() {
    simpleDescription += " (adjusted)"
  }
}

var b = SimpleStructure()
b.adjust()
let bDescription = b.simpleDescription
```

`SimpleClass` 에서는 구조체를 수정하는 메서드를 표시하기 위해 `mutating` 키워드를 사용한다. 클래스의 메서드는 항상 클래스를 수정할 수 있으므로  `SimpleClass` 의 선언에서는 `mutating` 으로 표시된 메서드가 필요없다.



그리고 변수를 프로토콜 타입으로 생성하여 프로토콜을 채택한 인스턴스를 할당할 수 있다. 변수가 만약 프로토콜 타입일 경우, 프로토콜에서 선언된 프로퍼티와 메서드를 제외한 나머지는 호출이 불가능하다. 

```swift
let protocolValue: ExampleProtocol = a
print(protocolValue.simpleDescription)
```

런타임에서는 `protocolValue` 의 값이 `SimpleClass` 타입이지만 컴파일러는 이 변수를 `ExampleProtocol`로 취급한다.









`extension` 키워드를 사용하여 기존 타입에 함수를 추가할 수 있다. 익스텐션을 사용하여 다른 곳에서 선언된 타입이나 라이브러리 또는 프레임워크에서 가져온 타입에 기능을 추가할 수 있다.

```swift
extension Int: ExampleProtocol {
    var simpleDescription: String {
        return "The number \(self)"
    }
    mutating func adjust() {
        self += 42
    }
}
print(7.simpleDescription)
// Prints "The number 7"
```



## Error Handling

에러를 `Error` 프로토콜을 채택한 타입으로 선언할 수 있다.

```swift
enum PrinterError: Error {
    case outOfPaper
    case noToner
    case onFire
}
```



`throw` 키워드를 통해 에러를 던질 수 있고, `throws` 키워드를 통해 에러를 던질 수 있는 메서드를 명시할 수 있다. 함수에 에러를 던지면 함수가 즉시 반환되고 함수를 호출한 코드에서 오류를 처리한다.

```swift
func send(job: Int, toPrinter printerName: String) throws -> String {
  if printerName == "Never has toner" {
    throw PrinterError.noToner
  }
  return "Job sent"
}
```



에러를 처리하는 여러 방식이 있다. 그 방식 중 하나는 `do-catch` 를 사용하는 것이다. `do` 블럭 내부에서는 앞에 `try` 를 붙여 에러를 유발할 수 있는 코드를 표시한다. `catch` 블럭 내부에서는 다른 이름을 지정하지 않으면 오류에 자동으로 `error` 라는 이름의 오류가 지정된다.



```swift
do {
  let printerResponse = try send(job: 1040, toPrinter: "Seowon")
  print(printResponse)
} catch {
  print(error)
}
```



`catch` 블럭을 여러개 둬서 에러별로 에러 핸들링 코드를 넣을 수도 있다.

```swift
do {
    let printerResponse = try send(job: 1440, toPrinter: "Gutenberg")
    print(printerResponse)
} catch PrinterError.onFire {
    print("I'll just put this over here, with the rest of the fire.")
} catch let printerError as PrinterError {
    print("Printer error: \(printerError).")
} catch {
    print(error)
}
// Prints "Job sent"

```



에러를 처리하는 다른 한가지 방식은 `try?` 키워드를 사용하는 것이다. 이 방식은 `try?` 뒤의 코드에서 에러가 발생한다면 `nil` 을 반환하고 에러가 발생하지 않는다면 원래 값으로 반환한다.

```swift
let printerSuccess = try? send(job: 1884, toPrinter: "Mergenthaler")
let printerFailure = try? send(job: 1885, toPrinter: "Never Has Toner")
```



`defer` 키워드를 사용하여 함수가 반환되기 직전에 (나중에) 실행되는 코드블록을 작성할 수 있다. 이 코드는 함수가 에러를 발생함에 관계 없이 마지막에 실행된다. 그래서 `defer` 키워드를 사용하여 설정 및 정리 코드를 작성할 수 있다.

```swift
var fridgeIsOpen = false
let fridgeContent = ["milk", "eggs", "leftovers"]

func fridgeContains(_ food: String) -> Bool {
    fridgeIsOpen = true
    defer { // 함수가 끝나는 시점에 처리됨 
        fridgeIsOpen = false
    }

    let result = fridgeContent.contains(food)
    return result
}
fridgeContains("banana")
print(fridgeIsOpen)
// Prints "false"
```



## 제네릭

`<>` 안에 이름을 작성하여 제네릭 함수나 제네릭 타입을 선언할 수 있다.

```swift
func makeArray<Item>(repeating item: Item, numberOfTimes: Int) -> [Item] {
    var result: [Item] = []
    for _ in 0..<numberOfTimes {
        result.append(item)
    }
    return result
}
makeArray(repeating: "knock", numberOfTimes: 4) // [String]
```



함수 및 메서드 뿐 아니라 클래스, 열거형, 구조체 또한 제네릭 타입을 만들 수 있다.

```swift
// Reimplement the Swift standard library's optional type
enum OptionalValue<Wrapped> {
    case none
    case some(Wrapped)
}
var possibleInteger: OptionalValue<Int> = .none
possibleInteger = .some(100)
```



`where` 키워드를 사용하여 제네릭 타입의 필요조건을 넣을 수 있다. 예를 들어, 타입이 어떤 프로토콜을 구현해야 하는지, 두 타입이 동일한 타입이어야 하는지, 클래스에 특정 수퍼 클래스가 있어야 하는지에 대한 필요조건을 넣을 수 있다.

```swift
func anyCommonElements<T: Sequence, U: Sequence>(_ lhs: T, _ rhs: U) -> Bool
    where T.Element: Equatable, T.Element == U.Element
{
    for lhsItem in lhs {
        for rhsItem in rhs {
            if lhsItem == rhsItem {
                return true
            }
        }
    }
    return false
}
anyCommonElements([1, 2, 3], [3])
```













