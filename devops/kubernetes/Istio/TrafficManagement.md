# Traffic Management

Istio의 트래픽 라우팅 룰은 서비스 간 트래픽과 API 호출 흐름을 쉽게 제어할 수 있게 한다. istio는 서킷 브레이터, 타임아웃, retry와 같은 서비스 레벨의 프로퍼티의 구성을 간단하게 하고 A/B 테스팅, canary rollout, 비율 기반 트래픽 분할로 단계적 롤아웃과 같은 중요한 작업을 쉽게 설정할 수 있다. 또한, 종속 서비스나 네트워크의 오류에 대해 애플리케이션을 보다 강력하게 만드는 데 도움이 되는 out-of-box failure recovery 기능을 제공한다.

istio의 트래픽 관리 모델은 서비스와 함께 배포되는 Envoy 프록시에 의존한다. 메쉬 서비스에서 주고받은 모든 트래픽은 Envoy를 통해 프록시되고, 서비스를 변경하지 않고도 메쉬 주변의 트래픽을 쉽게 전달하고 제어할 수 있다.



## 개요

메쉬에서의 트래픽을 전달하기 위해 istio는 모든 엔드포인트가 어디에 있는지, 어떤 서비스에 속하는지 알아야 한다. 자체 서비스 레지스트리를 채우기 위해 istio는 서비스 디스커버리 시스템에 연결한다. K8s에서 istio를 설치했을 경우, istio는 자동으로 클러스터 내 서비스와 엔드포인트를 감지한다.

서비스 레지스트리를 사용한다면 Envoy 프롯기는 트래픽을 관련된 서비스에 전달할 수 있다. 대부분의 마이크로서비스 기반 애플리케이션은 서비스 트래픽을 처리하기 위해 각 서비스 워크로드의 여러 인스턴스를 가진다. 이를 로드 밸런싱 풀이라고도 부른다. 기본적으로, Envoy 프록시는 라운드 로빈 모델을 사용하여 각 서비스 로드밸런싱 풀로 트래픽을 분산시킨다. 여기서 요청은 각 풀 멤버에게 차례대로 전송되며 각 서비스 인스턴스가 요청을 받으면 풀의 맨 위로 돌아간다.

istio의 기본 서비스 디스커버리와 로드밸런싱은 워킹 서비스 메쉬를 제공하지만, istio가 할 수 있는 모든 것과는 거리가 멀다. 많은 경우에 메쉬 트래픽에 발생하는 상황을 보다 세밀하게 제어하고 싶을 수도 있다. 그리고 A/B 테스트의 목적으로 특정 비율의 트래픽을 새로운 버전의 서비스로 전달하고 싶을 수도 있다. 또는, 서비스 인스턴스의 특정 서브셋에 대한 트래픽에 다른 로드밸런싱 정책을 적용하고 싶을 수도 있다. 그리고 메쉬에 출입하는 트래픽에 특정 룰을 적용시키거나 서비스 레지스트리에 메쉬의 외부 종속성을 추가하고 싶을 수도 있다. istio의 트래픽 관리 API를 사용하여 istio에 트래픽 구성을 추가함으로써 이러한 것들을 포함하여 다양한 기능들을 수행할 수 있다.

다른 istio 구성과 마찬가지로 API는 YAML을 사용하여 구성할 수 있는 쿠버네티스 사용자 지정 리소스 정의를 사용하여 지정된다.



## Virtual Service

Virtual service는 destination rule과 함께 istio 트래픽 라우팅 기능의 핵심 빌딩 블록이다. Virtial service를 사용하면 istio 및 플랫폼에서 제공하는 기본 연결 및 디스커버리를 기반으로 요청이 istio 서비스 메쉬 내 서비스로 어떻게 라우팅되는지 구성할 수 있게 한다. 각 virtual service는 순서대로 평가되는 일련의 라우팅 룰을 포함한다. istio는 가상 서비스에 대한 각 주어진 요청을 메쉬 내 특정 실제 대상과 일치시킬 수 있다. 메쉬는 경우에 따라서 여러 가상 서비스가 필요할 수도 있고 가상서비스가 필요없을 수도 있다.



### Virtual Service 사용 이유

Virtual service는 istio의 트래픽 관리를 강력하고 유연하게 관리하는 역할을 수행한다. Virtual service는 클라이언트가 실제로 구현하는 대상 워크로드에서 요청을 보내는 장소를 강력히 디커플링함으로써 역할을 수행한다. Virtual service는 트래픽을 워크로드로 보내기 위한 여러가지 트래픽 라우팅룰을 지정하는 풍부한 방식을 제공한다.

Virtual service가 없다면 Envoy는 트래픽을 라운드로빈 방식으로 로드밸런싱한다. 워크로드에 대해 알고있는 정보로 이런 로드밸런싱 방식을 개선할 수 있다. 예를 들어, 일부 서비스는 다른 버전으로 돌아가는데, 이는 A/B 테스팅에서 유용하게 사용할 수 있으며, 여러 서비스 버전의 백분율을 기반으로 트래픽 라우팅을 하거나 내부 사용자에서 특정 인스턴스 집합으로 트래픽을 지정할 수도 있다.

Virtual service가 있다면, 하나 이상의 호스트 이름에 대한 트래픽 동작을 지정할 수 있다. virtual service의 트래픽을 적절한 대상으로 보내는 방법을 Envoy에 알려주는 라우팅 룰을  virtual service에서 사용한다. 라우팅 목적지는 같은 서비스나 전반적으로 다른 서비스의 버전이 될 수 있다.

일반적인 유즈케이스는 트래픽을 서비스 서브셋으로 지정된 서비스의 다른 버전으로 보내는 것이다. 클라이언트는 요청을 virtual service의 호스트로 보낸다. 그리고 Envoy는 virtual sercice의 룰을 따라서 여러가지 버전으로 트래픽을 라우팅한다. *(20%의 요청은 새로운 버전으로, 나머지 경우에서는 기존 버전으로..) 이러한 경우는 새 서비스 버전으로 전성되는 트래픽의 비율을 점진적으로 늘리는 방법을 사용하는 canary rollout을 만들 수 있다.* 

트래픽 라우팅은 인스턴스 배포로부터 완전히 분리된다. 이는 새로운 서비스 버전을 구현하는 인스턴스의 수는 트래픽 라우팅을 참조하지 않고 트래픽 부하에 따라 스케일 업/다운을 할 수 있다. 반면에, K8s와 같은 컨테이너 오케스트레이션 플랫폼은 인스턴스 스케일 기반으로만 트래픽 분배를 지원한다.

Virtual service는 또한 다음을 지원한다.

* 단일 가상 서비스를 통해 여러 애플리케이션 서비스를 처리한다. K8s를 사용하는 경우, 특정 네임스페이스 내 모든 서비스들을 처리하기 위해 가상 서비스를 구성할 수 있다. 하나의 가상 서비스를 여러개의 실제 서비스로 매핑하면 서비스 소비자가 전환에 적응할 필요 없이 단일 애플리케이션을 고유한 마이크로서비스로 구성된 복합 서비스로 쉽게 전환할 수 있다. 라우팅 룰은 `monolith.com` 을 호출하는 URI를 `microService A` 로 지정할 수 있다
* 게이트웨이와 함께 트래픽 룰을 구성하여 수신 및 송신 트래픽을 제어한다.

일부 경우에서, 이러한 기능을 사용하도록 대상 규칙을 구성해야 할 수도 있다. 여기서 서비스 하위 집합을 지정할 수 있다. 서비스 하위 집합과 여러가지 대상 지정 정책을 별도의 객체에 지정하면 가상 서비스 간에 이를 깔끔하게 재사용할 수 있다.



### Virtual Service 예시

아래 Virtual service는 요청을 보낸 사용자에 기반하여 요청을 여러 버전의 서비스로 라우팅한다.

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - match:
    - headers:
        end-user:
          exact: jason
    route:
    - destination:
        host: reviews
        subset: v2
  - route:
    - destination:
        host: reviews
        subset: v3
```



#### hosts 필드

hosts 필드는 virtual service의 호스트 목록을 나타낸다. 이 리스트는 라우팅 규칙이 적용되는 사용자 주소 지정 대상을 의미한다.

virtual service의 호스트네임은 IP 주소나 DNS 이름이나 플랫폼에 따라 암시적/명시적으로 FQDN*(Fully Qualified Domain Name)*으로 확인되는 이름이 될 수 있다. 와일드카드 `(*)` 를 사용하여 일치하는 모든 서비스에 대한 단일 라우팅 규칙 집합을 만들 수도 있다. Virtual service의 호스트는 사실상 istio 서비스 레지스트리의 일부가 되진 않는다. 이 호스트들은 단지 가상 목적지이다. 이는 메쉬 내부에 라우팅 가능한 항목이 없는 가상 호스트에 대한 트래픽을 모델링할 수 있다.



### Routing Rules

라우팅 룰은 트래픽이 전달되길 원하는 도착지와 하나 이상의 일치조건으로 구성되어있다.



#### Match Condition

특정 조건이 일치하는지에 대한 라우팅 규칙을 세울 수 있다. 다음 예시는 `header` 값 중 `end-user` 값이 `jason` 으로 일치하는지에 대한 라우팅 규칙을 정의한다.

```yaml
...
spec:
  ...
  - match:
    - headers:
        end-user:
          exact: jason
```



#### Destination

route 섹션의 `destination` 필드는 특정 조건에 일치하는 트래픽에 대한 실제 목적지를 지정한다. virtual service의 호스트와는 다르게 목적지의 호스트는 istio 서비스 레지스트리의 실제 목적지여야 한다. 그렇지 않으면 Envoy는 트래픽을 어디에 보내야할지 알지 못한다. 이는 프록시와 함께 메쉬 서비스이거나 서비스 엔트리를 사용하여 추가되는 메쉬가 아닌 서비스가 될 수 있다. 이 경우, K8s에서 실행 중이고 호스트 이름은 kubernetes 서비스가 될 수 있다. 

```yaml
...
spec:
  ...
  - match:
    ...
  	route:
		- destination:
    	host: reviews
    	subset: v2
```

해당 예시와 다른 예시에서, 단순성을 위해 kubernetes의 destination host에서 short name을 사용한다. 라우팅 룰이 평가된다면, istio가 라우팅 규칙을 포함하는 가상 서비스의 네임스페이스를 기반으로 도메인 접미사를 추가하여 호스트의 정규화된 이름을 가져온다. 

또한, `destination` 섹션은 이 규칙과 조건이 일치하는 요청이 이동할 K8s 서비스의 서브셋을 지정할 수 있다. 위의 예시에서 서브셋의 이름은 `v2` 이다.



#### Routing rule precedence

라우팅 룰은 위에서 아래로 순서대로 평가된다. 룰이 위에 위치해 있을수록 우선순위가 높아진다. 어느 라우팅 룰에도 일치하지 않는 요청에 대해서 기본 목적지를 지정할 수 있다. 

```yaml
...
spec:
  ...
  - match:
    ...
  - route:
		- destination:
    	host: reviews
    	subset: v3
```

* 서브셋 이름은 `.` 이 불가하다. (`subset name is invalid: v1.0`)

#### 기타 Routing Rules

라우팅 룰은 트래픽의 특정 서브셋을 특정 목적지로 라우팅하는 아주 강력한 도구이다. 트래픽을 포트, 헤더필드, URI 등을 이용하여 조건을 추가할 수도 있다. 

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: bookinfo
spec:
  hosts:
    - bookinfo.com
  http:
  - match:
    - uri:
        prefix: /reviews
    route:
    - destination:
        host: reviews
  - match:
    - uri:
        prefix: /ratings
    route:
    - destination:
        host: ratings
```



그리고 목적지의 weigth를 줘서 트래픽 분산 비율을 정할 수 있다.

```yaml
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 75
    - destination:
        host: reviews
        subset: v2
      weight: 25
```



이외에도 트래픽에 대한 특정 행동을 할 때 트래픽 라우팅 룰을 사용할 수 있다. ([HTTPRoute reference](https://istio.io/latest/docs/reference/config/networking/virtual-service/#HTTPRoute))

* 헤더를 추가하거나 삭제
* URL rewrite
* 목적지를 호출할 때 retry policy를 설정할 수 있다.



## Destination Rules

virtual service와 함께, destination rules은 istio의 트래픽 라우팅 기능의 핵심 부분이다. Virtual service를 트래픽을 주어진 목적지에 라우트하는 방법으로 생각할 수 있고, 해당 목적지에 대한 트래픽이 어떻게 되는지 구성하기 위해 destination rule을 사용할 수 있다. Destination rules은 virtual service의 라우팅 룰이 평가된 후 적용된다. 따라서, destination rules은 트래픽의 실제 목적지에 적용된다. 

특히, 모든 주어진 서비스의 인스턴스를 버전별로 그루핑하는 것과 같이, 이름을 가진 서비스 서브셋을 지정하기 위해 destination rule을 사용한다. 그리고 트래픽을 서비스의 여러 인스턴스의 트래픽을 제어하기 위해 virtual service의 라우팅 룰 내의 서비스 서브셋을 사용할 수 있다. 

또한, Destination rule을 사용하면 전체 대상 서비스 또는 선호하는 로드밸런싱 모델, TLS 보안 모드 또는 서킷 브레이커 설정과 같은 특정 서비스 하위 집합을 호출할 때 Envoy 트래픽 정책을 커스터마이징 할 수 있다. 



### 로드밸런싱 옵션

기본적으로 istio는 라운드로빈 로드밸런싱 정책을 사용한다. 또한, istio는 아래 모델을 지원하며 특정 서비스나 서비스 하위집합에 대한 요청을 위한 destination rule을 지정할 수 있다.

* Random: 요청이 풀 내의 인스턴스 중 무작위로 전달된다.
* Weighted: 요청이 인스턴스 별로 특정 확률로 전달된다.
* Least requests: 요청이 요청 수가 가정 적은 인스턴스로 전달된다.



### Destination rule 예시

아래 예시는 3개의 서브셋에 대해 각각 다른 로드밸런싱 정책을 구성하였다.

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: my-destination-rule
spec:
  host: my-svc
  trafficPolicy:
    loadBalancer:
      simple: RANDOM
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
    trafficPolicy:
      loadBalancer:
        simple: ROUND_ROBIN
  - name: v3
    labels:
      version: v3
```

각 서브셋은 하나 이상의 레이블 기반으로 정의되었다. 이 레이블은 K8s 서비스 배포에 메타데이터로 적용되어 다른 버전을 식별한다.

destination rule은 모든 서브셋에 대한 기본 트래픽 정책을 정의한다. subset 필드 위에서 정의된 기본 정책은 simple random 로드밸런서로 설정하였다. (`v1`, `v3` 에 실제로 적용된다.) 그리고 `v2` 에서는 라운드로빈 로드밸런서를 지정하였다.





## Gateway

메쉬에 출입하는 트래픽을 관리하기 위해 gateway를 사용한다. gateway를 사용하면 메쉬쪽으로 들어오거나 나갈 트래픽을 지정할 수 있다. 게이트웨이 구성은 서비스 워크로드와 함께 실행되는 사이드카 Envoy 프록시가 아닌 메쉬 엣지에서 실행되는 독립 실행형 Envoy 프록시에 적용된다. 

K8s ingress API와 같이 시스템으로 들어오는 트래픽을 관리하기 위한 다른 메커니즘과 다르게, Istio gateway를 사용하면 istio 트래픽 라우팅의 모든 강력하고 유연한 기능을 사용할 수 있다. Istio gateway 자원은 노출되는 포트, TLS 세팅 등과 같은 4-6 레이어 로드밸런싱 프로퍼티를 구성할 수 있기 때문에 istio gateway를 사용할 수 있다. 그리고 같은 API 리소스에 L7 애플리케이션 레이어 트래픽 라우팅을 추가하는 것 대신, 게이트웨이에 istio virtual service를 바인딩할 수 있다. 이는 기본적으로 istio 메쉬 내 여러 데이터 플레인의 트래픽과 같은 트래픽을 관리할 수 있다.

게이트웨이는 주로 인그레스 트래픽을 관리하기 위해 사용되지만 이그레스 게이트웨이를 구성할 수도 있다. 이그레스 게이트웨이를 사용하면 메쉬를 떠나는 트래픽에 대한 전용 출구 노드를 구성하거나 어떤 서비스가 외부 네트워크로 접근할 수 있는지 제한하거나 메쉬에 보안을 추가하여 이그레스 트래픽에 대한 보안 제어를 할 수 있다.

istio는 몇 가지 미리 구성된 게이트웨이 프록시 배포(`istio-ingressgateway`, `istio-egressgateway`)를 제공한다. 이러한 배포에 고유한 게이트웨이 구성을 적용하거나 고유한 게이트웨이 프록시를 배포 및 구성할 수 있다.



### Gateway Example

아래 예시는 외부 HTTPS 인그레스 트래픽에 대한 게이트웨이 구성을 보여준다.

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: ext-host-gwy
spec:
  selector:
    app: my-gateway-controller
  servers:
  - port:
      number: 443
      name: https
      protocol: HTTPS
    hosts:
    - ext-host.example.com
    tls:
      mode: SIMPLE
      credentialName: ext-host-cert
```

이 게이트웨이 구성은 `ext-host.example.com` 에서 443포트의 메시로 HTTPS 트래픽을 허용하지만 트래픽에 대한 라우팅을 지정하지 않는다. 라우팅을 지정하고 게이트웨이가 의도한대로 작동하도록 하려면 게이트웨이에 virtual service를 바인딩해야 한다. 이를 virtual service의 `gateways` 필드를 사용하여 바인딩할 수 있다.

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: virtual-svc
spec:
  hosts:
  - ext-host.example.com
  gateways:
  - ext-host-gwy
```

그러고 난 후 외부 트래픽에 대한 라우팅 룰과 함께 virtual service를 구성할 수 있다



## Service Entries

엔트리를 istio가 내부적으로 유지하는 서비스 레지스트리에 추가하기 위해 `service entry	` 를 사용한다. 서비스 엔트리를 추가한 후, Envoy 프록시는 마치 메쉬의 서비스인 것 처럼 트래픽을 서비스로 전송할 수 있다. 서비스 엔트리를 구성하면 메쉬 밖에서 구동 중인 서비스에 대한 트래픽을 관리할 수 있고, 아래 기능 또한 사용할 수 있다.

* 웹에서 사용되는 API 또는 레거시 인프라의 서비스에 대한 트래픽과 같은 외부 대산에 대한 트래픽을 리다이렉션 라고 전달한다.
* 외부 목적지에 대한 retry, timeout, fault injection 정책을 정의한다.
* 메쉬에 VM을 추가함으로써 VM에서의 메쉬 서비스를 구동한다.



### Service Entry Example

다음 예시는 mesh-external service 엔트리에 istio의 서비스 레지스트리에  `ext-svc.example.com` 외부 디펜던시를 추가한다. `hosts` 필드를 사용하여 외부 리소스를 지정한다. 호스트를 완전히 한정하거나 와일드카드 접두사 도메인 이름을 사용할 수도 있다.

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: svc-entry
spec:
  hosts:
  - ext-svc.example.com
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  location: MESH_EXTERNAL
  resolution: DNS
```

메쉬의 다른 서비스에 대한 트래픽을 구성하는 방법과 마찬가지로, 서비스 엔트리에 대한 트래픽을 보다 세부적으로 제어하도록 virtual service와 destination rule을 구성할 수 있다. 예를 들어, 아래 destination rule은 서비스 엔트리를 사용하여 구성한 `ext-svc.example.com` 의 외부 서비스에 대한 연결을 보호하기 위해 상호 TLS를 사용하도록 트래픽 경로를 구성한다.

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: ext-res-dr
spec:
  host: ext-svc.example.com
  trafficPolicy:
    tls:
      mode: MUTUAL
      clientCertificate: /etc/certs/myclientcert.pem
      privateKey: /etc/certs/client_private_key.pem
      caCertificates: /etc/certs/rootcacerts.pem
```





## Sidecars

기본적으로, istio는 관련된 워크로드의 모든 포트에 대한 트래픽을 받고 트래픽을 전달할 때 메쉬 내의 모든 워크로드에 닿기 위해 모든 Envoy proxy를 구성한다. Sidecar 구성을 사용하여 다음을 수행할 수 있다.

* Envoy 프록시가 허용하는 포트 및 프로토콜 집합을 Fine-tuning한다.
* Envoy 프록시가 도달하는 서비스의 집합을 제한한다.



메쉬의 다른 모든 서비스에 도달하도록 모든 프록시를 구성하면 높은 메모리 사용량으로 인해 메쉬 성능에 잠재적으로 영향을 미칠 수 있는 대규모 애플리케이션에서 이와 같이 사이드카 도달 가능성을 제한할 수 있다. 

사이드카 구성이 특정 네임스페이스의 모든 워크로드에 적용되도록 지정하거나 `workloadSelector` 를 사용하여 특정 워크로드를 선택할 수 있다. 

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Sidecar
metadata:
  name: default
  namespace: bookinfo
spec:
  egress:
  - hosts:
    - "./*"
    - "istio-system/*"
```









## EnvoyFilter

https://istio.io/v1.4/docs/reference/config/networking/envoy-filter/#EnvoyFilter-PatchContext

EnvoyFilter는 istio Pilot에 의해 생성되는 Envoy 구성을 커스터마이징하기 위한 메커니즘을 제공한다. EnvoyFilter를 사용하면 어떤 필드에 대한 값을 변경하거나 특정 필터를 추가하거나 새 리스너, 새 클러스터 등을 추가할 수 있다. 대신 부적절한 구성은 메쉬 전체적으로 불안정하게 될 수 있기 때문에 이러한 기능은 조심스럽게 사용해야 한다. 다른 istio 네트워킹 객체와는 다르게, EnvoyFilter는 추가로 적용된다. 특정 네임스페이스의 주어진 워크로드에 EnvoyFilter가 여러 개 존재할 수 있다. 이러한 EnvoyFilter 애플리케이션의 순서는 config root namespace 내의 모든 EnvoyFilter -> 워크로드 네임스페이스의 일치하는 모든 EnvoyFilter 순서이다.





### Spec

**spec.workloadSelector**

패치 구성을 적용해야하는 특정 파드/VM 세트를 선택하는 데 사용되는 기준. 대표적으로 라벨 셀렉터가 있다.



**spec.configPatches**

일치 조건이 있는 하나 이상의 패치



**configPatches[].applyTo**

Envoy 구성에서 지정된 패치를 적용해야 하는 위치를 지정한다.

* HTTP_FILTER: 기존 필터를 변경하거나 새로운 필터를 추가하기 위해 패치를 HTTP 커넥션 매니저의 HTTP filter chain에 적용한다. 
* HTTP_ROUTE: 라우트 구성 내 일치하는 가상 호스트 내부 라우트 객체에 패치를 적용한다. 현재는 MERGE 연산자만이 허용된다.
* CLUSTER: CDS 아웃풋 내 클러스터에 패치를 적용한다. 





































