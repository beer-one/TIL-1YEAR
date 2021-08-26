# SpringBoot Gradeful Shutdown

Spring Boot 2.3부터 Boot에서 4가지 임베디드 웹 서버(Tomcat, Jetty, Undertow, Netty) 모두 graceful shutdown을 지원한다. Graceful shutdown을 적용시키기 위해서는 application.properties 파일에다가 `server.shutdown = graceful` 을 추가해주면 된다.

그러면, Tomcat, Netty, Jetty는 network layer에서 새로운 요청을 더이상 받지 않는다. 하지만 Undertow에서는 새로운 요청을 계속해서 받지만, 즉시 503에러를 클라이언트에 던져준다.

`server.shutdown` 프로퍼티의 기본값은 `immediate` 이며, 이 값으로 설정되면 서버가 즉시 셧다운된다.

graceful shutdown 단계가 시작되기 전에 이미 받아진 요청들이 있는데, 이런 요청의 경우, 서버는 요청에 대한 작업을 완료할 때 까지 특정 시간동안 기다린다. 이 특정 시간 또한 마찬가지로 설정할 수 있는데 아래와 같이 설정하면 된다.

````
spring.lifecycle.timeout-per-shutdown-phase=1m
````

위와 같이 설정하면 1분 동안 요청이 완료되었는지 기다린다. 기본값은 30초이다.

