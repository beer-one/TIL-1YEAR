# Docker Compose

Docker Compose는 multi-container 애플리케이션을 공유하고 정의하는데 도움이 되는 툴이다. Compose를 사용하면 서비스를 정의하는 yaml 파일을 생성할 수 있으며 단일 명령으로 모든 것을 실행시키거나 종료시킬 수 있다.

Compose를 사용하는 것에 대한 큰 장점은 애플리케이션 스택을 파일로 정의할 수 있고, 프로젝트 저장소의 루트에 유지할 수 있으며, 다른 사람이 해당 프로젝트에 쉽게 기여할 수 있도록 한다는 것이다.



## Docker Compose 설치

Docker Compose를 사용하려면 별도로 설치해야 하는데 Docker Desktop/Toolbox가 설치되어있다면 이미 Docker Compose가 설치 되어있다. 그렇지 않은 경우에는  [install Docker Compose](https://docs.docker.com/compose/install/) 에서 설치하는 방법이 설명되어있다. 설치가 되었다면 다음 명령어를 사용하여 버전을 확인할 수 있다.

```shell
$ docker-compose version
```



## Compose File 생성

1. 먼저 docker-compose.yml  파일을 생성하자.

```shell
$ touch docker-compose.yml
```

2. compose 파일에서 schema version을 정의할 수 있다. 

```yaml
version: "3.7"
```

3. 그 후, 컨테이너의 리스트를 정의할 수 있다. 

```yaml
version: "3.7"

services:
```



### App Service 정의

일단 간단한 예시로 todo-app을 정의해보겠다. 이전에 실행한 todo-app 실행 명령어는 아래와 같다.

```shell
$ docker run -dp 3000:3000 \
  -w /app -v "$(pwd):/app" \
  --network todo-app \
  -e MYSQL_HOST=mysql \
  -e MYSQL_USER=root \
  -e MYSQL_PASSWORD=secret \
  -e MYSQL_DB=todos \
  node:12-alpine \
  sh -c "yarn install && yarn run dev"
```



1. service entry와 컨테이너 이미지를 정의하자. 서비스에 대한 이름을 정의할 수 있는데 정의한 이름은 자동으로 network alias가 된다. 

```yaml
version: "3.7"

services:
  app:
    image: node:12-alpine
```

2. image 와 같은 depth에서 `command` 를 정의할 수 있다. 

```yaml
version: "3.7"

services:
  app:
    image: node:12-alpine
    command: sh -c "yarn install && yarn run dev"
```

3. 위의 `docker run` 명령어를 보면 포트를 정의하는 flag인 `-p 3000:3000` 이 있는데 docker-compose에서는 `ports` 에서 정의할 수 있다.

```yaml
version: "3.7"

services:
  app:
    image: node:12-alpine
    command: sh -c "yarn install && yarn run dev"
    ports:
      - 3000:3000
```

4. 위의 `docker run` 명령어를 보면 working directory를 정의하는 flag인 `-w /app` 이 있는데 docker-compose에서는 `working_dir` 으로 정의하며, volume을 정의하는 flag인 `-v "$(pwd):/app"` 은 docker-compose에서 `volumes` 으로 정의한다.

```yaml
version: "3.7"

services:
  app:
    image: node:12-alpine
    command: sh -c "yarn install && yarn run dev"
    ports:
      - 3000:3000
    working_dir: /app
    volumes:
    	- ./:/app
```

5. 마지막으로, 환경변수는 `environment` 에서 정의할 수 있는데 key-value 형식으로 여러 개 정의할 수 있다.

```yaml
version: "3.7"

services:
  app:
    image: node:12-alpine
    command: sh -c "yarn install && yarn run dev"
    ports:
      - 3000:3000
    working_dir: /app
    volumes:
    	- ./:/app
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: root
      MYSQL_PASSWORD: secret
      MYSQL_DB: todos
```



### MySQL 정의

Mysql 컨테이너 실행 명령어는 아래와 같다.

```shell
$ docker run -d \
  --network todo-app --network-alias mysql \
  -v todo-mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=todos \
  mysql:5.7
```



위에서 배운대로 하면 다음과 같다.

```yaml
version: "3.7"

services:
  app:
    # App Service Definition
  mysql:
    image: mysql:5.7
    volumes:
      - todo-mysql-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: todos
```



하지만 이렇게 하면 안된다.  `docker run` 명령어로 컨테이너를 실행시키면 named volume이 자동으로 생기지만 `docker-compose` 명령어로는 자동으로 생기지 않는다. 그래서 top-level에 `volumes:` 에서 named volume을 정의하고 service config에 mountpoint를 지정해야 한다. 

```yaml
version: "3.7"

services:
  app:
    image: node:12-alpine
    command: sh -c "yarn install && yarn run dev"
    ports:
      - 3000:3000
    working_dir: /app
    volumes:
      - ./:/app
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: root
      MYSQL_PASSWORD: secret
      MYSQL_DB: todos
  mysql:
    image: mysql:5.7
    volumes:
      - todo-mysql-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: todos
 
volumes:
  todo-mysql-data:
```

* app 단에서는 `./` 를 사용해서 (named volume이 아니라) 괜찮나보다.



### 실행

1. app / DB가 실행되어있다면 일단 먼저 지운다.

```shell
$ docker rm -f ${ids}
```

2. docker-compose.yml 파일이 있는 경로에서 다음 명령어로 실행시킨다.

```shell
$ docker-compose up -d
```

오타없이 입력한다면 아래와 같이 output이 찍힐 것이다.

```
Docker Compose is now in the Docker CLI, try `docker compose up`

Creating network "docker_default" with the default driver
Creating volume "docker_todo-mysql-data" with default driver
Pulling app (node:12-alpine)...
12-alpine: Pulling from library/node
6a428f9f83b0: Already exists
d0fe2b74aff9: Already exists
8a7ab7725978: Already exists
f1c9d3375a02: Already exists
Digest: sha256:1ea5900145028957ec0e7b7e590ac677797fa8962ccec4e73188092f7bc14da5
Status: Downloaded newer image for node:12-alpine
Creating docker_app_1   ... done
Creating docker_mysql_1 ... done
```

3. docker-compose에 대한 로그도 볼 수 있는데 아래 명령어로 볼 수 있다. 이 명령어로 각 서비스에 대한 로그를 볼 수 있다. `-f` 플래그는 로그를 추종하는 플래그이므로, 실시간으로 로그가 출력되어 관찰할 수 있다.

```shell
$ docker-compose logs -f
```

만약 특정 서비스에 대한 로그만 보고 싶다면 뒤에 서비스 이름을 추가하면 된다.

```shell
$ docker-compose logs -f app
```



### 종료

docker-compose로 앱을 동시에 띄운다면 해당 compose들을 모두 동시에 종료시킬 수도 있다. 아래 명령어로 가능하다.

```shell
$ docker-compose down
```



