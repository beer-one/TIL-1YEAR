# Multi Container Apps

대부분의 웹 애플리케이션은 웹서버-DB 가 동시에 돌아가야 한다. 그래서 웹 서버를 띄울 때 동시에 DB를 띄우고 싶어할 수도 있다. 그러면 웹 애플리케이션을 컨테이너로 만들 때 어떻게 해야 할까? 웹 서버와 DB를 하나의 컨테이너로 격리해서 띄워야할까 아니면 웹서버와 DB를 각각 다른 컨테이너로 띄워야 할까? 

일반적으로, 각 컨테이너는 한 가지 일을 맡으며, 그 일을 잘 수행해야 한다. (각각 다른 컨테이너로 띄우는게 좋다.) 이러한 이유는 다음과 같다.

* 웹 서버는 Scale-out을 해야할 수도 있는데 DB를 제외하고 웹 서버만을 늘리려면 각각 다른 컨테이너로 관리하는게 맞다.
* 한 가지 역할만을 수행하도록 컨테이너를 구성한다면 컨테이너에 대한 의존성을 분리시킬 수 있고, 업데이트에 용이하다.
* 애플리케이션 환경 별로 DB를 다르게 사용할 수도 있는데 이를 가능하게 하기 위해서는 DB를 웹서버와 함께 컨테이너화시키지 않는 것이 좋다(맞다).



이러한 이유 때문에 보통 웹 서버와 DB를 각각 다른 컨테이너로 관리하게 된다.

![image-20210927224621442](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210927224621442.png)

그러면 컨테이너 끼리 통신을 할 수 있어야 하는데 Docker에서는 컨테이너 간 통신을 어떻게 지원하는지 알아보자.



## Container Networking

기본적으로, 컨테이너 끼리는 고립된 환경에서 실행하기 때문에 같은 호스트 머신 내에서 다른 컨테이너에 관한 정보를 모른다. 그 렇기 때문에 여러 개의 컨테이너를 하나의 시스템으로 설계하기 위해서는 컨테이너와의 통신 방법을 알아야 하는데 Docker에서는 Networking 방식으로 컨테이너 끼리 통신할 수 있다.

컨테이너를 네트워크에 두는 방식은 두 가지가 있다. 하나는 **(1) 컨테이너가 시작할 때 네트워크를 할당하는 방식**이고, 다른 하나는 **(2) 이미 실행 중인 컨테이너에게 네트워크를 할당하는 방식**이다. 먼저 (1)의 방식부터 알아보자.



1. 네트워크를 생성한다.

```shell
$ docker network create todo-app

4624d89f37950bfb9f3370057aa46363c194108367f12c946688720be327fe71
```

2. MySQL 컨테이너를 실행시킴과 동시에 네트워크를 붙여보자. 데이터베이스를 사용하기 위해 데이터베이스를 초기화시킬 몇 가지 환경변수를 함께 정의한다.

```shell
$ docker run -d \
     --network todo-app --network-alias mysql \
     -v todo-mysql-data:/var/lib/mysql \
     -e MYSQL_ROOT_PASSWORD=secret \
     -e MYSQL_DATABASE=todos \
     mysql:5.7
```

* `-e MYSQL_ROOT_PASSWORD=secret` : root 계정의 비밀번호를 설정하는 명령어이다. 

3. MySQL이 정상적으로 실행 중인지 확인하기 위해 컨테이너로 접속해보자. 이 명령어를 친 후 루트계정의 비밀번호를 입력한다.

```shell
$ docker exec -it ${mysql-container-id} mysql -u root -p
```

컨테이너에 접속했다면 데이터베이스를 조회해보자.

```mysql
mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| sys                |
| todos              |
+--------------------+
5 rows in set (0.01 sec)
```



MySQL이 정상적으로 실행 중이라는 것을 확인하였다. 이제는 todo-app을 띄워서 MySQL 컨테이너와 통신하는 방법을 알아보자. 통신하는 방법을 알아내기 위해 [nicolaka/netshoot](https://github.com/nicolaka/netshoot) 컨테이너를 사용할 것이다. 이 컨테이너는 네트워킹 이슈를 트러블슈팅 하거나 디버깅할 때 유용한 툴이다.

1. nicolaka/netshoot 이미지를 사용하여 새로운 컨테이너를 실행시키자. 컨테이너를 실행시킬 때 이전에 만들었던 네트워크와 동일한 네트워크에 연결시켜야 한다.

```shell
$ docker run -it --network todo-app nicolaka/netshoot
```

2. 컨테이너 내부에서 `dig` 명령어를 사용할 것이다. `dig` 명령어는 유용한 DNS 툴인데, 이 명령어를 사용하여 `mysql` 컨테이너의 hostname과 IP 주소를 알 수 있다.

```shell
$ dig mysql

; <<>> DiG 9.16.19 <<>> mysql
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 415
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 0

;; QUESTION SECTION:
;mysql.				IN	A

;; ANSWER SECTION:
mysql.			600	IN	A	172.21.0.2

;; Query time: 3 msec
;; SERVER: 127.0.0.11#53(127.0.0.11)
;; WHEN: Mon Sep 27 14:11:50 UTC 2021
;; MSG SIZE  rcvd: 44

```

* ANSWER SECTION 에서 mysql의 `A` 레코드를 확인할 수 있는데, 이 레코드에서 mysql의 `IP주소`(172.21.0.2) 를 알 수 있다. 
* 여기서는 mysql 컨테이너의 hostname이 유효하지는 않지만, Docker는 해당 네트워크 별칭이 있는 컨테이너의 IP 주소로 이를 확인할 수 있다.





# 