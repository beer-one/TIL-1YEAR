# DataMongoTest

@DataMongoTest는 Spring framework에서 제공하는 MongoDB 테스트용 애노테이션이다.

이 애노테이션은 Mongo DB 구성 요소에만 초점을 맞춘 MongoDB 테스트를 할 때 사용한다. 이 애노테이션을 사용하면 전체적인 auto configuration을 비활성화 하는 대신 Mongo DB 테스트와 관련된 configuration만 적용된다. 기본적으로 이 애노테이션을 사용하면 테스트 할 때 embedded in-memory mongoDB를 사용한다.



## 의존성 설정

먼저 Spring에서 제공하는 mongodb와 flapdoodle에서 제공하는 embedded Mongo DB를 gradle 의존성에 추가한다.

```gradle
dependencies {
    // mongodb
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
}
```



## 테스트 코드 작성

```kotlin
@DataMongoTest
class MongoDbSpringIntegrationTest(
    private val mongoTemplate: MongoTemplate 
) {
    
    @Test
    fun test() {
        // given
        val objectToSave = BasicDBObjectBuilder.start()
            .add("key", "value")
            .get();

        // when
        mongoTemplate.save(objectToSave, "collection");

        // then
        assertThat(mongoTemplate.findAll(DBObject::class, "collection")).extracting("key")
            .containsOnly("value");
    }
}
```

간단히 @DataMongoTest만 추가하면 된다.



여기서 주의할 점은 @DataMongoTest는 일종의 Integration Test이기 때문에 @SpringBootTest와 같은 Integration Test와는 같이 사용할 수가 없다.



## 발견한 오류

회사에서 테스트용으로 작성했는데 막상 dev환경으로 배포를 했을 때 회사 젠킨스에서 에러가 발생했다.

![스크린샷 2021-01-05 오후 6.00.24](/Users/nhn/Desktop/스크린샷 2021-01-05 오후 6.00.24.png)

에러로그를 간략히 보면 embedded DB를 사용할 때 embedded DB가 로컬에 깔려있지 않으면 다운로드를 받은 후 테스트가 진행되는 것 처럼 보이는데 다운로드에서 SSL 에러가 났다. 이럴 땐 어떻게 해야하지,,?