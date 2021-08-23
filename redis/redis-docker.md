# Docker로 Redis 사용하기 + SpringBoot에서 Redis 사용하기

Docker로 Redis를 설치하고 사용하는 방법을 알아보자.



## Redis

Redis를 간단하게 소개하자면, In-memory의 key-value 기반 데이터 저장소이다. In-memory이기 때문에 데이터를 읽고 쓰는 데 아주 빠르지만, 그만큼 휘발성이 강해 (Redis가 죽으면 데이터가 다 날아감) Redis는 임시로 저장하는 용도(Cache, Session)로 많이 사용한다.



## Docker로 설치

Docker를 사용하면 레디스를 간단하게 설치할 수 있다.

```shell
$ docker pull redis
```



## Redis 띄우기

Docker로 Redis를 실행하기 위해서 다음의 명령어를 사용하면 된다.

```shell
$ docker run --name some-redis -d -p 6379:6379 redis
```

* -p 6379:6379 : 외부에서 포트(6379)를 이용하여 접근할 수 있게 열어둔다.
* -d : 백그라운드에서 실행 (터미널을 계속 붙잡고 있을 수는 없으니까..)



## Redis 사용해보기 (lettuce)

Redis를 띄웠으면 본격적으로 SpringBoot에서 Redis를 사용하는 방법을 알아보자. Redis를 사용하기 위한 라이브러리 중 대표적으로 lettuce를 사용해보겠다.



### 설정

먼저 의존성 설정을 하자

```groovy
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
```



그 다음 application.yml 에서 프로퍼티를 설정하자.

```yml
spring:
  redis:
    port: 6379
    host: 127.0.0.1
    password: yourpassword
    lettuce:
      pool:
        max-active: 10
        max-idle: 10
        min-idle: 2
```



### 클래스 작성

그 다음 @Configuration으로 RedisTemplate Bean을 생성하자.

```kotlin
@Configuration
class RedisConfig {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory()
    }

    @Bean
    fun stringRedisTemplate(): StringRedisTemplate {
        return StringRedisTemplate().apply {
            connectionFactory = redisConnectionFactory()
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
        }
    }
}
```



### 사용 방법

필자는 토이 프로젝트를 하면서 JWT를 저장하는 용도로 Redis를 사용하였다. 토이 프로젝트의 일부 코드를 예제 코드로 공개하겠다. (레디스 사용 코드만 이해하자.) 



#### 레디스로 데이터 저장

일단 아래의 코드는 JWT를 만들어서 레디스에 저장하는 예제코드이다. 아주 간단하다!

**AuthenticationService.kt**

```kotlin
@Component
class AuthenticationService(
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val stringRedisTemplate: StringRedisTemplate
) {
    fun getToken(command: TokenCreateCommand): String {
        val member = memberRepository.findByEmail(command.email)
            ?.takeIf { passwordEncoder.matches(command.password, it.password) }
            ?: throw WrongEmailOrPasswordException()

      	// (1)
        return jwtTokenProvider.generateToken(member).also { token ->
            // (2)                                                
            stringRedisTemplate.opsForValue()
                .set(RedisKeyEnum.TOKEN.getKey(command.email), token)
        }
    }
}
```

**RedisKeyEnum.kt**

```kotlin
enum class RedisKeyEnum(val key: String) {
    TOKEN("recipt:token");

    fun getKey(vararg data: String) = "$key:${data.joinToString(":")}"
}
```

1. 먼저 AuthenticationService에서는 회원 정보를 바탕으로 JWT 토큰을 발급한다. 
2. 토큰 값을 Redis에 저장한다.











