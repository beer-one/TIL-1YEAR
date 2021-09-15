# Docker

도커 공식문서 뜯어보기 (https://docs.docker.com/get-started/)



## 배우는 목적

* 도커로 이미지를 컨테이너로 빌드하고 실행시키는 방법
* Docker Hub를 사용하여 도커 이미지 공유
* DB 등과 함께 여러개의 컨테이너를 사용하는 도커 애플리케이션 배포
* Docker compose를 사용하여 애플리케이션 실행
* 이미지에서 보안 취약점은 찾는 방법 등 이미지 작성에 대한 모범 사례



## 설치

[설치사이트](https://docs.docker.com/get-started/#download-and-install-docker) 에 가서 자신이 사용하고 있는 운영체제의 도커를 설치하면 된다. 도커의 버전을 알고 싶으면 다음 명령어로 확인 가능하다.

```shell
$ docker --version 
Docker version 20.10.6, build 370c289
```



## 도커 이미지 실행시키기

일단 도커허브에 공개되어있는 튜토리얼 이미지를 받아와서 도커 이미지를 실행시켜보자. 일단 아래 명령어를 입력해보자.

```shell
docker run -d -p 8080:80 docker/getting-started
```

* `-d`: 컨테이너를 detached mode(background)로 실행시킨다.
* `-p 8080:80`: host의 8080포트와 컨테이너의 80포트를 매핑한다. (host의 8080포트로 접근하면 컨테이너의 80포트로 접근하는 것과 같다.)
* `docker/getting-started`: 컨테이너로 실행시킬 도커 이미지



명령어를 입력하면 아래와 같이 출력이 되는데 먼저 로컬에 `docker/getting-started` 라는 도커 이미지가 있는지 확인 후 없다면 도커허브에서 도커 이미지를 찾아서 pull을 한다. 도커 이미지를 받은 후 이미지를 컨테이너로 실행시킨다.

```
Unable to find image 'docker/getting-started:latest' locally
latest: Pulling from docker/getting-started
Digest: sha256:10555bb0c50e13fc4dd965ddb5f00e948ffa53c13ff15dcdc85b7ab65e1f240b
Status: Downloaded newer image for docker/getting-started:latest
096928e2bcb6e214cc5ca2f9ebcedced714f023366247605b0e599de1e4ea603
```



그리고 웹 브라우저에서 8080포트로 접속하면 아래와 같은 웹 페이지가 나온다.

![image-20210915235527228](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210915235527228.png)