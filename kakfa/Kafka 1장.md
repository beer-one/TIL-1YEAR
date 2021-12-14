# 카프카 설치



## 주키퍼 설치 및 실행

Kafka는 컨슈머 클라이언트와 카프카 클러스터에 관한 메타데이터를 저장하기 위해 주키퍼를 사용한다. 주키퍼 설치는 다음 명령어로 설치할 수 있다.

```shell
$ wget http://archive.apache.org/dist/zookeeper/zookeeper-3.4.12/zookeeper-3.4.12.tar.gz
$ tar -zxf zookeeper-3.4.12.tar.gz
$ sudo mv zookeeper-3.4.12 /usr/local/zookeeper
$ sudo mkdir -p /var/lib/zookeeper
$ cp /usr/local/zookeeper/conf/zoo_sample.cfg /usr/local/zookeeper/conf/zoo.cfg
$ export JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64
```

주키퍼 설치가 완료되었으면 실행한다.

```shell
$ /usr/local/zookeeper/bin/zkServer.sh start 

ZooKeeper JMX enabled by default
Using config: /usr/local/zookeeper/bin/../conf/zoo.cfg
Starting zookeeper ... STARTED
```

주키퍼 실행 확인은 telnet으로 할 수 있다.(주키퍼 기본 포트는 2181) telnet 명령어 입력 후 srvr 명령을 실행시키자.

```shell
$ telnet localhost 2181
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
srvr # 입력!
Zookeeper version: 3.4.12-e5259e437540f349646870ea94dc2658c4e44b3b, built on 03/27/2018 03:55 GMT
Latency min/avg/max: 0/0/0
Received: 2
Sent: 1
Connections: 1
Outstanding: 0
Zxid: 0x0
Mode: standalone
Node count: 4
Connection closed by foreign host.
```



### 주키퍼 앙상블

주키퍼의 클러스터를 앙상블이라고 한다. 하나의 앙상블은 여러 개의 서버를 멤버로 가질 수 있다. 이 때, 하나의 서버에만 서비스가 집중되지 않게 분산하여 동시에 처리하고 한 서버에서 처리한 결과를 다른 서버와 동기화시켜 데이터의 안정성을 보장한다. 또, 클라이언트와 연결되어 현재 동작 중인 서버에 문제가 생겨 서비스를 제공할 수 없을 때는 대기 중인 서버 중에서 자동 선정하여 새로 선택된 서버가 해당 서비스를 이어받아 처리함으로써 서비스가 중단되지 않게 한다. 

요청에 대한 응답을 항상 빠르고 안정적으로 하기 위해 앙상블은 홀수 개의 서버를 멤버로 갖는다. 앙상블의 서버 중 과반수가 작동 가능하다면 언제든 요청 처리가 가능하기 때문이다. (앙상블은 노드가 다섯개가 적당하다고 한다. 이유는 앙상블의 구성 변경 시에는 한 번에 하나의 서버 노드를 중단했다가 다시 로딩해야 하는데 노드가 3개이면 구성 변경 노드를 제외하고 하나가 죽어버리면 작동이 불가능하기 때문이다. 그리고 서버가 너무 많으면 오히려 성능 저하가 발생할 수 있다고 한다.)

주키퍼 서버를 앙상블로 구성하려면 각 서버가 공통된 구성 파일을 가져야 한다. 또, 각 서버는 자신의 ID 번호를 지정한 myid파일을 데이터 디렉터리에 가지고 있어야 한다.

**zoo.cfg**

```
tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=20
syncLimit=5
server.1=zoo1.example.com:2888:3888
server.2=zoo2.example.com:2888:3888
server.3=zoo3.example.com:2888:3888
```

* clientPort: 주키퍼에 접속하는 클라이언트는 clientPort에 지정된 포트번호로 앙상블과 연결
* initLimit: 팔로어가 리더에 접속할 수 있는 시간[=tickTime(ms)*initLimit], 위의 설정 기준 2000ms * 20 = 40초
* syncLimit: 리더가 될 수 있는 팔로어의 최대 개수
* server.X=hostname:peerPort:leaderPort
  * X: 서버의 ID 번호 (음이아닌 정수 값. 순차적일 필요는 없다.)
  * hostname: 호스트 이름 / ip 주소
  * peerPort: 앙상블의 서버들이 상호 통신하는 데 사용하는 포트번호
  * leaderPort: 리더를 선출하는 데 사용하는 포트번호
  * 각 서버는 dataDir에 지정된 디렉터리에 myid라는 이름의 파일을 가지고 있어야 한다. 





## 카프카 브로커 설치 및 실행

Kafka broker는 다음 명령어로 설치할 수 있다.

```shell
$ wget http://archive.apache.org/dist/kafka/2.7.0/kafka_2.13-2.7.0.tgz
$ tar -zxf kafka_2.13-2.7.0.tgz
$ sudo mv kafka_2.13-2.7.0 /usr/local/kafka
$ mkdir /tmp/kafka-logs
```

kafka broker 설치가 완료되었으면 실행해보자.

```shell
$ /usr/local/kafka/bin/kafka-server-start.sh -daemon /usr/local/kafka/config/server.properties
```

실행했으면 토픽을 생성하고 확인해보자.



**토픽 생성**

```shell
$ /usr/local/kafka/bin/kafka-topics.sh --create \
--zookeeper localhost:2181 \
--replication-factor 1 --partitions 1 --topic beer

Created topic beer.
```

**토픽 확인**

```shell
$ /usr/local/kafka/bin/kafka-topics.sh \
--zookeeper localhost:2181 \
--describe --topic beer

Topic: beer	PartitionCount: 1	ReplicationFactor: 1	Configs: 
	Topic: beer	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
```

**토픽에 메시지 쓰기**

```shell
$ /usr/local/kafka/bin/kafka-console-producer.sh \
--broker-list localhost:9092 \
--topic beer

> Test Message 1
> Test Message 2
^D # (Ctrl+D)
```

**토픽 메시지 읽기**

```shell
$ /usr/local/kafka/bin/kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic beer --from-beginning
Test Message 1
Test Message 2
^C # (Ctrl+C)
Processed a total of 2 messages
```



















