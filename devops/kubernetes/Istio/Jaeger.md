# Jaeger Operator

Jaeger Operator는 쿠버네티스 Operator의 구현체이다. Operator는 다른 소프트웨어를 실행하는 작업의 복잡성을 완화시키는 소프트웨어이다. Operator는 기술적으로는 쿠버네티스 애플리케이션을 패키징, 배포 및  관리하는 방법이다. 

쿠버네티스 애플리케이션은 쿠버네티스 API와 `kubectl`, `oc` 와 같은 툴을 사용하여 쿠버네티스에 배포되고 관리되는 애플리케이션이다. 쿠버네티스를 최대한 활용하려면 쿠버네티스에서 실행되는 앱을 서비스하고 관리하기 위해 확장할 응집력 있는 API 세트가 필요하다. Operator를 쿠버네티스에서 애플리케이션을 관리하는 런타임으로 생각하면 된다.



Jaeger Operator는 쿠버네티스 기반 클러스터에 설치되고 특정 네임스페이스 또는 클러스터 전체의 Jaeger custom resource (CR) 를 감시할 수 있다. 보통은 한 클러스터 당 하나의 Jaeger Operator를 두지만 multi-tenant 시나리오로 네임스페이스당 하나의 Jaeger Operator를 둘 수도 있다. 

새로운 Jaeger CR이 감지되면, operator는 자신을 리소스 소유자로 설정하려고 시도하고 `jaegertracing.io/operated-by` 레이블을 새 CR로 설정하고 운영자의 네임스페이스와 이름을 레이블 값으로 사용한다.

여러 operator가 같은 네임스페이스를 관찰하면서 공존할 수 있지만 어떤 operator가 CR의 소유자로 자신을 설정하는 데 성공하는지 여부는 정의되지 않은 동작이다. sidecar의 자동 주입또한 마찬가지로 정의되지 않은 동작이다. 그러므로, 각 네임스페이스를 감시하는 operator는 최대 하나인 것이 좋다. 



Jaeger operator는 아래를 수행할 수 있다.

* 모든 네임스페이스의 Jaeger 리소스와 관련된 이벤트를 관찰
* `sidecar.jaegertracing.io/inject` 애노테이션을 찾는 네임스페이스 자체를 관찰
* `sidecar.jargertracing.io/inject` 애노테이션을 기반으로 sidecar를 주입하거나 제거하는 모든 디플로이먼트를 관찰
* 필요하다면 cluster role binding을 생성



Cluster-wide resource(`ClusterRole`, `ClusterRoleBinding`) 을 사용하지 않는다면  `WATCH_NAMESPACE` 를 Jaeger Operator가 Jaeger 리소스와 관련된 이벤트를 감시해야 하는 쉼표로 구분된 네임스페이스 리스트로 설정한다. 

지정된 네임스페이스(`observability` 등) 에서 Jaeger operator를 실행하고 Jaeger 리소스를 다른 네임스페이스 (`myproject` 등) 에서 관리할 수 있다. 그러기 위해서는 Operator가 리소스를 감시해야 하는 각 네임스페이스에 대해 다음과 같이 `RoleBinding` 을 사용하면 된다.

```yaml
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: jaeger-operator-in-myproject
  namespace: myproject
subjects:
- kind: ServiceAccount
  name: jaeger-operator
  namespace: observability
roleRef:
  kind: Role
  name: jaeger-operator
  apiGroup: rbac.authorization.k8s.io
```



## Operator 설치

먼저 `observability` 네임스페이스를 생성하고 Jaeger Operator를 설치한다. 기본적으로 operator는 설치된 것과 동일한 네임스페이스를 관찰한다.

```shell
$ kubectl create namespace observability # <1>
$ kubectl create -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/crds/jaegertracing.io_jaegers_crd.yaml # <2>
$ kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/service_account.yaml
$ kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/role.yaml
$ kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/role_binding.yaml
$ kubectl create -n observability -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/operator.yaml
```

1. 디플로이먼트 파일에서 기본적으로 사용되는 네임스페이스를 먼저 생성한다.
2. `apiVersion: jaegertracing.io/v1` 에 대한 `Custom Resource Definition` 을 설치한다.



Operator는 클러스터 전체 권한이 부여된 경우 추가 기능을 활성화한다. 활성화 하려면 아래를 수행하자.

```shell
$ kubectl create -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/cluster_role.yaml
$ kubectl create -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/v1.28.0/deploy/cluster_role_binding.yaml
```



jaeger operator를 `observability` 가 아닌 다른 네임스페이스를 사용하려면 `cluster_role_binding.yaml` 파일을 다운로드하고 커스터마이징 해야 한다. 그리고  `operator.yaml` 파일을 다운로드하고 커스터마이징 해서 환경변수 `WATCH_NAMESPACE` 를 빈 값으로 설정하여 모든 네임스페이스에서 인스턴스를 감시하려고 할 수도 있다.



# Jaeger Deploy

Jaeger 인스턴스를 생성하는 전략이 여러가지가 있다. 이 전략은 custom resource file에 정의되어있고 Jaeger backend에 사용될 아키텍쳐를 결정한다. 기본 전략은 `allInOne` 이다. 그 외의 전략은 `production` 과 `streaming` 이 있다.



## AllInOne Strategy

해당 전략은 개발, 테스팅, 데모 목적에 맞는 전략이다. 매인 백엔드 컴포넌트, agent, collector와 query service는 모두 인메모리 저장소를 사용하도록 구성된 단일 실행 파일로 패키징된다. 이 전략은 하나의 레플리카로 스케일링이 불가능하다.



## Production Strategy

해당 전략은 추적 데이터의 장기 저장이 중요하며 확장성 있고 가용성이 높은 아키텍쳐가 필요한 프로덕션 환경에 적합한 전략이다. 그러므로 각 백엔드 컴포넌트는 분리되어 배포된다. 

Agent는 애플리케이션의 사이드카 또는 데몬셋으로 주입될 수 있다. 

Collector는 필요에 따라라 오토스케일링이 되도록 구성할 수 있다. 기본적으로는 `.Spec.Collector.Replicas`에 아무 값도 제공되지 않으면 Jaeger Operator는 collector에 **Horizontal Pod Autoscaler (HPA)** 구성을 생성한다. `.Spec.Collector.MaxReplicas` 에 대한 명시적인 값과 Collector의 파드가 소비할 것으로 예상되는 리소스에 대한 합리적인 값을 설정하는 것이 좋다. `.Spec.Collector.MaxReplicas` 값이 설정되어있지 않다면 기본값인 `100` 으로 설정된다. 오토스케일링을 원치 않는다면 `.Spec.Collector.Autoscale`을 `false` 로 설정하면 된다. 

Query와 Collector는 지원되는 스토리지 타입으로 구성된다. 현재는 **Cassandra** 와 **Elasticsearch** 가 있다. 성능 및 복원력을 위해 필요에 따라 이러한 각 구성 요소의 여러 인스턴스를 프로비저닝할 수 있다.

주요 추가 요구사항은 스토리지 유형 및 옵션에 대한 세부 정보를 제공하는 것이다. 예를 들면 아래와 같다.

```yaml
    storage:
      type: elasticsearch
      options:
        es:
          server-urls: http://elasticsearch:9200
```



## Streaming Strategy

해당 전략은 collector와 백엔드 스토리지 사이에 효과적으로 위치하는 스트리밍 기능을 제공하여 production 전략을 강화하도록 설계되었다. 이는 부하가 높은 상황에서 백엔드 스토리지에 대한 부담을 줄이는 효과를 가지며 다른 추적 후처리 기능이 스트리밍 플랫폼에서 직접 실시간 span 데이터를 활용할 수 있도록 한다. 

collector는 production strategy 와 마찬가지로 필요에 따라 오토스케일링 구성을 할 수 있다.

ingester도 마찬가지로 필요에 따라 오토스케일링 구성을 할 수 있다. 기본적으로는 `.Spec.Ingester.Replicas` 에 아무 값도 제공되어있지 않는다면, Jaeger Operator는 ingester에 대한 HPA 구성을 생성한다. `.Spec.Ingester.MaxReplicas` 에 대한 명시적인 값과 ingester의 파드가 소비할 것으로 예상되는 리소스에 대한 합리적은 값을 설정하는 것이 좋다. `.Spec.Ingester.MaxReplicas` 가 설정되어있지 않는다면 기본값인 `100` 으로 설정된다. 오토스케일링을 원치 않는다면 `.Spec.Ingester.Autoscale` 을 `false` 로 설정하면 된다.



### 기존 카프카 클러스터

streaming strategy 에서는 collector*(as producer)* 컴포넌트와 ingester*(as consumer)* 컴포넌트에서 접근할 카프카에 대한 세부 정보를 제공해야 한다.

```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: simple-streaming
spec:
  strategy: streaming
  collector:
    options:
      kafka: # <1>
        producer:
          topic: jaeger-spans
          brokers: my-cluster-kafka-brokers.kafka:9092
  ingester:
    options:
      kafka: # <1>
        consumer:
          topic: jaeger-spans
          brokers: my-cluster-kafka-brokers.kafka:9092
      ingester:
        deadlockInterval: 5s # <2>
  storage:
    type: elasticsearch
    options:
      es:
        server-urls: http://elasticsearch:9200
```

1. collector에서 메시지를 생산하고 ingester에서 메시지를 소비하기 위해 collector와 ingester에 Kafka 구성을 지정한다.
2. **deadlockInterval** 은 메시지가 도착하지 않을 때 수집이 종료되는 것을 방지하기 위해 기본적으로 비활성화되어 있다. *(set to 0)* 하지만 종료하기 전에 메시지를 기다리는 시간을 지정하도록 구성할 수도 있다.



### 자체 프로비저닝된 카프카 클러스터

자체 프로비저닝된 카프카 클러스터를 사용하려면 producer/consumer 브로커 프로퍼티를 지정하지 않으면 된다.



## Custom Resource Definitions 이해

쿠버네티스 API에서 리소스는 특정 종류의 API 오브젝트의 컬렉션을 저장하는 엔드포인트이다. 예를 들어, built-in 파드 리소스는 파드 오브젝트의 컬렉션을 포함한다. CRD(*Custom Resource Definition)* 오브젝트는 클러스터 내 새롭고 유일한 오브젝트 `Kind` 를 정의하고 쿠버네티스 apiserver가 전체 수명 주기를 처리할 수 있도록 한다. 

Custom Resource(CR)를 생성하기 위해서는 클러스터 관리자는 첫째로 CRD를 생성해야 한다. CRD는 클러스터 유저가 새로운 리소스 타입을 추가하기 위해 CR을 생성하는 것을 허락한다. Operator는 생성된 CR 오브젝트를 관찰하고 CR이 생성되는 것을 본다면 Operator는 CR 오브젝트에 정의된 파라미터를 기반으로 애플리케이션을 생성한다.



참고로, 더 복잡함 all-in-one 인스턴스를 만드는 방법은 아래와 같다.

```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: my-jaeger
spec:
  strategy: allInOne # <1>
  allInOne:
    image: jaegertracing/all-in-one:latest # <2>
    options: # <3>
      log-level: debug # <4>
  storage:
    type: memory # <5>
    options: # <6>
      memory: # <7>
        max-traces: 100000
  ingress:
    enabled: false # <8>
  agent:
    strategy: DaemonSet # <9>
  annotations:
    scheduler.alpha.kubernetes.io/critical-pod: "" # <10>
```

1. 기본 전략은 `allInOne` 이다.
2. 사용할 이미지를 정의한다.
3. 기본 바이너리에 전달될 옵션이다. 모든 옵션은 jaeger 문서나 관련 바이너리의 `--help` 옵션에서 알 수 있다.
4. 옵션은 key-value 구조로 정의할 수 있다. 이 경우에서는 바이너리에 `--log-level=debug` 로 옵션이 전달된다.
5. 사용될 스토리지 타입이다. 기본적으로는 `memory` 로 지정된다. `Cassandra`나 `Elasticsearch`, `Kafka` 등 지원되는 스토리지 타입을 지정해도 된다.
6. 스토리지와 관련된 옵션을 정의한다. 
7. 어떤 옵션은 네임스페이스가 있으며 이를 중첩된 객체로 분할할 수 있다. 이 옵션은 `memory.max-traces: 100000` 으로 지정된다.
8. 기본적으로 query service에 대한 ingress object 가 생성된다. 이는 `enabled` 옵션을 `false` 로 설정하면 비활성화된다. 
9. 기본적으로 operator는 agent가 타겟 파드에 사이드카로 배포된다고 가정한다. strategy를 `DaemonSet` 으로 설정하면 operator가 agent를 DaemonSet으로 배포하도록 변경할 수 있다. DaemonSet으로 설정하려면 Tracer 클라이언트는 `JAEGER_AGENT_HOST` 환경변수를 Node의 IP로 오버라이드 해야 한다.
10. 모든 디플로이먼트에 적용될 애노테이션을 지정한다. 이는 개별 컴포넌트에 정의된 애노테이션으로 재정의될 수 있다.



> You can view example custom resources for different Jaeger configurations [on GitHub](https://github.com/jaegertracing/jaeger-operator/tree/master/examples).





# Custom Resource 구성

simplest example을 사용하고 기본값을 사용하여 Jaeger 인스턴스를 생성하거나 고유한 사용자 지정 리소스 파일을 생성할 수 도 있다.



## Storage Options



### Cassandra storage

스토리지 타입이 Cassandra로 설정되어있다면 operator는 Jaeger가 실행하는 데 필요한 스키마를 생성하는 배치잡을 자동으로 생성한다. 배치잡은 jaeger 설치를 차단하므로 스키마가 성공적으로 생성된 후에만 시작된다. 해당 배치잡의 생성은 `enabled` 프로퍼티를 `false` 로 설정하면 비활성화 시킬 수 있다.

```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: cassandra-with-create-schema
spec:
  strategy: allInOne
  storage:
    type: cassandra
    options:
      cassandra:
        servers: cassandra
        keyspace: jaeger_v1_datacenter3
    cassandraCreateSchema: # <1>
      datacenter: "datacenter3"
      mode: "test"
```

1. `create-schema` 잡에 대한 옵션이다.



### Elasticsearch storage

```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: simple-prod
  namespace: jaeger # <1>
spec:
  strategy: production
  storage:
    type: elasticsearch # <2>
    options:
      es:
        server-urls: https://quickstart-es-http:9200 # <3>
        index-prefix: my-prefix
        tls: # <4>
          ca: /es/certificates/ca.crt
    secretName: jaeger-secret # <5>
  volumeMounts: # <6>
    - name: certificates
      mountPath: /es/certificates/
      readOnly: true
  volumes:
    - name: certificates
      secret:
        secretName: quickstart-es-http-certs-public
```

1. 네임스페이스이다. ECK로 elasticsearch를 생성하였다면 elasticsearch와 같은 네임스페이스로 해야 한다.

2. 스토리지 타입을 elasticsearch로 잡았다.

3. 실제 접근할 elasticsearch 서버 url이다. 

4. TLS 구성이다. 해당 예시에서는 오직 CA certificate만 구성하였는데 mutual TLS를 사용한다면 `es.tls.key`, `es.tls.cert` 을 추가로 구성할 수 있다.

5. elasticsearch에 접속할 계정 정보를 가지는 시크릿이다. 이 시크릿에는 `ES_PASSWORD`, `ES_USERNAME` 이 필요하며 아래 명령어로 시크릿을 만들면 된다. [K8S 공식문서](https://kubernetes.io/ko/docs/concepts/configuration/secret/#%EC%A0%9C%EC%95%BD-%EC%82%AC%ED%95%AD)에서는 **시크릿 리소스는 동일한 네임스페이스에 있는 파드에서만 참조가 가능하다**고 한다. 그러므로 Jaeger의 네임스페이스와 같은 네임스페이스에서 시크릿을 생성해야 한다.

   ```shell
   $ kubectl create secret generic jaeger-secret --from-literal=ES_PASSWORD=${password} --from-literal=ES_USERNAME=${username} -n jaeger
   ```

6. 볼륨마운트와 볼륨이다. `tls.ca` 와 관련이 있으며 volume은 elasticsearch의 cert 관련된 secret이다. ECK로 elasticsearch를 생성하였다면 기본적으로 `quickstart-es-http-certs-public` 시크릿이 생성된다. **(5)** 와 같은 이유로, **(1)** 이 설명된다.







##  Jaeger Agent 구성

Jaeger Agent는 sidecar로써 파드마다 붙어있을 수도 있고 daemonset으로써 모든 노드당 하나의 agent로 구성할 수도 있다. 각자의 장단점이 있을 것이니까 기호에 맞게 구성하면 된다.



### Agent Sidecar

operator는 디플로이먼트 워크로드에 Jaeger agent sidecar를 주입할 수 있으며 디플로이먼트나 네임스페이스에 `sidecar.jaegertracing.io.inject` 애노테이션에 알맞는 값이 있으면 자동으로 주입시켜준다. 해당 애노테이션 값은 `"true"` 이거나 `kubectl get jaegers` 에서 반환되는 Jaeger 인스턴스 이름으로 하면 된다. 

`"true"` 를 사용하면 디플로이먼트와 동일한 네임스페이스에 대해 정확히 하나의 jaeger 인스턴스가 있어야 한다. 아니면, operator는 사용할 jaeger 인스턴스를 자동으로 확인할 수 없다. Jaeger 인스턴스 이름을 디플로이먼트에 지정하는 것이 `true` 를  적용하는 것 보다 우선순위가 높다.

아래 예시는 동일한 네임스페이스에서 사용 가능한 단일 jaeger 인스턴스를 가리키는 jaeger agent와 함께 사이드카가 주입되는 간단한 애플리케이션이다. 

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
  annotations:
    "sidecar.jaegertracing.io/inject": "true"
spec:
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: acme/myapp:myversion
```

sidecar가 주입된다면 jaeger agent는 `localhost`로 접근이 가능하다.



### Deployment-level Configurations for Injected Sidecars

jaeger-opoerator에 의해 관리되지 않는 디플로이먼트에 사이드카가 주입된다면 디플로이먼트 레벨에서 적용하는 많은 구성은 에이전트 노드 아래에 지정되지 않는 한 사이드카의 디플로이먼트에 적용되지 않는다. 사이드카 디플로이먼트에는 다음 구성이 지원된다.

* Volume
* ImagePullSecret

아래 Jaeger 구성은 사이드카 디플로이먼트에 `agent-volume` 과 `agent-imagePullSecrets` 을 추가한다.

```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: my-jaeger
spec:
  agent:
    volumeMounts:
    - name: agent-volume
      mountPath: /tmp/agent
      readOnly: true
    volumes:
      - name: agent-volume
        secret:
          secretName: agent-secret
    imagePullSecrets:
    - name: agent-imagePullSecret
```



### Manually Defining Jaeger Agent Sidecars

디플로이먼트 이외의 컨트롤러에 대해 (StatefulSet, DaemonSet 등) Jaeger agent 사이드카는 명세 내에서 수동으로 정의할 수 있다.

아래 예시는 Jaeger agent 사이드카에 대한 컨테이너 섹션에 포함할 수 있는 수동 정의이다. (`template.spec.containers` 에 이 내용이 들어간다.)

```yaml
- name: jaeger-agent
  image: jaegertracing/jaeger-agent:latest
  imagePullPolicy: IfNotPresent
  ports:
    - containerPort: 5775
      name: zk-compact-trft
      protocol: UDP
    - containerPort: 5778
      name: config-rest
      protocol: TCP
    - containerPort: 6831
      name: jg-compact-trft
      protocol: UDP
    - containerPort: 6832
      name: jg-binary-trft
      protocol: UDP
    - containerPort: 14271
      name: admin-http
      protocol: TCP
  args:
    - --reporter.grpc.host-port=dns:///jaeger-collector-headless.observability:14250
    - --reporter.type=grpc
```



### Installing the Agent as DaemonSet

기본적으로 Operator는 타겟 애플리케이션에 agent가 사이드카로 배포될 것으로 예상한다. 이는 multi-tenant 시나리오 또는 더 나은 로드밸런싱과 같이 여러 목적에 대래 편리하지만 에이전트를 DaemonSet으로 설치할 수도 있다. 아래 예시는 에이전트의 전략을 `DaemonSet` 으로 설정한다.

```yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: my-jaeger
spec:
  agent:
    strategy: DaemonSet
```

그러면 tracer 클라이언트에 에이전트가 있는 위치를 알려야 할 가능성이 크다. 이는 보통 `JAEGER_AGENT_HOST` 환경변수에 쿠버네티스 노드 IP를 설정하는 방식을 사용한다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: acme/myapp:myversion
        env:
        - name: JAEGER_AGENT_HOST
          valueFrom:
            fieldRef:
              fieldPath: status.hostIP
```

















### Elasticsearch VS Cassandra

Cassandra는 key-value DB라서 traceID를 검색하는데 더 빠르지만 Elasticsearch와 같은 강력한 검색기능은 없다.

Jaeger 백엔드는 key-value 스토리지 위에 클라이언트 측에서 검색 기능을 구현한다. 이 기능은 제한적이며 일관되지 않은 결과를 생성할 수 있다. 그러나 ES는 이와 관련된 이슈가 없기 때문에 사용성이 향상된다. ES는 kibana 대시보드에서 직접 쿼리가 가능하다. 그리고 유용한 분석 및 집계 기능을 제공한다.

성능 실험에서는 ES보다 Cassandra가 단일 쓰기가 더 빠르다는 것을 발견하였다. 그러나 Jaeger backend는 key-value 스토리지 위에 검색 기능을 구현해야 하기 때문에 Cassandra에 span을 쓰는 것은 실제로 오래걸린다. Span 자체에 대한 레코드를 작성하는 것 외에도 Jaeger는 서비스 이름과 operation 이름 인덱싱에 대한 추가 쓰기와 모든 태그에 대한 추가 인덱스 쓰기를 수행한다. 반면에 Elasticsearch에 span을 저장하는 것은 단일 쓰기이며 모든 인덱싱은 ES 노드 내부에서 발생한다. 그 결과로 전체 throughput은 Cassandra와 Elasticsearch가 비슷하다.

Cassndra 백엔드의 이점은 데이터 TTL에 대한 기본 지원으로 인해 유지관리가 간소화된다. ES에서 데티어 만료는 index rotation을 통해 관리된다.









 