# Ingress

Ingress는 HTTP와 HTTPS 라우트를 클러스터 외부에서 클러스터 내 서비스로 노출시킨다. 인그레스 리소스에서 정의된 룰에 의해 트래픽 라우팅이 제어된다.

인그레스는 서비스에게 외부로 도달 가능한 URL을 주고 트래픽을 로드밸런싱하고 SSL / TLS를 종료시키고, 이름 기반의 가상 호스팅을 제공하도록 구성될 수 있다. `Ingress Controller` 는 일반적으로 로드밸런서를 사용하여 인그레스를 수행하는 역할을 하지만, 트래픽을 처리하도록 엣지 라우터나 추가 프런트엔드를 구성할 수도 있다.

인그레스는 임의의 포트나 프로토콜을 노출시키지 않는다. HTTP / HTTP 이외의 서비스를 인터넷에 노출시키면 일반적으로 `Service.type=NodePort` 또는 `Service.Type=LoadBalancer` 타입의 서비스를 사용한다.



## 용어

* Node: K8s의 워커 머신으로 클러스터의 일부분이다.
* Cluster: K8s에 의해 관리되는 컨테이너 기반 애플리케이션을 구동시키는 Node의 집합.
* Edge router: 클러스터의 방화벽 정책을 시행하는 라우터. 이는 클라우드 제공자 또는 하드웨어의 물리적 부분에 의해 관리되는 게이트웨이일 수 있다.
* Cluster network: K8s 네트워킹 모델에 따라 클러스터 내에서 통신을 용이하게 하는 물리적/논리적인 링크 집합. 
* Service: 라벨 셀렉터를 사용하는 파드집을 식별하는 K8s 서비스. 달리 언급하지 않는 한, 서비스는 클러스터 네트워크 내에서만 라우팅 가능한 가상 IP를 가지고 있다고 가정한다.



## Prerequisites

인그레스를 만족시키기 위래서는 **Ingress Controller** 를 가지고 있어야 한다. 오직 인그레스만을 생성하는 것은 효과가 없다. `ingress-nginx` 와 같은 인그레스 컨트롤러를 배포해야 할 것이다. 또한 다양한 인그레스 컨트롤러를 선택할 수 있다. 이상적으로, 모든 인그레스 컨트롤러는 참조 사양에 적합해야 한다. 실제로, 다양한 인그레스 컨트롤러는 약간 다르게 운영한다.



## 인그레스 리소스

최소한의 인그레스 리소스의 예시는 아래와 같다.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: minimal-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - http:
      paths:
      - path: /testpath
        pathType: Prefix
        backend:
          service:
            name: test
            port:
              number: 80
```

다른 모든 쿠버네티스 리소스와 같이, 인그레스는 `apiVersion`, `kind`, `metadata` 필드가 필요하다. 인그레스 객체의 이름은 유효한 [DNS subdomain name](https://kubernetes.io/docs/concepts/overview/working-with-objects/names#dns-subdomain-names) 이어야 한다. 인그레스는 인그레스 컨트롤러에 의존하는 일부 옵션을 구성하기 위해 주기적으로 애노테이션을 사용한다. 인그레스 컨트롤러 종류마다 다른 애노테이션을 지원한다. 인그레스 컨트롤러를 선택할 때 어떤 애노테이션이 지원되는지 검토해야 할 것이다.

인그레스 [스펙](https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#spec-and-status)은 로드밸런서나 프록시 서버를 구성하기 위해 필요한 모든 정보를 가지고 있다. 가장 중요한 것은, 모든 들어오는 요청에 대해 일치하는 규칙 목록이 포함되어있다. 인그레스 자원은 오직 HTTP(S) 트래픽을 전달하기 위한 규칙만 지원한다.



### 인그레스 규칙

각 HTTP 룰은 다음 정보를 포함한다.

* 선택적인 호스트. 위의 예시에서는 호스트가 지정되지 않았기 때문에 규칙이 지정된 IP 주소를 통해 들어오는 모든 인바운드 HTTP 트래픽에 적용된다. 규칙은 해당 호스트에 적용된다.
* 경로 목록. 각각의 경로는 `service.name` 이나 `service.port.name` 이나 `service.port.number`가 정의된 관련된 백엔드를 가지고있다. 로드밸런서가 트래픽을 참조 서비스로 전달하기 전에 호스트와 경로가 모두 수신 요청의 내용과 일치해야 한다.
* 백엔드는 CRD를 통해 서비스 문서나 사용자 지정 리소스 백엔드에 설명된 서비스 및 포트 이름의 조합이다.

`defaultBackend`는 규격의 경로와 일치하지 않는 요청을 처리하도록 인그레스 컨트롤러 내에서 보통 구성된다.



### DefaultBackend

아무 규칙도 없는 인그레스는 모든 트래픽을 하나의 기본 백엔드로 전달한다. `defaultBackend`는 일반적으로 인그레스 컨트롤러의 구성 옵션이고 인그레스 자원에서 지정되지 않는다. 인그레스 객체 내에서의 HTTP 요청이 어느 호스트나 경로가 일치하지 않는다면 트래픽은 기본 백엔드로 라우트된다.



### Resource Backends

`Resource` 백엔드는 인그레스 객체와 동일한 네임스페이스 내에 있는 다른 쿠버네티스 리소스에 대한 `ObjectRef` 이다. `Resource`는 `Srvice`와 상호 배제적인 설정이며, 둘 다 지정되어있으면 실패할 것이다. `Resource` 백엔드에 대한 일반적인 사용법은 정적 자산이 있는 객체 스토리지 백엔드에 데이터를 수신하는 것이다.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-resource-backend
spec:
  defaultBackend:
    resource:
      apiGroup: k8s.example.com
      kind: StorageBucket
      name: static-assets
  rules:
    - http:
        paths:
          - path: /icons
            pathType: ImplementationSpecific
            backend:
              resource:
                apiGroup: k8s.example.com
                kind: StorageBucket
                name: icon-assets
```



위의 명세로 인그레스를 생성한 후 인그레스 정보를 확인할 수 있다.

```shell
$ kubectl describe ingress ingress-resource-backend

Name:             ingress-resource-backend
Namespace:        default
Address:
Default backend:  APIGroup: k8s.example.com, Kind: StorageBucket, Name: static-assets
Rules:
  Host        Path  Backends
  ----        ----  --------
  *
              /icons   APIGroup: k8s.example.com, Kind: StorageBucket, Name: icon-assets
Annotations:  <none>
Events:       <none>
```



### Path Types

인그레스에서 각 경로는 해당 경로 유형을 가져야 한다. 명시적인 `pathType`을 포함하지 않는 경로는 검증이 실패할 것이다. Path Types에는 3가지의 유형을 지원한다.

* **ImplementationSpecific**: 이 경로 유형에 대한 일치 여부는 `IngressClass`에 달려있다. 구현에서는 이를 별도 `pathType` 으로 처리하거나 `Prefix` 또는 `Exact` 경로 유형과 같이 동일하게 처리할 수 있다.
* **Exact**: URL 경로와 대/소문자를 정확히 일치시킨다.
* **Prefix**: `/` 로 분할된 URL 경로 접두사를 기반으로 일치시킨다. 일치 여부는 대소문자를 구분하며 요소별로 경로 요소에 대해 수행된다. 경로 요소는 `/` separator로 분할된 경로 내 라벨의 목록으로 불린다. 모든 *p* 가 요청 경로의 요소별 접두사가 *p*인 경우 요청은 *p* 경로에 일치한다.



### Multiple matches

경우에 따라 인그레스 내부에서 여러개의 경로가 요청과 일치할 때가 있다. 이러한 경우는 가장 긴 일치 경로에 우선순위가 부여된다. 여전히 두 개의 경로가 일치할 때, **Prefix** 보다는 **Exact** 가 우선이다.



## Hostname Wildcards

호스트는 Precise matches(`foo.bar.com`) 이거나 wildcard 형태(`*.foo.com`)일 수 있다. Precise matches는 HTTP `host` 헤더와 `host` 필드와 일치해야 한다. wildcard는 HTTP `host` 헤더가 와일드카드 룰의 접미사아 일치해야 한다.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ingress-wildcard-host
spec:
  rules:
  - host: "foo.bar.com"
    http:
      paths:
      - pathType: Prefix
        path: "/bar"
        backend:
          service:
            name: service1
            port:
              number: 80
  - host: "*.foo.com"
    http:
      paths:
      - pathType: Prefix
        path: "/foo"
        backend:
          service:
            name: service2
            port:
              number: 80
```



## Ingress Class

인그레스는 다양한 설정으로 여러 다양한 컨트롤러로 구현될 수 있다. 각 인그레스는 클래스를 구현해야 하는 컨트롤러의 이름을 포함하여 추가 구성이 포함된 IngressClass 리소스에 대한 참조인 클래스를 지정해야 한다. 

```yaml
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: external-lb
spec:
  controller: example.com/ingress-controller
  parameters:
    apiGroup: k8s.example.com
    kind: IngressParameters
    name: external-lb
```

IngressClass 리소스는 옵션 파라미터 필드를 포함한다. 이 파라미터는 해당 클래스에 대한 추가 구현을 명세하는 설정을 참조하는 데 사용할 수 있다.



## Types of Ingress



### 단일 서비스로 지원되는 인그레스

이는 단일 서비스를 노출시키는 것을 허용하는 기존 쿠버네티스 개념이다. 아무 규칙없이 default backend를 지정함으로써 인그레스에서 이 작업을 수행할 수 있다.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: test-ingress
spec:
  defaultBackend:
    service:
      name: test
      port:
        number: 80
```



위의 명세를 이용하여 인그레스를 생성했다면(`kubectl apply -f`) 생성한 인그레스의 상태를 확인할 수 있다.

```shell
$ kubectl get ingress test-ingress
NAME           CLASS         HOSTS   ADDRESS         PORTS   AGE
test-ingress   external-lb   *       203.0.113.123   80      59s
```

 `203.0.113.123` 은 해당 인그레스를 만족시키기 위해 인그레스 컨트롤러가 할당한 IP이다.









