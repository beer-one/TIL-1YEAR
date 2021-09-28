# Docker File System

컨테이너를 실행하면, 컨테이너의 파일 시스템을 위해 이미지로부터 다양한 레이어를 사용한다. 각 컨테이너는 파일을 생성하고 수정하고 삭제하기 위해 자신의 `scratch space` 를 얻는다. 이렇게 하면 같은 이미지를 사용하는 컨테이너에서 한 컨테이너가 변경되더라도 다른 컨테이너에게는 절대 영향이 가지 않는다.



이를 알아보기 위해서 같은 이미지를 사용하는 두 컨테이너를 띄워본 후 각각 파일을 생성해보도록 하자.

1. `ubuntu` 컨테이너를 하나 띄워서 아무 텍스트파일을 하나 생성한다.

```shell
$ docker run -d ubuntu bash -c "shuf -i 1-10000 -n 1 -o /data.txt && tail -f /dev/null"
```

* && 이전의 명령어는 1~10000 의 숫자 중 하나를 랜덤으로 /data.txt에 쓰는 명령어이다.
* && 이후의 명령어는 컨테이너를 지속적으로 실행시키기 위해 파일을 보는 명령어이다.

2. 컨테이너에 접속하여 텍스트파일이 있는지 확인하자. 그렇게 하기 위해서 Dashboard를 열어 CLI 버튼을 입력하거나 다음 명령어를 입력하면 된다. 텍스트파일을 확인하기 위해서 cat 명령어를 사용하자.

<img width="1047" alt="스크린샷 2021-09-26 오후 11 12 10" src="https://user-images.githubusercontent.com/35602698/134813048-d4d64616-b668-493f-a973-00ded7272053.png">

```shell
$ docker exec -it ${containerId} /bin/sh
```

```shell
$ cat /data.txt
9733
```

더 간단하게 컨테이너를 /bin/sh로 접속하지 않고 곧바로 명령어로 확인할 수도 있다.

```shell
$ docker exec ${containerId} cat /data.txt
```

3. 같은 `ubuntu` 이미지를 사용하여 새로운 컨테이너를 생성해보자. 이 때 컨테이너를 생성할 때는 파일을 만들지 않고 생성 시 어떤 파일을 가지고있는지 확인하는 명령어를 사용함으로써 컨테이너가 이미지에 영향을 끼치는지 알아보자.

```shell
$ docker run -it ubuntu ls /

bin   dev  home  lib64	mnt  proc  run	 srv  tmp  var
boot  etc  lib	 media	opt  root  sbin  sys  usr
```

* 1 에서 생성한 /data.txt 파일이 없다는 것이 확인된다. 



## Container volumes

위의 실습에서 각 컨테이너는 컨테이너가 시작할 때 이미지에 대한 내용으로부터 시작된다는 것을 확인하였다. 각 컨테이너는 파일을 추가하고, 수정하고 삭제할 수 있고, 각 컨테이너는 고립되어있기 때문에 컨테이너에서 파일에 대한 변경은 다른 컨테이너에게 영향을 끼치지 않는다. 

`Volume` 은 컨테이너의 특정 파일 시스템 경로를 호스트 시스템에 다시 연결하는 기능을 제공한다. 컨테이너의 디렉터리가 마운트된다면 해당 디렉터리의 변경은 호스트 머신에 영향을 끼친다. 컨테이너를 시작할 때 특정 디렉터리를 마운트시킨다면 컨테이너들 끼리 파일을 공유할 수 있다.

Docker에서 제공하는 Volume은 총 두 가지가 있다.



### Persist the todo data

기본적으로, todo 앱은 데이터를 컨테이너 파일 시스템인 `/ect/todos/todo.db` 에 SQLite database 에 저장한다. 데이터베이스가 단일 파일인 경우, 호스트에서 해당 파일을 유지하여 다음 컨테이너에서 사용할 수 있도록 하려면 마지막 파일이 중단된 부분을 복구할 수 있어야 한다. Volume을 생성하고 데이터가 저장될 디렉터리를 마운팅함으로써 컨테이너가 죽어도 데이터를 지속적으로 유지할 수 있다.

호스트 머신에 데이터를 저장하기 위해서 먼저 `named volume` 을 사용할 수 있다. named volume을 간단히 데이터의 버킷으로 생각할 수 있다. Docker는 디스크의 물리적 위치를 유지하며 볼륨 이름만 기억하면 된다. 볼륨을 사용할 때 마다 Docker는 올바른 데이터가 제공되었는지 확인한다.

1. `docker volume create` 명령어로 볼륨을 생성한다.

```shell
$ docker volume create todo-db
```

2. todo-app에서 볼륨을 사용하기 위해 todo app 컨테이너가 실행중이라면 컨테이너를 멈춘 후 컨테이너를 제거하자. 
3. 볼륨 마운트를 지정하기 위해 `-v` 플래그를 이용하여 todo app 컨테이너를 실행시키자. 이렇게 하면 named volume을 사용할 것이고 볼륨이 `/etc/todos` 로 마운트된다.

```shell
$ docker run -dp 3000:3000 -v todo-db:/etc/todos getting-started
```

4. localhost:3000 으로 접속하여 데이터를 막 생성하자.

![스크린샷 2021-09-26 오후 11 45 11](https://user-images.githubusercontent.com/35602698/134813058-6b0750d1-d164-4723-a5d0-7b746795c4a2.png)


5. 컨테이너를 재시작하여 데이터가 유지되는지 확인하자. 아마 유지될 것이다.

```shell
$ docker rm -f ${containerId}
$ docker run -dp 3000:3000 -v todo-db:/etc/todos getting-started
```



### volume 확인하기

실제로 도커 볼륨이 어떻게 저장되는지 확인할 수 있는 명령어가 있다.

```shell
$ docker volume inspect todo-db

[
    {
        "CreatedAt": "2021-09-26T14:44:47Z",
        "Driver": "local",
        "Labels": {},
        "Mountpoint": "/var/lib/docker/volumes/todo-db/_data",
        "Name": "todo-db",
        "Options": {},
        "Scope": "local"
    }
]
```

* Mountpoint 는 데이터가 저장되어있는 디스크의 실제 공간을 의미한다. 



