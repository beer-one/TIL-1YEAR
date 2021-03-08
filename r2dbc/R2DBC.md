# R2DBC

R2DBC는 반응형 드라이버를 사용하여 관계형 데이터베이스를 통합하는 인큐베이터인 Relative Relational DataBase Connectivity의 약자이다. 



## 요점

* R2DBC는 Reactive Stream 스펙에 초점을 맞춰 개발하였다. R2DBC는 완전한 reactive non-blocking API이다.
* JDBC와 같은 blocking API와는 다르게, R2DBC는 Reactive API를 사용하여 SQL Database에 접근할 수 있도록 해준다.
  * 보니까 Reactor, RxJava, Smallrye Mutiny(?) API로 구현한 듯..
* R2DBC는 전통적인 `One thread per connection` 모델에서 더 강력하고 확장성 있는 접근 방식으로 전환할 수 있다.
* R2DBC는 개방형 스펙으로 드라이버 벤더가 구현하고 클라이언트가 사용할 수 있는 서비스 공급자 인터페이스(SPI)를 구축한다.







## Spring Data R2DBC

Spring Framework에서 WebFlux가 도입하고 나서 R2DBC도 지원해주는 듯 하다. Spring Data R2DBC는 개념적으로 쉽게 풀어내기 위해서 Caching, Lazy loading 과 같은 ORM Framework에서 지원하고 있는 개념들을 제공하지 않는다. 



### Dependency

```groovy
dependencies {
   implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
   runtimeOnly("dev.miku:r2dbc-mysql:0.8.2.RELEASE")
   runtimeOnly("mysql:mysql-connector-java")
}
```





### Repository

기본적인건 일단 뒤로 미루고.. Spring R2DBC도 마찬가지로 JPA와 같이 interface 형식으로 repository를 지원한다. 예제에는 ReactiveCrudRepository로 되어있는데 R2dbcRepository가 ReactiveCrudRepository를 상속하는 인터페이스니까 이래 해도 될 듯..

```kotlin
interface MemberRepository: R2dbcRepository<Member, Long> {
   ...
}
```



R2DBC Repository를 설정하기 위해서는 @EnableR2dbcRepisotories 애노테이션을 붙여야 한다.

```kotlin
@Configuration
class R2dbcConfig(private val properties: R2dbcProperties) : AbstractR2dbcConfiguration() {
    override fun connectionFactory(): ConnectionFactory {
        
        return MySqlConnectionFactory.from(
            MySqlConnectionConfiguration.builder()
                .host("192.168.0.4")
                .port(3306)
                .database("reciptMember")
                .username(properties.username)
                .password(properties.password)
                .build()
        )
    }
}
```



### Query Methods

JpaRepository 처럼 `findBy~~()` 메서드를 인터페이스 내에 선언만 하게 되면 자동으로 기능을 제공해주듯 ReactiveCrudRepository도 마찬가지로 제공해준다.

아래는 Spring Framework에서 제공되는 예시인데 한번 살펴보자.

```kotlin
interface ReactivePersonRepository: ReactiveSortingRepository<Person, Long> {

  fun findByFirstname(firstname: String): Flux<Person>  // (1)                                  

  fun findByFirstname(firstname: Publisher<String>): Flux<Person>  // (2)                     

  fun findByFirstnameOrderByLastname(firstname: String, pageable: Pageable): Flux<Person>  // (3)  

  fun findByFirstnameAndLastname(firstname: String, lastname: String): Mono<Person>  // (4)

  fun findFirstByLastname(lastname: String): Mono<Person>  // (5)                            

  @Query("SELECT * FROM person WHERE lastname = :lastname")
  fun findByLastname(lastname: String): Flux<Person>  // (6)                        

  @Query("SELECT firstname, lastname FROM person WHERE lastname = $1")
  fun findFirstByLastname(lastname: String): Mono<Person>  // (7)                            
}
```

1. firstname이 파라미터의 firstname와 같은 Person을 모두 가져온다.
2. 1과 같은 맥락이지만 파라미터가 Publisher이다.
3. 1과 같은 맥락이지만 Pageable 인터페이스를 파라미터로 던져서 페이징 처리 결과를 받을 수 있다.
4. firstname이 파라미터의 firstname와 같고 lastname이 파라미터의 lastname과 같은 Person을 하나 가져온다. 리턴 타입이 Mono이기 때문에 두 개 이상의 엔티티가 조회된다면 `IncorrectResultSizeDataAccessException`이 발생한다.
5. first 키워드를 이용하여 `IncorrectResultSizeDataAccessException`이 발생하지 않고 해당 조건에 맞는 첫 번째 데이터만 가져온다. 
6. query를 명시하여 query문과 일치하는 모든 Person을 받는다. 파라미터는 쿼리의 :{parameterName}에 매핑된다.
7. 마찬가지로 조건에 맞는 모든 결과 중 하나의 Person만 받는다. 6과는 다르게 \$1 , \$2 ... 키워드로 파라미터를 매핑할 수 있다.



#### 키워드 정리

[출처: SpringFramework](https://docs.spring.io/spring-data/r2dbc/docs/1.2.5/reference/html/#r2dbc.repositories.queries)

| Keyword                              | Sample                                      | Logical result                       |
| :----------------------------------- | :------------------------------------------ | :----------------------------------- |
| `After`                              | `findByBirthdateAfter(Date date)`           | `birthdate > date`                   |
| `GreaterThan`                        | `findByAgeGreaterThan(int age)`             | `age > age`                          |
| `GreaterThanEqual`                   | `findByAgeGreaterThanEqual(int age)`        | `age >= age`                         |
| `Before`                             | `findByBirthdateBefore(Date date)`          | `birthdate < date`                   |
| `LessThan`                           | `findByAgeLessThan(int age)`                | `age < age`                          |
| `LessThanEqual`                      | `findByAgeLessThanEqual(int age)`           | `age <= age`                         |
| `Between`                            | `findByAgeBetween(int from, int to)`        | `age BETWEEN from AND to`            |
| `NotBetween`                         | `findByAgeNotBetween(int from, int to)`     | `age NOT BETWEEN from AND to`        |
| `In`                                 | `findByAgeIn(Collection ages)`              | `age IN (age1, age2, ageN)`          |
| `NotIn`                              | `findByAgeNotIn(Collection ages)`           | `age NOT IN (age1, age2, ageN)`      |
| `IsNotNull`, `NotNull`               | `findByFirstnameNotNull()`                  | `firstname IS NOT NULL`              |
| `IsNull`, `Null`                     | `findByFirstnameNull()`                     | `firstname IS NULL`                  |
| `Like`, `StartingWith`, `EndingWith` | `findByFirstnameLike(String name)`          | `firstname LIKE name`                |
| `NotLike`, `IsNotLike`               | `findByFirstnameNotLike(String name)`       | `firstname NOT LIKE name`            |
| `Containing` on String               | `findByFirstnameContaining(String name)`    | `firstname LIKE '%' + name +'%'`     |
| `NotContaining` on String            | `findByFirstnameNotContaining(String name)` | `firstname NOT LIKE '%' + name +'%'` |
| `(No keyword)`                       | `findByFirstname(String name)`              | `firstname = name`                   |
| `Not`                                | `findByFirstnameNot(String name)`           | `firstname != name`                  |
| `IsTrue`, `True`                     | `findByActiveIsTrue()`                      | `active IS TRUE`                     |
| `IsFalse`, `False`                   | `findByActiveIsFalse()`                     | `active IS FALSE`                    |



### R2dbcEntityOperations

R2dbcEntityTemplate는 Spring Data R2DBC의 중심 엔트리포인트이다. R2dbcEntityTemplate는 entity 지향의 메서드들을 지원한다. 



#### insert

```kotlin
val person = Person("John", "Doe")

val saved = template.insert(person)
```

* POJO를 파라미터로 담아 insert() 연산을 실행할 수 있는데, 보통 이 때는 테이블 이름은 클래스 이름에 매핑된다. 



#### select

```kotlin
val loaded: Mono<Person> = template.select(query(where("firstname").is("John")))
val loaded: Flux<Person> = template.select(query(where("firstname").is("John")))
```



#### select (Fluent API)

```kotlin
val first: Mono<Person> = template.select(Person::class)  
  .from("other_person")
  .matching(query(where("firstname").is("John")     
    .and("lastname").in("Doe", "White"))
    .sort(by(desc("id"))))                          
  .one()

val people: Flux<Person> = template.select(Person::class) 
    .all()
```



#### insert (Fluent API)

```kotlin
val insert: Mono<Person> = template.insert(Person::class) 
    .using(new Person("John", "Doe"))
```



#### update (Fluent API)

```kotlin
val update: Mono<Integer> = template.update(Person::class)  
        .inTable("other_table")                           
        .matching(query(where("firstname").is("John")))   
        .apply(update("age", 42))
```



#### delete (Fluent API)

```kotlin
val delete: Mono<Integer> = template.delete(Person::class)  
        .from("other_table")                              
        .matching(query(where("firstname").is("John")))   
        .all()                                       
```

