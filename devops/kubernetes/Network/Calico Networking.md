# Calico Networking

https://projectcalico.docs.tigera.io/networking/determine-best-networking



Calico의 유연한 모듈식 아키텍처는 광범위한 배포 옵션을 지원한다. 따라서, 프로젝트 환경과 필요에 따라 가장 적합한 네트워킹 접근법을 선택할 수 있다. 이 글에서는 BGP가 있거나(non-overlay) 없는(overlay) 모드에서 다양한 CNI 및 IPAM 플러그인과 기본 네트워크 타입으로 실행할 수 있는 기능이 포함된다.

Calico에서 사용 가능한 네트워크 선택 사항을 완전히 이해하려면, 다음에 설명된 개념을 숙지하고 이해하는 것이 좋다.  



## K8S 네트워킹 기본사항

쿠버네티스 네트워크 모델은 다음과 같은 `flat` 네트워크를 정의한다.

* 모든 파드는 자신의 고유 IP를 가진다.
* 파드가 어떠한 노드에 배치되던 간에 NAT 없이 서로 다른 노드에 있는 파드끼리 통신이 가능하다.



이렇게 하면 포트 할당, 이름 지정, 서비스 디스커버리, 로드밸런싱, 애플리케이션 구성, 마이그레이션 등의 관점에서 파드를 VM이나 물리머신처럼 취급할 수 있는 깨끗하고 이전 버전과 호환되는 모델이 생성된다. 이러한 기본 네트워킹 기능 내에서 트래픽을 제한하는 네트워크 정책을 사용하여 네트워크 세그멘테이션을 정의할 수 있다.

이 모델에는 여러가지 네트워킹 접근 방식과 환경을 지원하기 위한 많은 유연성이 있다. 네트워크가 구현되는 방법에 대한 정확한 세부사항은 사용 중인 **CNI**, **네트워크**, **클라우드 프로바이더 플러그인**의 조합에 따라 다르다.



## CNI 플러그인

CNI (Container Network Interface) 는 다양한 네트워크 구현체를 쿠버네티스에 연결할 수 있는 표준 API이다. 쿠버네티스는 파드가 생성되거나 삭제될 때 마다 API를 호출한다. 여기에는 두 가지 종류의 CNI 플러그인이 있다.

* CNI network plugins: 쿠버네티스 파드 네트워크에서(로) 파드를 추가하거나 삭제하는 일을 담당한다. 이는 파드의 네트워크 인터페이스를 생성/삭제 및 나머지 네트워크 구현에 연결/연결 해제가 포함된다.
* CNI IPAM plugins: 파드가 생성되거나 삭제될 때 파드에 대한 IP주소 할당 및 해제를 담당한다. 플러그인에 따라 이는 하나 이상의 IP 주소 범위를 각 노드에 할당하거나 기본 퍼블릭 클라우드 네트워크에서 IP 주소를 가져와 파드에 할당하는 작업이 포함될 수 있다.



## Overlay networks

Overlay network는 다른 네트워크에 의해 계층화된 네트워크이다. 쿠버네티스의 컨텍스트에서 overlay network는 파드 IP 주소를 알지 못하거나 어떤 파드가 어떤 노드에서 실행되고 있는지 알지 못하는 기본 네트워크 상단의 노드에서 파드 간 트래픽을 처리하는 데 사용할 수 있다. Overlay network는 기본 네트워크가 처리하고 있는 방법을 알고 있는 외부 패킷 (노드 IP 주소) 내에서 기본 네트워크가 처리하는 방법을 알지 못하는 네트워크 패킷 (파드 IP 주소)을 캡슐화 함으로써 작동한다. 캡슐화에 사용되는 두 가지 일반 네트워크 프로토콜은 `VXLAN` 과 `IP-in-IP` 가 있다.

Overlay network를 사용하면서 얻을 수 있는 주요 장점은 기본 네트워크의 의존성이 감소된다는 것이다. 예를 들어, 기본 네트워크와 통합하거나 기본 네트워크를 변경할 필요 없이 모든 기본 네트워크 위에서 VXLAN overlay를 실행할 수 있다.

단점은 아래와 같다.

* 약간의 성능 영향이 있다. 패킷을 캡슐화하는 과정은 약간의 CPU를 필요로 하며 캡슐화된 패킷을 인코딩하기 위해 패킷에추가 바이트가 필요하기 때문에 전송할 수 있는 내부 패킷의 최대 크기가 줄어든다. 이는 결국 동일한 총 데이터에 대해 더 많은 패킷을 보내야 함을 의미한다.
* 파드 IP주소가 클러스터 밖에서 라우팅이 되지 않는다.



### Cross-subnet overlays

표준 VXLAN, IP-in-IP 외에도, Calico는 VXLAN과 IP-in-IP에 대한 `cross-subnet` 모드를 지원한다. 이 모드에서는 각 서브넷 내에서 기본 네트워크는 L2 네트워크 역할을 한다. 단일 서브넷 내로 전송되는 패킷은 캡슐화되지 않아서 non-overlay network의 성능을 얻을 수 있다. 서브넷을 통해 전송되는 패킷은 일반적은 overlay network처럼 캡슐화되어 기본 네트워크에 대한 종속성을 줄인다.

표준 overlay network와 같이, 기본 네트워크는 파드의 IP 주소를 모르고 파드 IP 주소는 클러스터 외부로 라우팅이 불가능하다.





## 클러스터 외부로의 Pod IP 라우팅 가능성

다양한 쿠버네티스 네트워크 구현의 중요한 구별 기능은 파드 IP주소가 더 넓은 네트워크에서 클러스터 외부로 라우팅될 수 있는지 여부이다.

### Not Routable

파드 IP 주소가 클러스터 외부로 라우팅이 불가능한 경우 파드가 클러스터 외부에 있는 IP 주소에 대한 네트워크 연결을 설정하려고 할 때, 쿠버네티스는 SNAT(Source Network Address Translation)으로 불리는 기술을 사용하여 출발지 IP 주소를 파드의 IP 주소에서 파드를 호스팅하는 노드의 IP 주소로 변경한다. 연결의 모든 반환 패킷은 자동으로 파드의 주소에 다시 매핑된다. 그래서 파드는 SNAT가 발생하는지 모르고, 연결에 대한 목적지는 연결의 시적점으로 노드를 본다. 또한 기본 네트워크는 파드의 IP주소를 볼 수 없다.

클러스터 외부의 무언가가 파드에 연결되어야 하는 반대 방향 연결의 경우, 쿠버네티스 서비스나 인그레스를 통해 수행할 수 있다. 네트워크는 패킷을 파드의 IP주소로 연결하는 방법을 모르기 때문에 클러스터 외부에서 직접 파드 IP주소와 연결할 수 없다. 



### Routable

파드의 IP주소가 클러스터 외부로 라우팅이 가능한 경우, 파드가 SNAT 없이 외부와 통신할 수 있고, 외부는 쿠버네티스 서비스 또는 쿠버네티스 인그레스를 통하지 않고 직접 파드에 연결할 수 있다.

클러스터 외부에서 라우팅할 수 있는 파드 IP 주소의 이점은 아래와 같다.

* 외부 연결에 대한 SNAT을 피하는 것은 기존 보안 요구사항과 통합하는 데 필수적일 수 있다. 작업 로그의 디버깅 및 이해도를 단순화할 수 있다.
* 쿠버네티스 서비스나 인그레스를 통하지 않고 일부 파드에 직접 접근 가능하는 것이 필요한 특수한 워크로드가 있는 경우 라우팅 가능한 파드 IP는 호스트 네트워크 파드를 사용하는 것 보다 운영상 더 간단할 수 있다.

단점은 아래와 같다.

* 해당 파드 IP는 네트워크 전반에 대해 유일해야 한다. 



### 라우팅 가능성을 결정하는 요인

클러스터에서 overlay network를 사용 중이라면, 파드 IP는 일반적으로 클러스터 외부로 라우팅이 불가능하다. Overlay network를 사용하지 않는다면, 파드 IP가 클러스터 외부로 라우팅 가능한지 여부는 CNI 플러그인, 클라우드 프로바이더 또는 실제 네트워크와의 BGP 피어링의 조합에 따라 달라진다.



## BGP

BGP (Border Gateway Protocol) 는 라우트를 네트워크 전반에 공유하기 위한 표준 기반 네트워킹 프로토콜이다. 뛰어난 확장성 특성을 가진 인터넷의 기본 빌딩 블록 중 하나이다.

Calico는 BGP를 지원한다. 온 프레미스 배포에서 Calico는 물리적 네트워크와 피어 경로를 교환할 수 있으므로 네트워크에 연결된 다른 워크로드와 마찬가지로 파드 IP주소를 더 넓은 네트워크에서 라우팅할 수 있는 non-overlay network를 만들 수 있다.



## Networking Options

on-prem에서 Calico의 가장 일반적인 네트워크 설정은 BGP를 사용하여 물리 네트워크와 피어링하여 파드 IP를 클러스터 외부에서 라우팅할 수 있도록 하는 `non-overlay` 모드이다. 이 설정은 쿠버네티스 서비스 IP를 알리는 기능과 파드, 네임스페이스, 노드 수준에서의 IP 주소 관리를 제어하여 기존 엔터프라이즈 네트워크 및 보안 요구사항과 통합할 수 있는 광범위한 통합 가능성을 지원하는 등 다양한 고급 calico 기능을 제공한다.

물리 네트워크에 대한 BGP 피어링이 옵션이 아닌 경우, 클러스터가 단일 L2 네트워크 내에 있는 경우 `non-overlay` 모드를 실행할 수도 있으며, Calico는 클러스터의 노드 간에 BGP를 피어링한다. 네트워크가 파드의 IP주소를 알지 못하기 때문에 엄밀히 말하면 오버레이 네트워크는 아니지만, 파드의 IP는 클러스터 외부러 라우팅이 불가능하다. 

![image-20211227182541844](/Users/nhn/Library/Application Support/typora-user-images/image-20211227182541844.png)





위의 방법 대신에 각 L2 서브넷 내에서 성능을 최적화하기 위해 cross-subnet overlay mode와 함께 `VXLAN` 이나 `IP-in-IP` overlay mode에서 Calico를 실행할 수 있다.

![image-20211227183322659](/Users/nhn/Library/Application Support/typora-user-images/image-20211227183322659.png)







# 구성



## BGP 피어링 구성

Calico 노드는 BGP를 통해 라우팅 정보를 교환하여 Calico 네트워크 워크로드에 연결할 수 있다. 온프레미스 배포에서 이를 통해 네트워크의 나머지 부분에서 워크로드를 일급시민으로 만들 수 있다. 퍼블릭 클라우드 배포에서는 클러스터 내부에서 라우팅 정보를 분산시키는 효과적인 방법을 제공하며 보통 IPIP 오버레이 또는 cross-subnet mode와 함께 사용된다.

BGP는 네트워크에서 라우터 사이에 라우팅 정보를 교환하는 데 사용하는 표준 프로토콜이다. BGP에서 구동 중인 각 라우터는 하나 이상의 BGP peer(BGP를 통해 통신하는 다른 라우터)가 있다. Calico 네트워킹을 각 노드에 가상 라우터를 제공하는 것으로 생각할 수 있다. Route reflectors 또는 top-of-rack(ToR) 라우터를 사용하여 서로 피어링하도록 Calico 노드를 구성할 수 있다.

환경에 따른 BGP 네트워크를 구성하는 여러가지 방식이 있다. 



### Full-mesh

BGP가 활성화된 경우, Calico의 기본 동작은 각 노드가 서로 피어링하는 내부 BGP 연결의 `full-mesh` 를 생성하는 것이다. 이를 통해 Calico는 퍼블릭 클라우드, 프라이빗 크라우드에 관게없이 모든 L2 네트워크에서 작동하거나 IPIP가 구성된 경우 IPIP 트래픽을 차단하지 않는 모든 네트워크에서 오버레이로 작동할 수 있다. Calico는 VXLAN overlay에 대한 BGP를 사용하지 않는다.

`full-mesh` 는 100 개 노드 이하의 소,중규모의 클러스터에서 잘 작동하지만, 대규모 클러스터에서는 효율성이 떨어진다는 단점이 있다. 보통 대규모에서는 `route reflectors` 를 권장한다.



### Router reflectors

큰 규모의 내부 BGP의 클러스터를 구축하기 위해서는, 각 노드에서 사용되는 BGP peering의 개수를 줄이기 위해 BGP `route reflectors` 가 사용될 수 있다. 이 모델에서, 일부 노드는 route reflectors 역할을 하며 그들 사이에 full mesh를 설정하도록 구성된다. 다른 노드들은 그 route reflectors의 서브셋과 피어링하도록 구성되며 full-mesh에 비해 총 BGP peering 연결 개수를 줄인다.



### Top of Rack (ToR)

온 프레미스 배포에서, 물리 네트워크 인프라스트럭쳐와 직접 피어링하도록 Calico를 구성할 수 있다. 일반적으로, 여기에는 Calico의 기본 full-mesh 동작을 비활성화하고 대신 Calico를 L3 ToR 라우터와 피어링하는 작업이 포함된다. 여기에는 온프레미스 BGP 네트워크를 구축하는 여러가지 방식이 있다. BGP 구성 방법은 사용자에게 달려있다. Calico는 iBGP와 eBGP 구성 모두 잘 작동하며 네트워크 설계에서 Calico를 다른 라우터처럼 효과적으로 취급할 수 있다.

토폴로지에 따라 각 rack 내부에서 BGP route reflector를 사용할 수도 있다. 하지만 이는 일반적으로 각 L2 도메인의 노드 수가 많은 경우 (100개 이상) 에만 필요하다.

NHN Cloud에서는 안되는거롤..?



## 실제 구성 방법

일단 실제로 구성하기 위해서는 calicoctl을 설치해야 한다.

Calico를 최초로 설치했을 경우에는 `node-to-node mesh` 로 구동한다. 이를 확인하기 위해서 아래 명령어를 사용해보자. (이 명령어는 `10.104.17.84` 에서 입력했다고 가정)

```shell
$ sudo calicoctl node status
Calico process is running.

IPv4 BGP status
+--------------+-------------------+-------+------------+-------------+
| PEER ADDRESS |     PEER TYPE     | STATE |   SINCE    |    INFO     |
+--------------+-------------------+-------+------------+-------------+
| 10.104.17.66 | node-to-node mesh | up    | 2021-11-05 | Established |
| 10.104.17.79 | node-to-node mesh | up    | 2021-11-05 | Established |
| 10.104.17.55 | node-to-node mesh | up    | 2021-12-22 | Established |
| 10.104.17.71 | node-to-node mesh | up    | 2021-12-08 | Established |
| 10.104.17.66 | node-to-node mesh | up    | 2021-12-08 | Established |
+--------------+-------------------+-------+------------+-------------+
```



Node-to-node mesh는 말그대로 full-mesh 형식으로 돌아간다. 이는 모든 노드가 라우트를 브로드캐스트하기 위해 다른 모든 노드에 피어링이 되어있음을 의미한다. 하지만 노드 개수가 많이 늘어나면 node-to-node mesh를 더 이상 확장할 수 없다. (피어링 개수가 nC2 = O(N^2) 개?)

![img](https://ml89fc3hpqdi.i.optimole.com/DJ4BLEo-mMkOfziB/w:477/h:134/q:100/https://www.tigera.io/wp-content/uploads/2019/03/routes.png)



그래서 노드 개수가 많다면 node-to-node 대신 route reflector로 변경하는 것이 좋다.

![img](https://ml89fc3hpqdi.i.optimole.com/DJ4BLEo-pNrtRgJA/w:477/h:257/q:100/https://www.tigera.io/wp-content/uploads/2019/03/route-reflection.png)

 Route reflectors로 변경하려면 3가지 방법 중 하나를 택하면 된다.

1. 일부 K8S 노드를 route reflector로 사용
2. `calico/node` 를 쿠버네티스가 아닌 호스트에서 컨테이너로 실행
3. 전용 BIRD 바이너리와 같은 다른 reflector를 실행하고 calico를 설정하여 피어링

그 중 1번 방법이 제일 낫다. (추가 호스트가 필요없기 때문)





> Router reflectors로 사용할 노드 개수는 노드가 하나일 때 피어링 개수가 가장 적다 (N-1개). 하지만 가용성을 위해 최소 2~3개 정도의 노드를 사용하는 것이 좋다. (그래도 O(N)개 이다.)



### Node 구성

calicoctl이 설치가 되었다면 route reflector로 변경할 노드를 하나 선택하여 다음 명령어를 사용하자. (두개정도를 사용하자.)

```shell
$ calicoctl get node ${node-name} --export -o yaml > ${node-name}.yaml
```

그러면 yaml 파일이 나오는데 yaml 파일을 아래와 같이 편집하자.

```yaml
apiVersion: projectcalico.org/v3
kind: Node
metadata:
  name: zyisy
  labels:
    route-reflector: true # 추가
spec:
  bgp:
    ipv4Address: 10.30.0.13/22
    ipv4IPIPTunnelAddr: 192.168.0.1
    routeReflectorClusterID: 244.0.0.1 # 추가
```

* `metadata.labels.router-reflector: true` 옵션을 추가하자. 이 옵션을 추가하면 해당 노드에 해당 레이블이 추가된다.
* `spec.bgp.routeReflectorClusterID: 1.0.0.1` 옵션을 추가하자. `routeReflectorClusterID` 의 값으로는 사용하지 않는 IP 주소를 입력하면 된다.



편집 후에 yaml 파일을 적용시키자.

```shell
$ calicoctl replace -f ${node-name}.yaml
```



### BGP 피어링 구성

BGPPeer 리소스는 피어링할 노드를 정의한다. 이는 또한 ToR 라우터에 대한 피어링을 구성하여 Calico 네트워크와 데이터 센터 페브릭에 피어링할 때 일반적으로 사용된다. 

추가된 `route-reflector: true` 레이블을 사용하여 피어링해야 하는 reflector node의 동적 감지를 쉽게 설정할 수 있다. `calico/node` 인스턴스가 reflector 인스턴스와 피어링을 보장하기 위해 아래 노드 피어링 연결을 추가하자.

```yaml
kind: BGPPeer
apiVersion: projectcalico.org/v3
metadata:
  name: node-peer-to-rr
spec:
  nodeSelector: !has(route-reflector)
  peerSelector: has(route-reflector)
```

그리고 route-reflector 끼리의 피어링을 추가하자.

```yaml
kind: BGPPeer
apiVersion: projectcalico.org/v3
metadata:
  name: rr-to-rr-peer
spec:
  nodeSelector: has(route-reflector)
  peerSelector: has(route-reflector)
```

그 후 BGPPeer를 적용하자.

```shell
$ calicoctl apply -f bgp-peer.yaml
```



적용 후 node status가 변경됨을 알 수 있다.

```shell
$ sudo calicoctl node status
Calico process is running.

IPv4 BGP status
+--------------+-------------------+-------+------------+-------------+
| PEER ADDRESS |     PEER TYPE     | STATE |   SINCE    |    INFO     |
+--------------+-------------------+-------+------------+-------------+
| 10.104.17.66 | node-to-node mesh | up    | 01:43:10   | Established |
| 10.104.17.79 | node-to-node mesh | up    | 2021-11-05 | Established |
| 10.104.17.55 | node-to-node mesh | up    | 2021-12-22 | Established |
| 10.104.17.71 | node-to-node mesh | up    | 2021-12-08 | Established |
| 10.104.17.66 | global            | start | 02:44:05   | Idle        |
| 10.104.17.79 | global            | start | 02:44:05   | Idle        |
+--------------+-------------------+-------+------------+-------------+
```



### node-to-node 메쉬 비활성화

마지막으로, 노드가 오직 위에서 구성했던 reflector와 피어링 할 수 있도록 node-to-node 메쉬를 비활성화해야 한다. 이는 기본 `BGPConfiguration` 을 추가하거나 변경해야 한다.

일단 기존 구성이 있는지 확인한 후 있다면 변경하고 없으면 새로 만들어야 한다.

```shell
$ calicoctl get bgpconfig default
resource does not exist: BGPConfiguration(default) with error: bgpconfigurations.crd.projectcalico.org "default" not found
```

* 이런 메시지가 나온다면 구성이 없다는 뜻이다.



일단 구성을 생성하자.

```yaml
apiVersion: projectcalico.org/v3
kind: BGPConfiguration
metadata:
  name: default
spec:
  logSeverityScreen: Info
  nodeToNodeMeshEnabled: false
  asNumber: 63400
```

* `spec.nodeToNodeMeshEnabled: false` 로 설정하여 node-to-node 를 비활성화한다.
* `spec.asNumber` 를 추가하여 Autonomous System number를 설정한다. asNumber는 데이터센터 전체의 다른 BGP 구성에 따라 다를 수 있다.





















