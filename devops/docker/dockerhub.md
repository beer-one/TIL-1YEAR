# DockerHub

도커 허브를 통해 도커 애플리케이션을 공유하는 방법에 대해 알아보자.



## 레포지토리 생성

도커 이미지를 공유하기 위해서는 도커허브에서 레포지토리를 생성해야 한다. (깃허브와 비슷) 먼저 도커허브 계정이 없다면 계정을 만들자.([Sign up](https://www.docker.com/pricing?utm_source=docker&utm_medium=webreferral&utm_campaign=docs_driven_upgrade))

도커허브 계정을 만들었다면 [Docker Hub 로그인](https://hub.docker.com/) 을 하고 Create Repository 버튼을 눌러 레포지토리를 생성하자. (한 이미지당 한 레포지토리 일듯.) 

![image-20210917181630277](/Users/nhn/Library/Application Support/typora-user-images/image-20210917181630277.png)

## 이미지 push

이전 실습에서 만들었던 도커 이미지를 레포지토리에 push하여 공유해보자. 도커 이미지를 push하는 방법은 다음과 같다.

1. 도커허브에 로그인

```shell
$ docker login -u {{username}}
```

2. `docker tag` 를 이용하여 도커 이미지에다가 이름 태깅

```shell
$ docker tag getting-started {{username}}/getting-started
```

3. push

```shell
$ docker push {{username}}/getting-started
```



## Push된 이미지 받아와서 이미지 실행

```shell
$ docker run -dp 3000:3000 YOUR-USER-NAME/getting-started
```

