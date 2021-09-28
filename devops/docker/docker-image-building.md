# Docker Image Building

## Security Scanning

이미지를 빌드했을 때 `docker scan` 명령어를 사용하여 보안 취약점을 스캔하는 것이 좋다. Docker는 Snyk와 협력하여 취약점 스캐닝 서비스를 제공한다. 

```shell
$ docker scan getting-started

Testing getting-started...

Package manager:   apk
Project name:      docker-image|getting-started
Docker image:      getting-started
Platform:          linux/amd64
Base image:        node:12.22.6-alpine3.11

✓ Tested 38 dependencies for known vulnerabilities, no vulnerable paths found.

According to our scan, you are currently using the most secure version of the selected base image

For more free scans that keep your images secure, sign up to Snyk at https://dockr.ly/3ePqVcp
```

이 스캔 방식은 취약점을 지속적으로 업데이트하는 데이터베이스를 사용한다. 그래서 output이 나중에는 변경될 수도 있다. 위의 예시에서는 취약점이 발견되지 않았다는 정보가 나오는데 취약점이 발견된다면 아래와 같이 나올 것이다.

```
✗ Low severity vulnerability found in freetype/freetype
  Description: CVE-2020-15999
  Info: https://snyk.io/vuln/SNYK-ALPINE310-FREETYPE-1019641
  Introduced through: freetype/freetype@2.10.0-r0, gd/libgd@2.2.5-r2
  From: freetype/freetype@2.10.0-r0
  From: gd/libgd@2.2.5-r2 > freetype/freetype@2.10.0-r0
  Fixed in: 2.10.0-r1

✗ Medium severity vulnerability found in libxml2/libxml2
  Description: Out-of-bounds Read
  Info: https://snyk.io/vuln/SNYK-ALPINE310-LIBXML2-674791
  Introduced through: libxml2/libxml2@2.9.9-r3, libxslt/libxslt@1.1.33-r3, nginx-module-xslt/nginx-module-xslt@1.17.9-r1
  From: libxml2/libxml2@2.9.9-r3
  From: libxslt/libxslt@1.1.33-r3 > libxml2/libxml2@2.9.9-r3
  From: nginx-module-xslt/nginx-module-xslt@1.17.9-r1 > libxml2/libxml2@2.9.9-r3
  Fixed in: 2.9.9-r4
```

* Description에서는 취약점 유형을 알려준다.
* Info에 있는 URL로 취약점에 대한 상세 정보를 알 수 있다.
* Introduced through, From 에서는 취약점 관련 라이브러리를 알려준다.

* Fixed in 에서는 취약점을 수정한 버전 정보를 알려준다.
* 자세한 내용은 [docker scan documentation](https://docs.docker.com/engine/scan/) 에서 볼 수 있다.



## Image Layering

`docker image history` 명령어를 사용하여 이미지 내 각 레이어를 생성하는 데 사용된 명령어를 볼 수 있다.

```shell
$ docker image history getting-started
IMAGE          CREATED       CREATED BY                                      SIZE      COMMENT
e331a813b974   12 days ago   CMD ["node" "src/index.js"]                     0B        buildkit.dockerfile.v0
<missing>      12 days ago   RUN /bin/sh -c yarn install --production # b…   85.2MB    buildkit.dockerfile.v0
<missing>      12 days ago   COPY . . # buildkit                             4.62MB    buildkit.dockerfile.v0
<missing>      12 days ago   WORKDIR /app                                    0B        buildkit.dockerfile.v0
<missing>      12 days ago   RUN /bin/sh -c apk add --no-cache python g++…   205MB     buildkit.dockerfile.v0
<missing>      3 weeks ago   /bin/sh -c #(nop)  CMD ["node"]                 0B        
<missing>      3 weeks ago   /bin/sh -c #(nop)  ENTRYPOINT ["docker-entry…   0B        
<missing>      3 weeks ago   /bin/sh -c #(nop) COPY file:238737301d473041…   116B      
<missing>      3 weeks ago   /bin/sh -c apk add --no-cache --virtual .bui…   7.62MB    
<missing>      3 weeks ago   /bin/sh -c #(nop)  ENV YARN_VERSION=1.22.5      0B        
<missing>      3 weeks ago   /bin/sh -c addgroup -g 1000 node     && addu…   75.7MB    
<missing>      3 weeks ago   /bin/sh -c #(nop)  ENV NODE_VERSION=12.22.6     0B        
<missing>      3 weeks ago   /bin/sh -c #(nop)  CMD ["/bin/sh"]              0B   
```

각 줄은 이미지에서의 레이어를 나타낸다. 위로 갈 수록 최신의 레이어를 나타낸다. 이를 사용하여 각 레이어의 크기를 알 수 있다. 

위에서 봤듯이 일부 정보는 ... 처리되어 잘려서 출력된다. 잘린 정보를 포함하여 보고싶다면 `--no-trunc` 플래그를 추가하면 된다.

```shell
$ docker image history --no-trunc getting-started
```



## Layer Caching

일단 [github](https://github.com/docker/getting-started/tree/master/app) 에 가서 clone 후 app 디렉토리에서 진행하자.



Layer Caching은 컨테이너를 빌드하는데 시간을 줄이도록 돕는 데 사용하는 방식이다. 여기 아래 Dockerfile을 살펴보자.

```Dockerfile
# syntax=docker/dockerfile:1
FROM node:12-alpine
WORKDIR /app
COPY . .
RUN yarn install --production
CMD ["node", "src/index.js"]
```

위에서 본 **image history output** 을 보면, Dockerfile의 각 커맨드들은 이미지에서의 layer가 된다. 여기서 이미지를 변경한다면 yarn 디펜던시를 다시 설치해야 할 것이다. 이는 별로 효율적이지는 못할 것이다.

이미지를 변경할 때 디펜던시를 재 설치하는 비효율적인 방법을 고치기 위해서 디펜던시를 캐싱하는 방법을 사용하여 Dockerfile을 다시 만들어보자. Node 기반의 애플리케이션에서는 디펜던시 관리를 `package.json` 파일에서 하고 있다. 그렇다면 `package.json` 을 copy한 후 이미지 내의 package.json 파일이 변경되었을 때만 디펜던시를 설치하는 방식으로 변경할 수도 있겠다.

1. Dockerfile에 `package.json` 을 복사하는 명령어를 추가하자. 그 후 디펜던시를 설치하고 모든 것들을 카피하도록 변경하자.

```Dockerfile
# syntax=docker/dockerfile:1
FROM node:12-alpine
WORKDIR /app
COPY package.json yarn.lock ./
RUN yarn install --production
COPY . .
CMD ["node", "src/index.js"]
```

2. `.dockerignore` 파일을 Dockerfile과 같은 디렉터리에 생성하자.

```dockerignore
node_modules
```

`.dockerignore` 파일은 이미지 관련 파일만 선택적으로 복사하는 방식이다. 이 경우에서는 두 번째 COPY 스텝에서  `node_modules` 폴더를 생략해야 한다. 그렇지 않으면 RUN 스텝에서 명령으로 생성된 파일을 덮어쓸 수 있기 때문이다.

3. 변경된 Dockerfile으로 새 이미지를 빌드해보자.

```shell
$ docker build -t getting-started .
[+] Building 19.3s (10/10) FINISHED
 => [internal] load build definition from Dockerfile
 => => transferring dockerfile: 212B
 => [internal] load .dockerignore
 => => transferring context: 53B
 => [internal] load metadata for docker.io/library/node:12-alpine
 => [1/5] FROM docker.io/library/node:12-alpine
 => [internal] load build context
 => => transferring context: 4.63MB
 => CACHED [2/5] WORKDIR /app
 => [3/5] COPY package.json yarn.lock ./
 => [4/5] RUN yarn install --production
 => [5/5] COPY . .
 => exporting to image
 => => exporting layers
 => => writing image sha256:e21e025db6978299f743ef67a6b84821129c6b0347b4240754cdca22b431f635
 => => naming to docker.io/library/getting-started
```

WORKDIR 을 제외하고 모두 재빌드되었다. 

4. 이제 `src/static/index.html` 파일을 변경해보자. (\<title> 부분을 변경할 예정 )

5. 빌드를 다시 해보자. 

```shell
$ docker build -t getting-started .
[+] Building 0.2s (10/10) FINISHED
 => [internal] load build definition from Dockerfile
 => => transferring dockerfile: 37B
 => [internal] load .dockerignore
 => => transferring context: 34B 
 => [internal] load metadata for docker.io/library/node:12-alpine
 => [internal] load build context
 => => transferring context: 3.40kB
 => [1/5] FROM docker.io/library/node:12-alpine
 => CACHED [2/5] WORKDIR /app
 => CACHED [3/5] COPY package.json yarn.lock ./
 => CACHED [4/5] RUN yarn install --production
 => [5/5] COPY . .
 => exporting to image
 => => exporting layers
 => => writing image sha256:29cf3e2a96f0af6c9b3618b0a6cb29875474ef413429decdb1e5102df0771a6b
 => => naming to docker.io/library/getting-started
```

* FROM 부터 RUN 까지 모두 캐싱되었다.



## Multi-stage Build

multi-stage build는 여러 단계를 사용하여 이미지를 만드는 데 도움이 되는 매우 강력한 도구이다. 그리고 몇 가지 장점 또한 있다.

* 빌드타임 디펜던시와 런타임 디펜던시를 분리한다.
* 앱이 실행하는 데 필요한 것들만 적재함으로써 이미지 크기를 줄인다.



### Maven/Tomcat Example

자바 기반 애플리케이션을 빌드할 때, 소스코드를 바이트코드로 컴파일하기 위해 JDK가 필요하다. 그러나 JDK는 프로덕션에서는 필요하지 않다. 그리고, 앱을 빌드할 때 Maven이나 Gradle과 같은 빌드 툴을 사용할텐데 최종 이미지에서는 그런 빌드툴 같은건 필요하지 않는다. 이럴 때 Multi-stage builds가 도움이 된다.

```Dockerfile
# syntax=docker/dockerfile:1
FROM maven AS build
WORKDIR /app
COPY . .
RUN mvn package

FROM tomcat
COPY --from=build /app/target/file.war /usr/local/tomcat/webapps 
```

위의 예시에서 첫 번째 스테이지에서는(`build`) 메이븐을 사용하여 자바 빌드를 수행한다. 두 번째 스테이지(`From tomcat`)에서는 `build` 스테이지의 결과 파일을 복사한다. 그래서 최종 이미지는 마지막 스테이지에서 생성된 이미지가 된다. 



































