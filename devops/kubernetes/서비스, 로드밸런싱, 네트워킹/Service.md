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
    - protocol: TCP
      port: 80
      targetPort: 9376
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
apiVersion: v1
kind: Endpoints
metadata:
  name: my-service
subsets:
  - addresses:
      - ip: 192.0.2.42
    ports:
      - port: 9376
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

![image-20211011203511490](file:///Users/yunseowon/Library/Application%20Support/typora-user-images/image-20211011203511490.png?lastModify=1633961759)



### iptables proxy mode

해당 모드에서, kube-proxy는 서비스와 엔드포인트 객체가 추가되는지 삭제되는지 확인하기 위해 컨트롤 플레인을 지켜본다. 각 서비스에 대해 서비스의 `ClusterIP`와 `Port`로 향하는 트래픽을 캡쳐하는 iptable rule을 설치한다. 그리고 그 트래픽을 서비스의 백엔드 셋 중 하나로 리다이렉트한다. 각 엔드포인트 객체에 대해 백엔드 파드를 선택하는 iptable rule을 설치한다. 

기본적으로 iptables mode에서 kube-proxy는 무작위로 백엔드를 선택한다.

userspace와 kernel space 사이를 스위칭 할 필요 없이 리눅스 netfilter에 의해 트래픽이 다뤄지기 때문에 트래픽을 다루기 위해 iptable을 사용하는 것은 시스템 오버헤드가 적다. 이러한 접근법은 더 신뢰성이 있을 수도 있다. 

kube-proxy가 iptable mode에서 구동 중이고 선택된 첫 번째 파드가 응답하지 않는 경우에는 연결이 실패한다. 이는 `userspace mode` 와는 다르다. 해당 시나리오에서, kube-proxy는 첫 번째 파드로 향하는 연결이 실패했다는 것을 감지하고 자동으로 다른 백엔드 파드로 연결한다. 

백엔드 파드가 잘 실행 중인지 확인하기 위해 파드 `readiness probe`를 사용할 수 있다. 그래서 iptable mode에서 `kube-proxy`는 정상적으로 테스트된 백엔드만 볼 수 있다. 이렇게 하는 것은 트래픽이 kube-proxy를 통해 실패한 것으로 알려진 파드로 전송되는 것을 막을 수 있다.

![image-20211011211510259](file:///Users/yunseowon/Library/Application%20Support/typora-user-images/image-20211011211510259.png?lastModify=1633961759)

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

















