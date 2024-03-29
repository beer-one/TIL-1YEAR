# Zookeeper

주키퍼는 분산 코디네이션 서비스를 제공하는 오픈소스이다. 주키퍼는 Leader-Follower 형식으로 구성되어 있으며, 이를 기반으로 여러 주키퍼 서버로 이루어진 앙상블, 앙상블 데이터의 불일치를 방지하고자 하는 쿼럼과 분산 데이터 시스템인 znode로 주키퍼 데이터 모델이 주키퍼를 구성하게 된다.



## 사용 용도

주키퍼는 글로벌 락, 클러스터 정보, 리더 선출, 구성 서버들 끼리 공유되는 데이터 동기화 등을 구현해야하는 곳에 활용할 수 있다. 

* 설정 관리: 클러스터의 설정 정보를 최신으로 유지하기 위한 조율 시스템
* 클러스터 관리: 클러스터의 서버가 추가되거나 제외될 때 그 정보를 클러스터 내 서버들이 공유하는 데 사용
* 리더 선출: 다중 애플리케이션 중 리더 노드를 선출하는 로직을 만드는 데 사용
* 락, 동기화: 클러스터의 쓰기 연산이 많은 경우, 경쟁상태에 들어갈 가능성이 크다. 이는 데이터 불일치를 발생시키는데, 클러스터 전체 서버들을 동기화해 경쟁상태에 들어갈 경우를 사전에 방지한다.



## 아키텍처

![structure](https://ence2.github.io/img/zookeeper.png)

앙상블 내의 주키퍼 서버들은 조율된 상태이며 항상 동일한 데이터를 가지고 있다. 따라서 어느 서버에서 데이터를 읽어도 같은 데이터를 전달받는다.

클라이언트들은 주키퍼 서버들로 이루어진 앙상블에 접근하여 znode의 데이터를 읽거나 쓰는 작업을 한다. 만약 쓰기 동작을 할 경우, 클라이언트는 앙상블 내 서버 중 하나에 접속하여 그 서버의 데이터를 업데이트한다. 업데이트된 서버는 **리더** 서버에 업데이트된 데이터를 알리고 업데이트한다. 리더 서버는 업데이트된 정보를 다른 서버들에게 브로드캐스트 형식으로 알린다. 나머지 **팔로워** 들도 업데이트 내용을 갱신하여 결국 전체 서버들의 데이터들은 일관된 상태로 유지할 수 있다.



## Znode

주키퍼에서 데이터가 저장되는 곳이며, znode를 통해 주키퍼가 제공하는 글로벌 락, 동기화, 리더 선출, 설정 관리 등의 기능을 구현할 수 있다. 데이터 모델은 리눅스 파일 시스템과 유사하며 계층형 구조이다. 

![img](https://t1.daumcdn.net/cfile/tistory/212C9A41552A787B2C)



각 znode는 stat 구조를 유지한다. stat은 znode의 메타데이터를 제공하는데 메타데이터 정보들은 아래와 같다.

* `version number`: znode의 버전 번호. znode의 데이터가 업데이트 될 때 마다 버전넘버도 같이 업데이트된다.
* `ACL (Action Control List)`: znode에 접근하기 위한 권한 획득 메커니즘이다. 해당 권한을 통해 znode의 읽기/쓰기 연산을 제어한다.
* `Timestamp`: znode가 생성되고 나서 경과된 시간 및 업데이트된 시간. (ms)
* `Data length`: 데이터의 크기. 최대 1MB 까지이다.



그리고 znode는 타입을 가지는데 타입의 종류는 3가지이다.

* `Persistence znode`: znode를 만든 클라이언트의 접속이 끊어져도 데이터 모델이 남아있는다. (default)
* `Ephemeral znode`: znode를 만든 클라이언트의 접속이 끊어지면 데이터 모델 상에서 사라진다. 자식 znode를 가질 수 없다. 이 znode는 리더 선출을 구현할 때 사용된다.
* `Squential znode`: 영속적일 수도 있고 임시적일 수도 있다. 이 znode는 10자리 연속된 숫자를 가지고 있는 이름을 가지고 만들어진다. 이 znode는 락을 구현하거나 글로벌 큐를 구현할 때 사용된다.





## Kafka에서의 주키퍼 용도

Kafka에서 주키퍼를 사용하는 이유는 여러가지가 있다.



### 컨트롤러 선정

컨트롤러는 파티션 관리를 책임지는 브로커 중 하나이다. 파티션 관리로는 리더 선정, 토픽 생성, 파티션 생성, 복제본 관리 등이 포함된다. 리더 브로커에 장애가 발생하면 컨트롤러는 팔로워 중 하나의 브로커를 리더로 선출한 후 선출한 리더를 다른 팔로워에게 알린다.



### 브로커 메타데이터

주키퍼는 카프카 클러스터 안의 모든 브로커에 대해 상태 정보를 기록한다. 프로듀서와 컨슈머는 주키퍼를 통해 브로커 상태 정보를 얻는다.



### 토픽 메타데이터

주키퍼에 파티션 수, 특정 설정 파라미터 등 토픽 메타데이터를 기록한다.









