# Node

K8s는 컨테이너를 파드 내에 배치하고 노드에서 실행하여 워크로드를 구동한다. 여기서 **노드**는 클러스터에 따라 가상머신이 될 수도 있고 물리머신이 될 수도 있다. 각 노드는 `컨트롤 플레인` 에 의해 관리되며 파드를 실행하는데 필요한 서비스를 포함한다. **컨트롤 플레인 노드** 는 etcd와 API Server를 포함한 컨트롤 플레인 컴포넌트가 실행되는 머신이다.

일반적으로 클러스터를 관리할 때 여러 개의 노드를 둔다. 하지만 학습할 때는 머신이 하나뿐이면 하나만 써도 가능하긴 하다..

노드의 컴포넌트에는 kubelet, 컨테이너 런타임, kube-proxy가 포함된다.



## 노드 상태

쿠버네티스 클러스터 내 노드를 조회하는 방법은 아래와 같다.

```shell
$ kubectl get nodes
NAME         STATUS   ROLES                  AGE   VERSION
kube-cp      Ready    control-plane,master   61m   v1.22.2
kube-node1   Ready    <none>                 19m   v1.22.2
```



노드의 상태를 조회할 수 있다.

```shell
$ kubectl describe node <node-name>
```

노드의 상태를 조회하면 다양한 정보가 나오는데 상세 정보를 나눠서 살펴보자.



### Addresses

describe 명령어로 노드의 주소를 알 수 있다.

```shell
$ kubectl describe node <node-name>
...
Addresses:
  InternalIP:  192.168.0.17
  Hostname:    kube-node1
...
```

* HostName: 노드의 커널에 의해 알려진 호스트 이름. 
* ExternalIP: 노드의 외부 IP 주소. 일반적으로 노드의 IP 주소는 클러스터 내에서만 라우트 가능하기 때문에 ExternalIP가 없을 수 있다.
* InternalIP: 노드의 내부 IP 주소. 



### Conditions

https://kubernetes.io/ko/docs/concepts/architecture/nodes/#condition

![image-20211006172834880](/Users/nhn/Library/Application Support/typora-user-images/image-20211006172834880.png)





### Capacity, Allocatable

노드 상에 사용 가능한 리소스 정보를 알 수 있다.  Allocatable은 노드에 할당된 일반 파드에서 사용할 수 있는 노드의 리소스 양을 나타낸다. (Capacity - Allocatable = 노드 컴포넌트?)

```shell
$ kubectl describe node <node-name>
...
Capacity:
  cpu:                2
  ephemeral-storage:  19475088Ki
  hugepages-2Mi:      0
  memory:             2035228Ki
  pods:               110
Allocatable:
  cpu:                2
  ephemeral-storage:  17948241072
  hugepages-2Mi:      0
  memory:             1932828Ki
  pods:               110
...
```



### System Info

커널 버전, k8s 버전, 컨테이너 런타임 (종류) 버전, OS 이름과 같은 노드에 대한 시스템 정보를 보여준다. 이 정보는 kubelet에 의해 노드로부터 수집된다.

```shell
$ kubectl describe node <node-name>
  Machine ID:                 bbee473d048d4e5aac07666f2916055c
  System UUID:                8d3886bc-9508-de45-8367-c3fe058d452f
  Boot ID:                    2b8b80da-2e3f-4f94-9127-16bc4876d078
  Kernel Version:             5.4.0-88-generic
  OS Image:                   Ubuntu 20.04.3 LTS
  Operating System:           linux
  Architecture:               amd64
  Container Runtime Version:  docker://20.10.9
  Kubelet Version:            v1.22.2
  Kube-Proxy Version:         v1.22.2
```





## 하트비트

K8s 노드에서 보내는 하트비트는 노드의 가용성을 결정하는 데 도움이 되며 실패를 감지할 때 실패에 대한 핸들링을 하는데 도움이 된다.

노드에 대해서 하트비트는 두 가지 종류가 있다.

* Node의 `.status` 를 업데이트하는 것
* `kube-node-lease` 네임스페이스 내 Lease 오브젝트. 각 노드는 관련된 Lease 오브젝트를 가진다.



Node의 `.status` 를 업데이트 하는 방식에 비해 Lease는 경량 리소스이다. 하트비트를 Lease로 사용하면 큰 클러스터에 대한 업데이트의 성능 영향을 줄일 수 있다.

kubelet은 Node의 `.status` 를 생성하고 업데이트하고 관련된 Lease 오브젝트를 업데이트하는 책임을 가진다. 

* kubelet은 **상태가 변경**되거나 **상태 업데이트 간격에 대한 설정** 업데이트가 없는 경우 노드 상태를 업데이트한다. 기본 상태 업데이트 간격은 5분이다.
* kubelet은 매 10초마다 Lease 오브젝트를 생성한 후 업데이트한다. Lease 업데이트는 노드의 `.status` 업데이트와 독립적으로 발생한다. Lease 업데이트가 실패하는 경우, kubelet은 200ms 에서 시작하여 7초로 제한되는 exponential backoff를 사용하여 재시도한다. 





## 노드 컨트롤러

노드 컨트롤러는 노드의 다양한 측면을 관리하는 쿠버네티스 컨트롤 플레인 컴포넌트이다.

노드 컨트롤러는 노드가 생성되고 유지되는 동안 여러 역할을 한다.

1. 노드 등록 시점에서 노드에 CIDR 블럭을 할당한다.
2. 클라우드 벤더의 사용 가능한 머신 리스트 정보를 바탕으로 노드 컨트롤러의 내부 노드 리스트를 최신 상태로 유지한다. 클라우드 환경에서 동작 중일 경우, 노드 상태가 불량할 때 마다 노드 컨트롤러는 클라우드 벤더에 해당 노드용 VM이 사용가능한지 확인 후 사용 가능하지 않을 경우 노드 컨트롤러는 노드 리스트로부터 해당 노드를 삭제한다.
3. 노드 동작 상태를 모니터링한다. 먼저 어떠한 이유로 노드 컨트롤러가 하트비트 수신이 중단되는 경우 NodeReady 상태를 ConditionUnknown으로 업데이트 한다. 그리고 노드가 계속 접근 불가능할 경우, 노드로부터 정상적인 종료를 이용하여 모든 파드를 제거한다. `ConditionUnknown` 을 알리기 시작하는 기본 타임아웃 값은 40초이며 파드를 제거하기 시작하는 값은 5분이다.



### 안정성

대부분의 경우에서, 노드 컨트롤러는 초당 `--node-eviction-rate` 개의 속도로 파드를 제거한다. *(default = 0.1)* 기본적으로 파드를 10초당 한개 이상 제거하지 않는다는 의미이다.

노드 제거 행위는 주어진 가용성 영역 내 하나의 노드가 불량한 비율에 따라 행위가 달라진다. 노드 컨트롤러는 영역 내 불량(NodeReady 상태가 `ConditionUnknown` 또는 `ConditionFalse`) 한 노드의 비율을 체크한다. 

* 불량 노드 비율이 `--unhealthy-zone-threshold`*(default = 0.55)* 보다 적다면, 제거 속도가 줄어든다.
* 클러스터가 작은 경우 (`--large-cluster-size-threshold`*(default=50)* <= node 개수) 제거가 중단된다.
* 위 두 가지 경우가 아닌 경우, 제거 속도는 초당 `--secondary-node-eviction-rate`*(default=0.01)* 으로 줄어든다.



이 정책들이 가용성 영역 단위로 실행되는 이유는 나머지가 연결되어있는 동안 하나의 가용성 영역이 마스터로부터 분할되어질 수도 있기 때문이다. 클러스터가 여러 클라우드 제공 사업자의 가용성 영역에 걸쳐있지 않다면 오직 하나의 가용성 영역만 존재하게 된다.

노드가 가용성 영역에 걸쳐 퍼져있는 주된 이유는 하나의 전체 영역이 장애가 발생하는 경우 워크로드가 상태 양호한 영역으로 이전될 수 있도록 하기 위해서이다. 그러므로 하나의 영역 내 모든 노드가 불량인 경우, 노드 컨트롤러는 일반적인 속도인 `--node-eviction-rate` 로 노드를 제거한다. 코너 케이스는 모든 영역이 전체적으로 불량한 경우인데, 이러한 경우에는 노드 컨트롤러는 컨트롤 플레인과 노드 사이의 연결에 대한 문제가 있을 것으로 간주하고 어떠한 노드 제거도 하지 않는다. 

노드 컨트롤러는 또한 taint를 무시하고 실행할 수 있는 파드라도 노드에서 실행 중이면서 `NoExecute` taint가 있는 파드를 제거하는 책임을 가진다. 노드 컨트롤러는 또한 노드에 도달할 수 없거나 준비되지 않는 것과 같은 노드 문제에 대응하는 taint를 추가한다. 이는 스케줄러가 불량한 노드로 파드를 배치하지 않음을 의미한다.



## 노드 용량

노드 객체는 노드의 리소스 용량에 관한 정보를 트래킹한다. 노드의 자체 등록은 등록하는 중에 용량을 보고한다. 수동으로 노드를 추가하는 경우, 추가할 때 노드 용량 정보를 설정해야 한다.

K8s 스케줄러는 노드 상의 모든 파드에 대해 충분한 리소스가 존재하도록 보장한다. 스케줄러는 노드 위의 컨테이너의 요청의 합이 노드의 용량보다 크지 않은지 확인한다. 요청의 합은 컨테이너 런타임에 의해 직접 시작된 컨테이너를 제외하고 kubelet의 제어 밖에서 실행되는 다른 프로세스를 제외한 kubelet에 의해 관리되는 모든 컨테이너를 포함한다.



























