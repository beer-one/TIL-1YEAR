# KRaft Mode Kafka (KIP-500)

Kafka 3.0 버전 부터 Zookeeper 없이 카프카 운영이 가능해졌다. Zookeeper 없이 카프카를 운영하는 방식을 **Kafka Raft metdata mode (KRaft)** 로 불린다. 대신에, 이 모드는 현재 테스팅을 위한 모드이며 **프로덕션 용으로는 적절하지 않다고 한다.** 그리고 기존 주키퍼 기반으로 된 카프카 클러스터를 해당 모드로 업그레이드 할 수 있는 방법을 제공하지 않고있다. 그리고 3.1 버전이 출시되면 KRaft 기반 클러스터를 3.1로 업그레이드 할 수 없을 수도 있다. 또한 3.0버전은 불안정해서 심각한 버그가 포함되어 있을 수 있다.



KRaft모드로 카프카를 운영한다면 메타데이터를 Zookeeper에 저장하지 않는다. 그 대신 메타데이터를 컨트롤러 노드의 KRaft 쿼럼에 저장한다. 그말인 즉슨, Zookeeper를 구동할 필요가 없다는 뜻이다. 

KRaft mode는 여러 이점이 있는데, 그 중 하나는 두 개의 서비스(Zookeeper, Kafka)를 관리하고 구성할 필요 없이 Kafka 서비스만 관리하고 구성하면 된다. 그리고 단일 프로세스 카프카 클러스터를 구축할 수 있으며 KRaft 모드는 더 높은 확장성을 가진다. 



## 아키텍처 (비교)

![img](https://cwiki.apache.org/confluence/download/attachments/123898922/a.png?version=1&modificationDate=1564694752000&api=v2)



주키퍼 기반 카프카 클러스터는 여러 브로커 노드가 있고, 주키퍼 노드인 외부 쿼럼이 포함되어있다. 컨트롤러는 선택된 후 주키퍼 쿼럼에서 해당 상태를 로드한다. 컨트롤러에서 브로커의 다른 노드로 향하는 화살표는 LeaderAndIsr 및 UpdateMetadata 메시지와 같이 컨트롤러가 푸쉬하는 업데이트를 나타낸다.

컨트롤러 외 다른 브로커들은 주키퍼와 통신할 수 있다. 그리고 외부 커맨드라인 툴과 유틸리티가 컨트롤러의 개입 없이 주키퍼의 상태를 수정할 수 있다. 이러한 문제로 인해 컨트롤러의 메모리 상태와 실제 주키퍼의 영구 상태와 같은지 여부를 알기 어렵다.

KRaft 기반 카프카 클러스터는 3개의 주키퍼 노드를 3개의 컨트롤러 노드로 대체한다. 컨트롤러 노드는 메타데이터 파티션을 위해 하나의 리더를 선출한다. 컨트롤러가 브로커에 업데이트를 푸쉬하지 않고 브로퍼가 리더에 업데이트된 메타데이터를 풀한다.

컨트롤러 프로세스가 브로커 프로세스와 논리적으로 분리되어 있지만 물리적으로 분리될 필요가 없다. 어떤 경우에는 브로커 프로세스와 동일한 노드에 컨트롤러 프로세스의 일부 또는 전체를 배포할 수도 있다. 





### 컨트롤러 쿼럼

컨트롤러 노드는 메타데이터 로그를 관리하는 Raft 쿼럼으로 구성된다. 해당 로그는 클러스터 메타데이터의 각 변경사항에 대한 정보가 포함되어있다. 토픽, 파티션, ISR, 구성 등과 같이 주키퍼에 저장된 모든 것들은 이 로그에 저장될 것이다. 

KRaft 카프카 클러스터는 외부 시스템에 의존하지 않고 Raft 알고리즘을 사용하여 컨트롤러 노드들 중 리더 노드를 선출한다. 그 중 리더 노드를 액티브 컨트롤러라고 한다. 액티브 컨트롤러는 브로커에서 만든 모든 RPC를 처리한다. 팔로워 컨트롤러는 액티브 컨트롤러가 쓴 데이터를 복제하고 액티브 컨트롤러가 실패할 경우를 대비해 대기하는 역할을 한다. 컨트롤러가 모두 최신 상태를 추저가기 때문에 컨트롤러 장애 조치는 모든 상태를 새 컨트롤러로 전송하는 긴 리로드 기간이 필요하지 않는다.

주키퍼와 같이 Raft를 계속 실행하려면 대부분의 노드가 실행되어야 한다. 그러므로, 3개의 노드로 구성된 컨트롤러 클러스터는 하나가 죽어도 살아있을 수 있다. 5개의 노드로 구성된 컨트롤러 클러스터는 2개가 죽어도 괜찮다.

주기적으로 컨트롤러는 메타데이터의 스냅샷을 디스크에 기록한다. 이는 개념적으로는 압축과 유사하지만 디스크에서 로그를 다시 읽지 않고 단순히 메모리에서 상태를 읽을 수 있기 때문에 코드 경로는 약간 다르다.





### 브로커 메타데이터 관리

컨트롤러가 다른 브로커에 업데이트를 푸쉬하지 않고 브로커가 새로운 `MetadataFetch` API를 통해 활성 컨트롤러에서 업데이트를 가져온다.  `MetadataFetch` 는 Fetch 요청과 유사하다. Fetch 요청과 같이, 브로커는 가져온 마지막 업데이트의 오프셋을 추적하고 액티브 컨트롤러에서 최신 업데이트만을 요청한다.

브로커는 디스크로부터 가져온 메타데이터를 유지한다. 이는 아주 많은 파티션이 있더라도 브로커가 아주 빠르게 시작할 수 있도록 한다.

대부분의 경우 브로커는 전체 상태가 아닌 델타(변화한 부분?)만 가져와야 한다. 그러나 브로커가 액티브 컨트롤러보다 너무 뒤떨어져 있거나 브로커에 캐시된 메타데이터가 전혀 없는 경우에는 컨트롤러는 전체 메타데이터를 이미지로 보낸다.

브로커는 메타데이터 업데이트를 위해 액티브 컨트롤러로 요청한다. 이 요청은 하트비트 역할도 해서 컨트롤러에게 브로커가 살아있음을 알린다. 



![img](https://cwiki.apache.org/confluence/download/attachments/123898922/b.png?version=1&modificationDate=1564694800000&api=v2)



### 브로커 상태 머신

주키퍼 모드에서는 브로커가 시작한 후에 주키퍼에 자신을 등록한다. 이 등록 과정은 두 가지를 수행한다. 하나는 브로커에게 컨트롤러로 선출되었는지 여부를 알리고, 나머지 하나는 다른 노드에게 컨트롤러와 통신하는 방법을 알려준다.

KRaft 모드에서는 브로커가 자신을 주키퍼가 아닌 컨트롤러 쿼럼에 등록한다.

주키퍼 모드에서는 브로커가 주키퍼 세션을 잃어버린다면 컨트롤러는 클러스터 메타데이터로부터 브로커를 제거한다. 

KRaft 모드에서는 액티브 컨트롤러가 MetadataFetch 하트비트를 오랫동안 전달받지 못하는 브로커를 클러스터 메타데이터로부터 제거한다.

주키퍼 모드에서는 주키퍼와 통신할 수 있지만 컨트롤러로부터 파티셔닝된 브로커는 사용자 요청을 계속 처리하지만 어떠한 메타데이터 업데이트를 받지 않는다. 이러면 문제가 발생할 수 있는데 예를 들어 acks=1을 사용하는 프로듀서는 실제로 더 이상 리더가 아니지만 리더를 이동시키는 컨트롤러의 LeaderAndIsrRequest를 수신하지 못하는 리더에게 계속 프로듀싱 할 수 있다.

KRaft 모드에서는 클러스터 멤버쉽은 메타데이터 업데이트와 통합된다. 브로커는 메타데이터 업데이트를 수신할 수 없는 경우 계속해서 클러스터의 구성원이 될 수 없다. 브로커가 특정 클라이언트에서 분할될 수 있지만 컨트롤러에서 분할되는 경우 브로커는 클러스터에서 제거된다.

![img](https://cwiki.apache.org/confluence/download/attachments/123898922/c.png?version=1&modificationDate=1564694862000&api=v2)



* Offine: 브로커 프로세스가 Offline 상태일 때, 전혀 실행되지 않거나 JVM 초기화 또는 로그 복구 수행과 같이 시작에 필요한 단일 노드 작업을 수행하는 과정이다.
* Fenced: 브로커가 Fenced 상태일 때, 클라이언트의 RPC에 응답하지 않는다. 브로커는 시작하고 최신 메타데이터를 가져오려 할 때 Fenced 상태가 된다. 브로커가 액티브 컨트롤러와 통신할 수 없다면 fenced 상태로 다시 변경된다. Fenced 상태의 브로커는 클라이언트에 전송되는 메타데이터에서 생략되어야 한다.
* Online: 브로커가 Online 상태일 때, 클라이언트의 RPC 요청을 받고 응답할 준비가 되어있다.
* Stopping: 브로커가 SIGINT 시그널을 받으면 해당 브로커는 Stopping 상태가 된다. 이는 시스템 운영자가 브로커를 종료하려고 함을 나타낸다. 브로커가 정지될 때는 여전히 실행 중이지만 브로커에서 파티션 리더를 마이그레이션하려고 한다. 결국, 액티브 컨트롤러는 MetadataFetchResponse의 특정한 result code를 반환함으로써 브로커를 offline 상태로 변경되도록 요청한다. 또는, 리더가 미리 결정된 시간 동안 이동할 수 없다면 브로커는 종료된다.

