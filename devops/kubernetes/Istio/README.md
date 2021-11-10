# Istio

https://istio.io/latest/about/service-mesh/



## Serice Mesh

현대 애플리케이션은 보통 마이크로서비스의 분산 컬렉션으로 설계되며 각 마이크로서비스는 각각 다른 비즈니스 기능을 수행한다. 서비스 메쉬는 애플리케이션에 추가할 수 있는 전용 인프라 계층이다. 서비스 메쉬는 코드를 추가하지 않고도 모니터링, 트래픽 관리 및 보안과 같은 기능을 투명하게 추가할 수 있다. 서비스 메쉬는 이 패턴을 구현하는 데 사용하는 소프트웨어 타입과 소프트웨어를 사용할 때 생성되는 보안 또는 네트워크 도메인 둘 다 설명한다.

쿠버네티스 기반 시스템과 같은 분산 서비스 배포의 규모와 복잡성이 증가함에 따라서, 시스템을 이해하고 관리하는 것이 점점 더 어려워질 것이다. 요구사항에는 디스커버리, 로드밸런싱, 장애 복구, 메트릭, 모니터링 등이 포함될 수 있다. 서비스 메쉬는 더 복잡한 운영 요구사항을 해결하는데 A/B testing, canary 배포, rate limiting, 접근 제어, 암호화, end-to-end 인증 등을 해결한다.

서비스 간 통신은 분산 응용 프로그램을 가능하게 한다. 애플리케이션 클러스터 내부와 전체에서 이러한 통신을 라우팅하는 것은 서비스 수가 증가함에 따라 더 복잡해질 것이다. istio는 이러한 복잡성을 줄여주면서 동시에 개발 팀의 부담을 덜어준다.



## Istio

Istio는 기존 분산 애플리케이션에 투명하게 계층화되는 오픈소스 서비스 메쉬이다. Istio의 강력한 기능은 서비스에 대한 보안, 연결, 모니터링을 통일성 있고 효율적인 방법을 제공한다. istio는 로드밸런싱, 서비스간의 인증, 모니터링을 위한 경로이다. (서비스 코드를 거의 변경하지 않고도 가능하다.) 

istio의 강력한 컨트롤 플레인은 중요한 기능을 제공하는데 기능은 아래와 같다.

* TLS 암호화와 강력한 identity 기반의 인증과 인가를 사용하여 클러스터 내 서비스 간 통신을 보호한다.
* HTTP, gRPC, WebSocket, TCP traffic에 대한 자동 로드밸런싱
* 풍부한 라우팅 룰, retry, failover, fault injection 등을 이용한 세밀한 트래픽 행동 제어
* 접근 제어, 속도 제한 및 할당량을 지원하는 플러그형 정책 레이어 및 구성 API
* 클러스터 ingress 및 egress를 포함한 클러스터 내 모든 트래픽에 대한 자동 메트릭, 로깅, 추적



istio는 확장성을 위해 설계되었고 다양한 범위의 배포 요구 사항을 처리할 수 있다. istio의 컨트롤 플레인은 쿠버네티스에서 구동한다 그리고 해당 클러스터에 배포된 애플리케이션을 메쉬에 추가하거나 메쉬를 다른 클러스터에 확장하거나 쿠버네티스 밖에서 구동 중인 VM이나 다른 엔드포인트에 연결할 수 있다.





## Architecture

istio는 데이터 플레인과 컨트롤 플레인으로 두 가지 컴포넌트가 있다.

데이터 플레인은 서비스들과 통신한다. 서비스 메쉬 없이, 네트워크는 전송되는 트래픽을 이해하지 못하며 어떤 유형의 트래픽인지 또는 누구에게서 오는지, 누구에게 보내는지에 따라 결정을 내릴 수 없다. 그리고 사이드카로써 배포된 Envoy Proxy들로 구성된다. 이 Envoy Proxy는 파드가 배포될 때 파드 내에 컨테이너로 자동 주입된다. (어떤 설정에 의하면) 그리고 해당 프록시는 마이크로서비스 간의 모든 네트워크 통신을 조정하고 제어한다. 또한 모든 메쉬 트래픽에 대한 분석을 수집하고 보고한다.

컨트롤 플레인은 원하는 구성과 서비스 뷰를 사용하고 프록시 서버를 동적으로 프로그래밍하여 규칙이나 환경이 변경되면 업데이트한다. 그리고 트래픽을 라우팅하도록 프록시를 관리한다.

![The overall architecture of an Istio-based application.](https://istio.io/latest/docs/ops/deployment/architecture/arch.svg)



### Envoy

Istio는 데이터 플레인을 구성할 때 Envoy proxy를 사용한다. Envoy는 서비스 메쉬 내 모든 서비스에 대한 인바운드 및 아웃바운드 트래픽을 중재하기 위해 C++로 개발된 고성능의 프록시이다. Envoy Proxy는 서비스에 대한 사이드카로 배포되어 Envoy의 많은 내장 기능으로 서비스를 논리적으로 보강한다.

* 동적 서비스 디스커버리
* 로드밸런싱
* TLS 종료
* HTTP/2, gRPC 프록시
* 서킷브레이커
* 헬스체크
* % 기반 트래픽 분산과 함께 staged rollout
* Falut Injection
* Rich metrics



사이드카 배포는 istio가 정책 결정을 시행하고 모니터링 시스템으로 보내 전체 메쉬의 동작에 대한 정보를 제공할 수 있는 풍부한 원격 분석을 추출할 수 있다. 사이드카 프록시 모델을 사용하면 코드를 재설계하거나 다시 작성할 필요 없이 기존 배포에 istio 기능을 추가할 수도 있다.

* 트래픽 컨트롤 기능 - 풍부한 HTTP, gRPC,WebSocket, TCP 트래픽 라우팅 룰 시행
* 네트워크 복원 기능 - 설정 재시도, 장애 조치, 서킷 브레이커, 오류 주입
* 보안 및 인증 기능 - 보안 정책과 접근제어 및 구성 API를 통해 정의된 rate limiting 시행
* 메쉬 트래픽에 대한 맞춤형 정책 시행 및 원격 측정 생성을 허용하는 WebAssembly 기반 플러그형 확장 모델



### istiod

istiod는 서비스 디스커버리와 구성 및 인증서 관리를 제공한다.

istiod는 트래픽 동작을 제어하는 고급 라우팅 규칙을 Envoy 전용 구성으로 변환하고 그 구성을 런타임 시점에서 사이드카로 전파한다. Pilot은 플랫폼별 서비스 디스커버리 메커니즘을 추상화하고 Envoy API를 준수하는 모든 사이드카를 사용할 수 있는 표준 형식으로 통합한다. 

istio는 쿠버네티스 또는 VM과 같은 여러 환경에 대한 검색을 지원할 수 있다. 





## 설치

먼저 istio를 다운로드 한다.

```shell
$ curl -L https://istio.io/downloadIstio | sh -
```

그리고 설치한 istio의 디렉토리로 들어가서 환경변수를 설정한다. 설치한 디렉토리 내에는 아래가 포함되어있다.

* sample application (`sample/`)
* Istioctl 클라이언트 바이너리 (`bin/`)

```shell
$ cd istio-1.11.4

$ export PATH=$PWD/bin:$PATH
# 또는
$ sudo mv /home/ubuntu/istio/istio-1.11.4/bin/istioctl /usr/local/bin
```



istio 설치를 위해 `demo configuration profile` 을 사용할 것이다. 해당 프로파일은 테스팅 목적으로 적절하지만 프로덕션용으로는 다른 프로파일을 사용하는 것이 좋다.

```shell
$ istioctl install --set profile=demo -y
✔ Istio core installed
✔ Istiod installed
✔ Egress gateways installed
✔ Ingress gateways installed
✔ Installation complete
```



설치가 완료되었다면 애플리케이션을 배포할 때 Envoy sidecar 프록시에 자동으로 주입될 수 있도록 istio에 지시하는 네임스페이스 라벨을 추가하자.

```shell
$ kubectl label namespace default istio-injection=enabled
```



## 튜토리얼

### Sample Application 배포

Sample application을 배포해보자.

```shell
$ kubectl apply -f samples/bookinfo/platform/kube/bookinfo.yaml
```

시간이 지나면 모든 파드가 뜬 것을 확인할 수 있다.

```shell
$ kubectl get po
NAME                              READY   STATUS    RESTARTS        AGE
details-v1-79f774bdb9-fb6vv       2/2     Running   0               60s
productpage-v1-6b746f74dc-d24wg   2/2     Running   0               60s
ratings-v1-b6994bb9-lfq48         2/2     Running   0               60s
reviews-v1-545db77b95-n6tkp       2/2     Running   0               60s
reviews-v2-7bf8c9648f-8bc95       2/2     Running   0               60s
reviews-v3-84779c7bbc-w2ps6       2/2     Running   0               60s
```





### 애플리케이션을 외부로 오픈

Sample application이 배포되었지만 외부로 노출은 되지 않았다. (ClusterIP) 외부에서 접근 가능하게 하기 위해 `Istio Ingress Gateway` 를 생성하자. 이는 메쉬 가장자리의 경로에 대한 라우트에 경로를 매핑한다.

```shell
$ kubectl apply -f samples/bookinfo/networking/bookinfo-gateway.yaml
```



게이트웨이를 배포하였으면 구성에 문제가 없는지 확인해보자. `istioctl analyze` 명령어로 확인이 가능하다.

```shell
$ istioctl analyze

✔ No validation issues found when analyzing namespace: default.
```





### Ingress IP, Port 결정

아래 명령어를 실행하여 쿠버네티스 클러스터가 외부 로드밸런서를 지원하는 환경에서 실행 중인지 확인하자.

```shell
$ kubectl get svc istio-ingressgateway -n istio-system
NAME                   TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)                                                                      AGE
istio-ingressgateway   LoadBalancer   10.109.57.77   <pending>     15021:30955/TCP,80:30881/TCP,443:30002/TCP,31400:30451/TCP,15443:31010/TCP   47m
```

`EXTERNAL_IP` 값이 설정되어있으면 인그레스 게이트웨이에서 사용할 수 있는 외부 로드밸런서가 있는 환경이다. `EXTERNAL_IP` 값이 `<none>` 이거나 `<pending>` 인 경우, 인그레스 게이트웨이에서 사용할 수 있는 외부 로드밸런서가 없는 환경이다. 외부 로드밸런서가 없는 환경에서는 게이트웨이를 `NodePort` 로 접근할 수 있다.







**Case.1 ) 외부 로드밸런서를 지원하는 경우**

인그레스 IP와 port를 환경변수에 설정하자.

```shell
$ export INGRESS_HOST=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
$ export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
$ export SECURE_INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="https")].port}')
```

**Case.2 ) 외부 로드밸런서를 지원하지 않는 경우**

인그레스 포트를 환경변수에 설정하자.

```shell
$ export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
$ export SECURE_INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}')
$ export INGRESS_HOST=$(kubectl get po -l istio=ingressgateway -n istio-system -o jsonpath='{.items[0].status.hostIP}')
```



그 후 `GATEWAY_URL` 을 환경변수에 설정하자.

```shell
$ export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```



그리고 아래 명령어에 나온 URL로 sample application과 통신해보자.

```shell
$ echo "http://$GATEWAY_URL/productpage"
```



## Dashboard

istio는 다양한 원격 측정 애플리케이션을 통합한다. 이는 서비스 메쉬의 구조를 이해하고, 메쉬의 토폴로지를 보여줄 수 있으며 메쉬의 상태를 분석할 수 있다.

`Prometheus`, `Grafana`, `Jaeger` 와 함게  `Kiali dashboard` 를 배포해보자.

1. Kiali와 여러 애드온을 설치하고 배포될 때 까지 기다리자.

   ```shell
   $ kubectl apply -f samples/addons
   $ kubectl rollout status deployment/kiali -n istio-system
   Waiting for deployment "kiali" rollout to finish: 0 of 1 updated replicas are available...
   deployment "kiali" successfully rolled out
   ```

   

2. 대시보드를 실행한다.

   ```shell
   $ istioctl dashboard kiali 
   ```

   * 외부 접속을 허용하려면 `--address=0.0.0.0` 을 추가한다.





## 샘플 삭제

```shell
$ kubectl delete -f samples/addons
$ istioctl manifest generate --set profile=demo | kubectl delete --ignore-not-found=true -f -
$ kubectl delete namespace istio-system
$ kubectl label namespace default istio-injection-
```











