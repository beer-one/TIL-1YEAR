# Redis UI

[redis-ui](https://github.com/patrikx3/redis-ui)는 레디스 클러스터 환경을 지원하는 레디스 웹 UI 오픈소스이다. 제공하는 기능으로는 아래와 같다.



## Features

* 여러 레디스 클러스터 연결 가능
* Read-Only 기능 제공 (설정 바꾸면 되서 의미없지만)
* CRUD 
* 레디스 서버/클라이언트, 메모리 정보 등 통계정보
* 콘솔



## 없는 기능

* 로그인



## 설치 방법 (K8s)

1. [깃 레포지토리](https://github.com/patrikx3/redis-ui)에서 clone 한다. 여기서는 k8s/chart 만 사용할 것이다. (헬름차트 레포가 없어서 클론 할 수 밖에 없다..)
2.  `k8s/chart/value.yaml` 파일을 열어서 정보를 수정한다.

```yaml

image:
  repository: patrikx3/p3x-redis-ui
  tag: latest
  pullPolicy: Always

license:

replicas: 1

resources: {}
  # requests:
  #   cpu: 100m
  #   memory: 100M
  # limits:
  #   cpu: 200m
  #   memory: 200M

connections: []
# - name:
#   host:
#   port:
#   id: (unique)
#   password:
#   azure: false
#   cluster: false
#   awsElastiCache: false
#   nodes: If you configure a cluster
#   - host:
#     port:
#     id:
#     password:

service:
  type: ClusterIP
#  nodePort: 30200 If type is NodePort

ingress:
  enabled: false
  annotations: {}
  #   kubernetes.io/ingress.class: nginx
  #   cert-manager.io/cluster-issuer: letsencrypt
  paths: [/]
  hosts: []
  tls: []
  # - hosts: [host]
  #   secretName: host-tls
```

* Connections 에는 여러 레디스 클러스터 정보가 들어간다. 알맞게 기입하고 redis cluster 를 연결하려면 `cluster: true` 로 바꿔주고 `nodes` 에 클러스터 노드를 하나씩 넣으면 된다.
* nodePort로 서비스를 열고싶다면 `service.type: NodePort` 로 하고 `nodePort`를 설정하면 된다.

3. 다음 명령어를 입력하여 배포한다.

   ```shell
   # k8s/chart 디렉토리에서 시작
   $ helm install redis-ui . -n redis-cluster
   ```

   * 네임스페이스는 넣어도 되고 안넣어도 된다.



## 주의할점?

Redis-ui 화면에서 Settings 을 통해 설정값을 바꿀 수 있긴한데 K8s helm chart로 배포하면 설정 파일이 `configmap` 을 통해 만들어진다. 기본적으로 `configmap` 을 통해 만들어진 파일은 `read-only` 속성을 가진다. (이는 k8s에서 의도한 것이라고 한다.) 그래서 설정 파일을 변경하면 다음 오류가 떠서 찝찝하다.

![image-20211216101329649](/Users/nhn/Library/Application Support/typora-user-images/image-20211216101329649.png)



사실 에러가 나도 업데이트 사항이 적용된다. (아마 내부적으로 로컬스토리지를 사용하고 있지 않을까..) 대신 애플리케이션이 죽으면 업데이트 내용이 날아간다. `configmap`은 불변성을 가지는 설정에만 적합한 것 같다.



이를 방지하기 위해서는 configmap이 아닌 PersistenceVolume (PV) 을 사용하면 된다. PV를 사용하고 싶으면 매니페스트 파일을 수정하면 된다. 