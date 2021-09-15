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



> -d 같이 single character flag는 다른 flag와 같이 붙여서 사용 가능하다.
>
> ```shell
> docker run -dp 8080:80 docker/getting-started
> ```



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



## Docker Dashboard

Docker Dashboard는 Mac과 Windows에서 지원한다. Docker Dashboard는 컨테이너의 로그를 빨리 접근할 수 있고, 컨테이너 내부의 shell을 얻을 수 있게 해주고, 쉽게 컨테이너의 라이프사이클을 관리할 수 있게 해준다. 

위에서 `docker/getting-started` 이미지를 실행시킨 후 대시보드를 확인하면 다음과 같이 컨테이너가 목록에 생기는 것을 알 수 있다.

![image-20210916001310465](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210916001310465.png)

현재 Running 중인 컨테이너는 초록색으로, 포트 또한 명시되어 있다. 그리고 이미 종료된 컨테이너는 흑색으로 보여준다. 그리고 `peaceful_hypatia` 컨테이너는 `docker run` 으로 실행된 컨테이너고, mysql는 docker compose로 실행된 컨테이너라 하위 컨테이너들을 볼 수 있다.







## Container, Image

Container는 Host 시스템의 다른 모든 프로세스로부터 격리된 시스템의 또 다른 프로세스이다. 여기서 격리는 오랫동안 리눅스에 있었던 커널 네임스페이스와 cgroup을 활용한다. Docker는 이러한 기능에 접근하고 사용하기 쉽게 만들기 위해 제작되었다.

컨테이너를 실행할 때는 격리된 fileSystem을 사용한다. 이렇게 지정된 fileSystem은 `Container image` 로써 제공된다. 이미지가 컨테이너의 파일시스템을 포함하기 때문에, 이미지는 애플리케이션을 실행시킬 때 필요한 모든 것들(모든 의존성, 설정, 스크립트, 바이너리파일 등등..)을 포함해야 한다. 이미지는 환경변수나 기본 실행 커맨드나 여러 메타데이터 등 컨테이너가 필요한 다른 설정들 또한 포함한다.





## Sample Application으로 도커 체험하기

Docker 공식 홈페이지에 있는 샘플 애플리케이션으로 도커를 체험해보자. 이 샘플 애플리케이션은 Node.js로 만든 웹 서버이지만, 도커를 공부할 때 사용하기 때문에 Javascript의 별 다른 지식 없이도 할 수 있을 것이다. 

먼저 [github](https://github.com/docker/getting-started/tree/master/app) 에 접속하여 getting-started/app 파일을 다운로드 받자. (또는 git clone을 해도 관계 없다.) 



### 컨테이너 이미지 Build

이미지를 빌드하기 위해서는 `Dockerfile` 이라는 파일을 만들어야 한다. Dockerfile은 컨테이너 이미지를 만들기 위해 사용되는 텍스트 기반의 설명 스크립트이다. 

1. 먼저 Dockerfile을 package.json이 있는 app 디렉터리에서 만들어보자. 이 파일은 별도의 확장자가 없다.

```shell
$ vi Dockerfile
```

```dockerfile
# syntax=docker/dockerfile:1
FROM node:12-alpine
RUN apk add --no-cache python g++ make
WORKDIR /app
COPY . .
RUN yarn install --production
CMD ["node", "src/index.js"]
```

2. Dockerfile이 있는 path에서 도커 이미지를 빌드하는 명령어를 입력한다.

```shell
$ docker build -t getting-started .
```

이 명령어는 Dockerfile로 새로운 컨테이너 이미지를 빌드할 때 사용한다. `FROM` 은 Dockerfile로 도커 이미지를 만들 때 필요한 Base 도커 이미지를 정의한다. 해당 이미지가 로컬에 없다면 당연히 도커 허브에서 pull해서 받아온다. 

이미지를 받은 후 애플리케이션에서 복사하고 yarn을 사용하여 애플리케이션의 종속성을 설치한다. CMD 지시문은 이 이미지에서 컨테이너를 시작할 때 실행할 기본 명령어를 지정한다.

마지막으로, `-t` flag는 이미지를 태깅할 때 사용하는 flag이다. 태그는 쉽게 말하면 사람이 읽기 쉬운 이름을 이미지에 부여하는 것이다. 

`docker build` 명령어 마지막에 `.`  커맨드는 이미지를 빌드할 때 필요한 Dockerfile를 현재 디렉토리에서 찾으라는 의미이다.



도커 이미지가 완성되었다면 로컬에서 다음 명령어로 확인할 수 있다.

```shell
$ docker images
REPOSITORY        TAG       IMAGE ID        CREATED      SIZE
getting-started  latest   e331a813b974   7 minutes ago   384MB
...
```



### 컨테이너 실행

도커 이미지가 완성되었다면 방금 만든 이미지로 컨테이너를 실행시켜보자.

```shell
$ docker run -dp 3000:3000 getting-started
8d1551cfa58f652d69be6abaa77f424c0806b41da9c4d784d581dbfbb88b23b2
```



그 후 localhost:3000 에 접속하면 다음과 같은 화면으로 접속이 된다.

![image-20210916005122875](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210916005122875.png)

















