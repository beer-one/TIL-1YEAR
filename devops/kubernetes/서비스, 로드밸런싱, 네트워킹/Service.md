# 서비스

K8s에서 서비스는 일련의 파드로 구동 중인 애플리케이션을 네트워크 서비스로서 외부로 노출시키는 추상적인 방법이다.

K8s를 사용하면 익숙하지 않은 서비스 디스커버리 매커니즘을 사용하기 위해 애플리케이션을 변경할 필요가 없다. K8s는 파드에게 고유의 IP 주소와 파드 집합에 대한 하나의 DNS 이름을 부여하고 파드 집합에 대해 로드밸런싱을 수행할 수 있다.

파드는 클러스터의 상태에 맞게 생성되고 삭제되기 때문에 파드는 영구적인 리소스는 아니다. 앱을 구동시키기 위해 디플로이먼트를 사용한다면, 파드를 동적으로 생성하고 삭제시킬 수 있다.

각 파드는 고유의 IP 주소를 가지지만 디플로이먼트에서는 한 시점에 실행되는 파드 집합이 잠시 후 실행되는 파드 집합과 다를 수 있다. 이러한 점은 문제를 일으키는데, 일부 파드 집합이 클러스터 내부의 다른 파드에게 어떤 기능을 제공하거나 프론트엔드가 백엔드의 기능을 사용하는 경우, 파드가 변경되면 변경된 파드의 IP주소를 알 수 없다. 이런 이유 때문에 서비스가 필요하다.



## 서비스 리소스

K8s에서 서비스는 논리적인 파드집합과 정책을 정의하는 추상이다. 셀렉터에 의해 서비스에 타게팅될 파드집합이 결정된다. 



### 클라우드 기반 서비스 디스커버리

애플리케이션에서 서비스 디스커버리 용도로 K8s API를 사용할 수 있다면 서비스 내 파드가 변경될 때 마다 업데이트되는 엔드포인트를 apiservice에 질의할 수 있다.

native 애플리케이션이 아닌 경우, K8s는 애플리케이션과 백엔드 파드 사이에 네트워크 포트 또는 로드밸런서를 두는 방식을 제공한다.



## 정의

K8s에서 서비스는 파드와 같은 REST 객체이다. 모든 REST 객체와 같이, 서비스 정의를 apiserver에 POST 요청하여 새 인스턴스를 만들 수 있다. 서비스 객체의 이름은 [RFC 1035 label name](https://kubernetes.io/docs/concepts/overview/working-with-objects/names#rfc-1035-label-names) 룰을 따라야 한다.

아래 예시는 각 9376번 포트를 듣고 `app=MyApp` 라벨을 포함하는 파드의 셋을 가지는 서비스에 대한 명세이다. 여기서는 서비스의 이름을 `my-service` 로 명명하였다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 9376
    - name: https
      protocol: TCP
      port: 443
      targetPort: 9377
```

K8s는 서비스에게 서비스 프록시가 사용할 IP 주소를 할당한다. (이를 clusterIP 라고도 불린다.) 

서비스 셀렉터에 대한 컨트롤러는 서비스에서 정의한 셀렉터와 일치하는 파드를 지속적으로 스캔한다. 그 후 모든 변경사항을 서비스와 같은 이름을 가진 엔드포인트 객체에 POST 요청한다.

파드의 포트 정의는 이름을 가지며, 서비스의  `targetPort` 속성에서 해당 이름을 참조할 수 있다. 이는 서비스에 구성된 단일 이름을 사용하는 파드가 혼합되어 있고 다른 포트 번호를 통해 동일한 네트워크 프로토콜을 사용할 수 있는 경우에도 작동한다. 이는 파드의 포트번호를 변경한 후 다음 버전을 업데이트 하는 경우에도 해당 서비스를 사용하는 외부 애플리케이션에 영향이 없기 때문에 서비스를 배포하고 발전시킬 수 있는 많은 유연성을 제공한다. 

서비스의 기본 프로토콜은 TCP이며 다른 프로토콜을 사용할 수도 있다.



### 셀렉터 없는 서비스

서비스는 대부분은 파드에 대한 접근을 추상화하지만, 다른 종류의 백엔드를 추상하는 경우도 있다. 예를 들면 아래와 같다.

- 워크로드를 쿠버네티스로 전환하려는 경우. 해당 방식을 평가하는 동안 백엔드의 한 부분을 쿠버네티스로 실행하려는 경우

- 하나의 서비스에서 다른 네임스페이스 또는 다른 클러스터로의 서비스를 지정하려는 경우
- 프로덕션 레벨에서 외부 데이터베이스 클러스터를 사용하려고 하지만 테스트환경에서 자체 데이터베이스를 사용하는 경우

다음 예시는 파드 셀렉터 없이 서비스를 정의하려는 예시이다. 서비스에 셀렉터가 없기 때문에 관련된 엔드포인트 객체는 자동으로 생성되지 않는다. 서비스와 네트워크 주소, 포트를 수동으로 매핑할 수도 있다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
```

```yaml
apiVersion: v1
kind: Endpoints
metadata:
  name: my-service
subsets:
  - addresses:
      - ip: 192.0.2.42
    ports:
      - port: 9376
```

엔드포인트 객체의 이름은 [DNS subdomain name](https://kubernetes.io/docs/concepts/overview/working-with-objects/names#dns-subdomain-names) 을 따라야 한다.

셀렉터가 없는 서비스를 접근하는 것은 셀렉터가 있는 서비스에 접근하는 방식과 같다. ExternalName 서비스는 셀렉터를 가지지 않는 대신 DNS 이름은 사용하는 서비스의 한 예시이다. 



### Over Capacity Endpoints

엔드포인트 리소스가 1000개보다 많으면 K8s V1.22 부터에서 클러스터는 엔드포인트에 `endpoints.kubernetes.io/over-capacity: truncated` 라는 내용으로 애노테이션을 추가한다. 이 애노테이션은 영향을 받은 엔드포인트 객체는 용량이 초과되었다는 것을 알리고 해당 엔드포인트 컨트롤러는 엔드포인트 수를 1000개로 줄인다.



## 가상 IP, Service Proxy

클러스터 내 모든 노드들은 `kube-proxy` 를 구동한다. `kube-proxy` 는 ExternalName이 아닌 서비스 타입의 가상 IP 형식을 구현하는 책임을 가진다.



### Round-robin DNS를 사용하지 않는 이유

K8s는 inbound traffic을 백엔드에 전달하기 위해 프록시를 의존하는 이유가 가끔씩 제기된다. 서비스에 프록시를 사용하는 몇 가지 이유가 있다.

- 레코드 TTL을 고려하지 않고 만료된 이름 검색 결과를 캐싱하는 DNS 구현에 대한 역사가 있다.
- 일부 앱은 DNS 조회를 한 번만 수행하고 결과를 무한히 캐싱한다. (캐싱 만료시간 X)
- 애플리케이션과 라이브러리가 적절히 재확인을 한다 하더라도 DNS의 레코드의 TTL이 낮거나 0이면 DNS에 부하가 높아 관리하기가 어려워질 수 있다.



### 설정

kube-proxy가 다른 모드로 시작할 수 있다. kube-proxy의 모드는 설정에 의해 결정된다.

- kube-proxy의 설정이 ConfigMap을 통해 이루어진다. 그리고 kube-proxy를 위한 ConfigMap은 kube-proxy에 대한 거의 모든 플래그에 대한 동작을 효과적으로 사용하지 않는다.
- kube-proxy의 ConfigMap은 설정의 live reloading을 지원하지 않는다. (실행 도중 변경해도 변하지 않는다는 뜻인듯)
- 시작 시 kube-proxy에 대한 ConfigMap 파라미터를 모두 검증하거나 확인할 수 없다.



### User space proxy mode

해당(레거시) 모드에서, `kube-proxy`는 서비스와 엔드포인트 객체가 추가되는지 삭제되는지 컨트롤 플레인을 지켜본다. 각 서비스에 대해 로컬 모드에서 포트를 연다. 해당 "proxy port"에 대한 모든 연결은 서비스의 백엔드 포트 중 하나에 프록시된다. kube-proxy는 사용할 백엔드 파드를 결정할 때 서비스의 `SessionAffinity` 설정을 고려한다.

마지막으로, `user-space proxy` 는 서비스의 `ClusterIP`와 `Port`로 향하는 트래픽을 캡쳐하는 iptable rule을 설치한다. 이 rule은 해당 트래픽을 백엔드 파드로 프록시하는 proxy port로 리다이렉트한다.

기본적으로 userspace mode 내 kube-proxy는 Round-robin 알고리즘을 사용하여 백엔드를 선택한다.

![유저스페이스 프록시에 대한 서비스 개요 다이어그램](https://d33wubrfki0l68.cloudfront.net/e351b830334b8622a700a8da6568cb081c464a9b/13020/images/docs/services-userspace-overview.svg)

쿠버네티스 서비스에 대한 iptable 리스트를 보는 방법은 아래 명령어를 사용하면 된다.

```shell
$ sudo iptables -t nat -L KUBE-SERVICES -n  | column -t
```



### iptables proxy mode

해당 모드에서, kube-proxy는 서비스와 엔드포인트 객체가 추가되는지 삭제되는지 확인하기 위해 컨트롤 플레인을 지켜본다. 각 서비스에 대해 서비스의 `ClusterIP`와 `Port`로 향하는 트래픽을 캡쳐하는 iptable rule을 설치한다. 그리고 그 트래픽을 서비스의 백엔드 셋 중 하나로 리다이렉트한다. 각 엔드포인트 객체에 대해 백엔드 파드를 선택하는 iptable rule을 설치한다. 

기본적으로 iptables mode에서 kube-proxy는 무작위로 백엔드를 선택한다.

userspace와 kernel space 사이를 스위칭 할 필요 없이 리눅스 netfilter에 의해 트래픽이 다뤄지기 때문에 트래픽을 다루기 위해 iptable을 사용하는 것은 시스템 오버헤드가 적다. 이러한 접근법은 더 신뢰성이 있을 수도 있다. 

kube-proxy가 iptable mode에서 구동 중이고 선택된 첫 번째 파드가 응답하지 않는 경우에는 연결이 실패한다. 이는 `userspace mode` 와는 다르다. 해당 시나리오에서, kube-proxy는 첫 번째 파드로 향하는 연결이 실패했다는 것을 감지하고 자동으로 다른 백엔드 파드로 연결한다. 

백엔드 파드가 잘 실행 중인지 확인하기 위해 파드 `readiness probe`를 사용할 수 있다. 그래서 iptable mode에서 `kube-proxy`는 정상적으로 테스트된 백엔드만 볼 수 있다. 이렇게 하는 것은 트래픽이 kube-proxy를 통해 실패한 것으로 알려진 파드로 전송되는 것을 막을 수 있다.

![iptables 프록시에 대한 서비스 개요 다이어그램](https://d33wubrfki0l68.cloudfront.net/27b2978647a8d7bdc2a96b213f0c0d3242ef9ce0/e8c9b/images/docs/services-iptables-overview.svg)



### IPVS proxy mode

ipvs 모드에서, `kube-proxy`는 서비스와 엔드포인트를 지켜보고, IPVS 룰을 적절히 생성하는 `netlink` 인터페이스를 호출하고 서비스와 엔드포인트와 함께 IPVS 룰을 주기적으로 동기화한다. 이러한 컨트롤 루프는 IPVS 상태가 원하는 상태로 맞춰지기를 보장한다. 서비스를 접근할 때, IPVS는 트래픽을 백엔드 파드 중 하나로 보낸다.

IPVS 프록시 모드는 iptable mode와 유사한 netfilter hook function 기반이지만 해시 테이블을 기본 데이터 구조로 사용하여 커널 공간에서 작동한다. 이는 IPVS 모드에서 kube-proxy는 iptable mode보다 적은 latency로 트래픽을 리다이렉트하고 proxy rule을 동기화할 때 성능이 훨씬 향상된다. 다른 proxy mode와 비교했을 때 IPVS mode는 더 높은 네트워크 트래픽 성능을 제공한다.

IPVS는 백엔드 파드로 트래픽을 조정하기위한 옵션을 제공한다.

- `rr`: round-robin
- `lc`: least connection
- `dh`: destination hashing
- `sh`: source hashing
- `sed`: shortest expected delay
- `nq`: never queue



![image-20211011212558655](file:///Users/yunseowon/Library/Application%20Support/typora-user-images/image-20211011212558655.png?lastModify=1633961759)



해당 proxy model에서, 서비스의 IP:port로 향하는 트래픽은 클라이언트가 쿠버네티스, 서비스, 파드에 대해 전혀 알지 못하는 상태에서 적절한 백엔드로 프록시된다.

특정 클라이언트로의 요청이 매번 동일한 파드로 전달되는지 확인하려는 경우, `service.spec.sessionAffinity`를 **ClientIP** *(default: None)*로 설정하여 클라이언트 IP 주소를 기반으로 Session Affinity를 선택할 수 있다. 또한, `service.spec.sessionAffinityConfig.clientIP.timeoutSeconds` 을 적절히 설정함으로써 최대 세션 고정 시간을 설정할 수 있다.



### Multi-Port Services

일부 서비스는 하나 이상의 포트를 노출시켜야 할 수도 있다. K8s는 서비스 객체 정의에 여러 포트를 구성할 수 있도록 해준다. 서비스에 여러 포트를 사용하는 경우, 모든 포트 이름이 명확하도록 포트 이름을 지정해야 한다. 

아래가 그 예시이다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 9376
    - name: https
      protocol: TCP
      port: 443
      targetPort: 9377
```



### 자신의 IP 주소 선택

서비스 생성 요청 시 고유한 Cluster IP 주소를 지정할 수 있다. 이를 위해서는 `.spec.clusterIP` 필드를 설정하면 된다. 선택한 IP 주소는 API Server로 구성된  `service-cluster-ip-range` CIDR 범위 내의 IPv4(IPv6) 형태를 만족해야 한다. 서비스를 유효하지 않은 ClusterIP 주소 값으로 설정하여 생성하려는 경우, API 서버는 문제가 있음을 알리는 상태값 422를 반환한다.





## Traffic Policies

<일단 생략>



## 서비스 디스커버리

<일단 생략>



## Headless Services

가끔 로드밸런싱과 단일 서비스 IP가 필요없는 경우도 있다. 이 경우에는, `.spec.clsterIP` 속성을 `None`으로 명시함으로써 **headless** 서비스라고 불리는 서비스를 생성할 수 있다.  headless 서비스를 K8s 구현체에 얽메이지 않고 다른 서비스 디스커버리 메커니즘과 함께 인터페이스로 사용할 수 있다.

Headless 서비스에 대해, cluster IP는 할당되지 않고 kube-proxy는 해당 서비스를 다루지 않는다. 그리고 플랫폼에서 수행하는 로드밸런싱이나 프록시가 없다. DNS 자동 구성 방법은 서비스에 셀렉터가 정의되어있는지 여부에 따라 달라진다.



### 셀렉터가 있을 때

셀렉터가 정의되어있는 headless 서비스에 대해, 엔드포인트 컨트롤러는 API 내부에서 `Endpoint` 레코드를 생성하고 서비스를 지원하는 파드를 직접 가리키는 A 레코드*(IP address)*를 반환하기 위한 DNS 설정을 변경한다.



### 셀렉터가 없을 때

셀렉터를 지정하지 않은 headless 서비스에 대해, 엔드포인트 컨트롤러는 `Endpoint` 레코드를 생성하지 않는다. 그러나 DNS 시스템은 다음 중 하나를 찾고 구성한다.

* ExternalName 타입 서비스에 대한 CNAME 레코드
* 서비스와 이름을 공유하는 모든 `Endpoint` 에 대한 레코드



## 서비스 퍼블리싱 (ServiceTypes)

애플리케이션 일부분에 대해서는 서비스를 클러스터 외부로 노출시켜야 할 수 있다. 쿠버네티스 `ServiceTypes` 은 원하는 서비스 종류를 지정하는 것을 허용한다. *(default: ClusterIP)* 타입의 종류는 아래와 같다.

* **ClusterIP** *(default)*: 서비스를 클러스터 내부 IP로 노출시킨다. 이 값을 선택하는 것은 오직 클러스터 내부로만 접근 가능하도록 하기 위해서이다.
* **NodePort**: 서비스를 각 노드의 IP에 정적 포트로 노출시킨다. NodePort 서비스가 라우팅하는 ClusterIP 서비스가 자동으로 생성된다. 이 서비스는 `<NodeIP>:<NodePort>` 로 요청함으로써 클러스터 외부로부터 서비스와 통신할 수 있다.
* **LoadBalancer**: 클라우드 제공자의 로드 밸런서를 사용하여 서비스를 외부에 노출시킨다. 외부 로드밸런서가 라우팅되는 `NodePort`와 `ClusterIP` 서비스가 자동으로 생성된다.
* **ExternalName**: `externalName` 값과 함께 `CNAME` 레코드를 반환함으로써 서비스와 `externalName` 필드 컨텐츠를 매핑한다. 어떠한 프록시도 설정되지 않는다.



또한 `Ingress`를 사용하여 서비스를 외부로 노출시킬 수 있다. Ingress는 서비스 타입은 아니지만 클러스터의 엔트리 포인트로써 역할을 한다. 여러 서비스를 동일한 IP 주소로 노출시킬 수 있으므로 라우팅 규칙을 단일 리소스로 통합할 수 있다.



### Type NodePort

`type` 필드를 `NodePort`로 설정한 경우, 쿠버네티스 컨트롤 플레인은 `--service-node-port-range` 플래그 *(default=30000-32767)* 에 지정된 범위 내 포트번호를 할당한다. 각 노드는 해당 포트를 서비스에 프록시한다. 해당 서비스는 할당된 포트를 `.spec.ports[*].nodePort` 필드에 기록한다.

포트를 프록시할 특정 IP를 지정하려면 kube-proxy에 대한 `--nodeport-addresses` 플래그 또는 kube-proxy 구성 파일의 `nodePortAddresses` 필드를 특정 IP 블록으로 설정할 수 있다. 이 플래그는 kube-proxy가 이 노드에 대해 로컬로 간주해야 하는 IP주소 범위를 지정하기 위해 `,`로 구분된 IP 블록 리스트를 사용한다. *(default : empty list)* empty list인 경우는 kube-proxy가 모든 사용 가능한 네트워크 인터페이스를 NodePort로 간주해야한다는 것을 의미한다.

특정 포트번호를 지정하길 원한다면 `nodePort` 필드에 값을 지정하면 된다. 컨트롤 플레인은 해당 포트번호를 할당하거나 할당이 실패하면 API 트랜잭션이 실패함을 기록할 것이다. 포트 번호를 명시적으로 지정하려면 유효한 포트번호를 사용해야 한다. (사용 중이지 않은 포트, NodePort가 사용할 범위 내 포트번호 - `--service-node-port-range`)

NodePort를 사용하면 자신만의 로드밸런싱 솔루션을 설정하거나 k8s에서 완전히 지원하지 않는 환경을 구성하거나 하나 이상의 노드의 IP를 직접 노출시킬 수 있다.

서비스는 `<NodeIP>:spec.ports[*].nodePort` 와  `spec.clusterIP:spec.ports[*].port` 로 보일 수 있다. kube-proxy의 `--nodeport-addresses` 플래그나 kube-proxy 설정의 필드가 설정되어있다면 `<NodeIP>` 는 node IP로 필터링될 것이다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  type: NodePort
  selector:
    app: MyApp
  ports:
      # By default and for convenience, the `targetPort` is set to the same value as the `port` field.
    - port: 80
      targetPort: 80
      # Optional field
      # By default and for convenience, the Kubernetes control plane will allocate a port from a range (default: 30000-32767)
      nodePort: 30007

```



### Type LoadBalancer

외부 로드밸런서를 지원하는 클라우드 프로바이더에서, `type` 필드를 `LoadBalancer`로 설정하는 것은 서비스에 로드밸런서를 제공하는 것이다. 로드밸런서의 실제 생성은 비동기적으로 발생하고 제공된 밸런서에 관한 정보는 서비스의 `.status.loadBalancer` 필드에 퍼블리싱된다. 

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: MyApp
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
  clusterIP: 10.0.171.239
  type: LoadBalancer
status:
  loadBalancer:
    ingress:
    - ip: 192.0.2.127
```

외부 로드밸런서로의 트래픽은 백엔드 파드로 향한다. 클라우드 제공자는 로드밸런싱 방법을 결정한다.

일부 클라우드 제공자는 `loadBalancerIP`를 지정하는 것을 허용한다. 이 경우에 대해서, 로드밸런서는 사용자 지정 `loadBalancerIP` 와 함께 생성된다. `loadBalancerIP` 필드가 지정되지 않는다면, 로드밸런서는 하나의 IP 주소로 설정된다. `loadBalancerIP`를 지정하지만 클라우드 제공자가 로드밸런서 기능을 지원하지 않는다면, 설정한 `loadBalancerIP` 는 무시된다.









































