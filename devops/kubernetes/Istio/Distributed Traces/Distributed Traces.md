# Distributed Traces

분산 추적은 메쉬를 통해 흐르는 개별 요청을 모니터링하여 동작을 모니터링하고 이해하는 방법을 제공한다. 추적을 통해 메쉬 운영자는 서비스 메쉬 내에서 서비스 종속성과 대기 시간의 원인을 이해할 수 있다.

istio는 Envoy proxy를 통해 분산 추적을 지원한다. 프록시는 프록시하는 애플리케이션을 대신하여 자동으로 추적 스팬을 생성하므로 애플리케이션이 적절한 요청 컨텍스트를 전달하기만 하면 된다.

istio는 `Zipkin`, `Jaeger`, `Lightstep`, `Datadog` 등 여러가지 추적 백엔드를 지원한다. 운영자는 추적 생성을 위해 sampling rate를 제어한다. 이는 운영자가 메쉬에서 생성되는 추적 데이터의 양과 속도를 제어할 수 있다는 것이다.



## Jaeger

Jaeger는 마이크로서비스 기반 분산 시스템을 모니터링하고 트러블슈팅하기 위해 사용된다. 기능으로는 아래와 같다.

* 분산 컨텍스트 전파
* 분산 트랜잭션 모니터링
* 원인 분석
* 서비스 디펜던시 분석
* 성능 / 레이턴시 최적화



### 기능

#### High Scalability

Jaeger 백엔드는 단일 실패점이 없고 비즈니스 요구 사항에 따라 확장할 수 있도록 설계되었다.



#### Native Support of OpenTracing

Jaeger 백엔드 Web UI, 라이브러리는 OpenTracing 표준을 지원하도록 설계되었다.

* span reference를 통해 trace를 DAG (Directed Acyclic Graphs)로 표현하였다.

* 강타입의 span tag와 structed logs를 지원한다.
* baggage를 통한 일반 분산 컨텍스트 전파 메커니즘을 지원한다.



#### Multiple Storage Backends

Jaeger는 NoSQL을 추적 스토리지 백엔드로 지원한다. (Cassandra 3.4+, Elasticsearch 5.x/6.x/7.x) 또한, 테스트를 위해 in-memory storage를 지원한다.



#### Modern Web UI

React와 같은 프레임워크를 사용하여 JS로 구현, UI가 대량의 데이터를 효율적으로 처리할 수 있도록 성능 개선을 하였고, 수만 가지의 span의 추적을 표시할 수 있도록 하였다.



#### Cloud Native Deployment

Jaeger는 도커 이미지의 컬렉션으로 분산된다. 이 바이너리들은 커멘드라인 옵션, 환경변수 등 여러 형식의 구성파일을 포함한 다양한 구성 방법을 지원한다.

K8s 클러스터에 대한 배포는 `Kubernetes oprator`, `Kubernetes templates`, `Helm chart` 의 지원을 받는다.



#### Observability

모든 Jaeger 백엔드 컴포넌트는 기본적으로 프로메테우스 메트릭을 노출한다. 로그는 structed logging 라이브러리인 zap을 사용하여 stdout으로 기록된다



#### Backwards Compatibility with Zipkin

Jaeger는 Zipkin의 포멧을 허용하여 Zipkin과의 하위 호환성을 제공한다.



#### Topology Graphs

Jaeger UI는 두 가지 타입의 서비스 그래프를 지원한다.



**System Architecture** 

System Architecture는 아키텍쳐 내에서 보여지는 모든 서비스에 대한 서비스 디펜던시 그래프이다. 이 그래프는 서비스 간의 one-hop 디펜던시만을 나타낸다. 예를 들어, `A - B - C` 로 나타낸 그래프는 `A` 와 `B` 사이는 의존이 있고, `B` 와 `C` 사이도 의존이 있지만 `A` 가 `C` 에 의존한다고 말할 수는 없다.

해당 그래프의 노드 세분성은 서비스 엔드포인트가 아닌 서비스에 대한 그래프이다.

System Architecture 그래프는 인메모리 스토리지에서 즉시 구축할 수 있다. 또는, 분산 스토리지를 사용할 때 Spark 또는 Flink 잡을 사용하여 구축할 수 있다.



**Deep Dependency Graph**

Deep Dependency Graph는 Transitive Dependency Graph로도 알려져 있으며, `A -> B -> C` 는 `A` 가 `C` 에 의존한다는 의미를 가진다.



## 맛보기

### 설치

간단한 설치는 아래와 같이 파드를 다운받으면 된다. 

```shell
$ kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.11/samples/addons/jaeger.yaml
```

위에 있는 파드는 `jaegertracing/all-in-one` 이며 jaeger의 모든 컴포넌트가 하나의 도커 이미지로 구성되어있는 컴포넌트를 사용한다. 테스트용으로 적당하며 프로덕션용은  [Jaeger Docker Image Download](https://www.jaegertracing.io/download/#docker-images) 에서 all-in-one을 제외한 모든 컴포넌트를 다운로드 받아야 할 것이다.





추적을 활성화하면 istio가 추적에 사용하는 샘플링 속도를 설정할 수 있다. `meshConfig.defaultConfig.tracing.sampling` 옵션을 사용하여 샘플링 속도를 설정할 수 있다. 가본 샘플링 속도는 1%이다.

```shell
cat <<'EOF' > tracing.yaml
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
spec:
  meshConfig:
    enableTracing: true
    defaultConfig:
      tracing:
        sampling: 10
        custom_tags:
          my_tag_header:
            header:
              name: host
EOF
```

```shell
$ istioctl install -f tracing.yaml 
```



### 접속

jaeger에 접속하기 위해 먼저 jaeger를 실행하자.

```shell
$ istioctl dashboard jaeger
```

* `--address 0.0.0.0` 을 사용하여 외부 접속이 가능하도록 할 수 있다.
* `--port <port>` 를 사용하여 포트를 지정할 수 있다. *(default=16686)*



## 용어

OpenTracing 스펙에서 정의한 용어를 간단히 알아보자.



### Span, Trace

Span은 Jaeger의 논리적 작업 단위를 말한다. Span은 operation name, operation 시작 시간과 기간이 있다. Span은 인과 관계를 모델링하기 위해 중첩되고 정렬될 수 있다.

Trace는 시스템을 통한 데이터/실행 경로이다. 그리고 span의 방향성 순환 그래프로 생각할 수 있다.



![Traces And Spans](https://www.jaegertracing.io/img/spans-traces.png)



## 컴포넌트

Jaeger는 모든 Jaeger 백엔드 컴포넌트가 하나의 프로세스로 실행되는 all-in-one 바이너리로 배포하거나 확장성 있는 분산 시스템으로 배포할 수도 있다. 여기에 두 가지 주요 배포 옵션이 있다.

1. Collector가 직접 스토리지에 작성
2. Collector가 Kafka와 같은 예비 버퍼에 작성

![Architecture](https://www.jaegertracing.io/img/architecture-v1.png)



![Architecture](https://www.jaegertracing.io/img/architecture-v2.png)

일단 Jaeger의 컴포넌트가 어떻게 구성되는지, 컴포넌트 끼리 어떤 상호작용을 하는지 살펴보자.



### Jaeger Client Library

Jaeger 클라이언트는 OpenTracing API의 언어별 구현체이다. Jaeger Client는 수동으로 또는 Flask, Dropwizard, gRPC 등과 같은 OpenTracing과 통합된 기존 오픈소스 프레임워크와 함께 분산 추적을 위한 애플리케이션을 계측하는데 사용할 수 있다.

계측 서비스는 새로운 요청을 받고 컨텍스트 정보(traceId, spanId, baggage 등)를 나가는 요청에 첨부할 때 span을 생성한다. id와 baggage만이 요청과 함께 전파되며 oepration name, timing, tags, logs 와 같은 프로파일링 데이터는 전파되지 않는다. 대신, 이는 프로세스 외부의 백그라운드에서 jaeger 백엔드로 비동기적으로 전송된다.

그리고 Jaeger client는 오버헤드를 줄이기 위해 다양한 샘플링 전략을 사용한다. 추적이 샘플링될 때, 프로파일링 스팬 데이터가 캡쳐되고 Jaeger 백엔드로 전송된다. 추적이 샘플링되지 않을 때는 프로파일링 데이터가 수집되지 않는다. 그리고 OpenTracing API에 대한 호출은 오버헤드를 최소화하기 위해 단락된다. 기본적으로 Jaeger Client는 0.1%의 trace를 샘플링하며 Jaeger 백엔드에서 샘플링 전략을 검색할 수 있다. 

![Context propagation explained](https://www.jaegertracing.io/img/context-prop.png)



### Agent

Jarger Agent는 UDP로 전송되는 span을 청취한 후 이를 일괄 처리하여 Collector로 보내는 네트워크 데몬이다. 이는 모든 호스트의 인프라 구성 요소로 배포되도록 설계되었다. Agent는 클라이언트에서 수집기의 라우팅 및 검색을 추상화한다.



### Collector

Jaeger Collector는 Jaeger Agent로부터 trace를 받고 프로세싱 파이프라인을 통해 실행한다. 파이프라인은 trace를 확인하고 인덱싱을 하며 변환을 수행한 후 저장한다.

Jaeger의 스토리지는 플러그 가능한 컴포넌트이다. 이는 Cassandra, Elasticsearch, Kafka를 지원한다.



### Query

Query는 스토리지로부터 trace를 검색하고 이를 표시하기 위한 UI를 호스팅하는 서비스이다.



### Ingester

Ingester는 Kafka 토픽을 읽고 다른 스토리지 백엔드에 작성하는 서비스이다.





## Trace 컨텍스트 전파

분산 추적을 통해 사용자는 여러 서비스에 분산되어있는 메쉬를 통해 요청을 추적할 수 있다. 이는 시각화를 통해 요청 대기 시간, 직렬화 및 병렬 처리에 대해 더 깊이 이해할 수 있다. 

istio는 Envoy의 분산 추적 기능을 활용하여 즉시 추적 통합을 제공한다. 특히, istio는 다양한 추적 백엔드를 설치하고 추적 범위를 자동으로 보내도록 프록시를 구성하는 옵션을 제공한다. 

istio 프록시가 자동으로 span을 보내지만, 전체 추적을 연결하기 위해서는 몇 가지 힌트가 필요하다. 프록시가 span 정보를 보낼 때 span이 단일 추적으로 올바르게 연관될 수 있도록 애플리케이션은 적절한 HTTP 헤더를 전파해야 한다. 이를 위해 애플리케이션은 들어오는 요청에서 나가는 요청으로 아래 헤더를 수집하고 전파해야 한다.

* `x-request-id` : Envoy에서 요청을 고유하게 식별하고 안정적인 액세스 로깅 및 추적을 수행하는 데 사용된다. 모든 외부 요청에 대해 생성하고, 내부 요청에 대해서는 헤더가 없다면 새로 생성한다.  `x-request-id` 가 전체 메쉬에 걸쳐 안정적인 ID를 갖기 위해서는 클라이언트 애플리케이션 간에 전파되어야 한다.
* `x-b3-traceid` : 이 헤더는 Envoy에서 Zipkin tracer가 사용한다. 64비트의 길이를 가지며 trace 전체의 ID를 나타낸다. 모든 Span은 이 traceid를 공유한다. (span들이 한 trace에 묶이려면 이 헤더를 공유해야 한다.)
* `x-b3-spanid` : 이 헤더는 Envoy에서 Zipkin tracer가 사용한다. 64비트의 길이를 가지며 추적 트리에서 현재 작업의 위치를 나타낸다. 
* `x-b3-parentspanid` : 이 헤더는 Envoy에서 Zipkin tracer가 사용한다. 64비트의 길이를 가지며 추적 트리에서 부모 작업의 위치를 나타낸다. span이 root면 해당 헤더는 없다.
* `x-b3-sampled` : 이 헤더는 Envoy에서 Zipkin tracer가 사용한다. Sampled flag가 지정되지 않거나 1로 설정되면 span이 추적 시스템에 보고된다. sampled가 한번 설정되면 sampled 값이 downstream으로 전달된다.
* `x-b3-flags` : 이 헤더는 Envoy에서 Zipkin tracer가 사용한다. 하나 이상의 옵션이 인코딩된다.
* `x-ot-span-context` : Envoy에서 LigthStep 추적 프로그램과 함께 사용할 때 추적 범위 간의 적절한 상-하위 관계를 설정하는 데 사용된다. Egress span은 ingress span의 자식이 된다. Envoy는 해당 헤더를 ingress 요청에 주입학고 로컬 서비스로 전달한다. 



위에서 설명된 헤더는 기본적으로 [envoy proxy](https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_conn_man/headers#x-request-id)가 생성한다.

















---

https://blog.navr.com/alice_k106/221832024817

















