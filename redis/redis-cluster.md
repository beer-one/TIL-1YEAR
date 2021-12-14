# Redis Cluster with K8s

레디스 클러스터는 여러 Redis 노드에서 데이터가 자동으로 샤딩되도록 설치할 수 있는 방법을 제공한다.

레디스 클러스터는 파티셔닝 중에도 약간의 가용성을 제공한다. 즉, 실용적인 측면에서는 일부 노드가 실패하거나 통신할 수 없을 때 작업을 계속할 수 있는 기능을 제공한다.

레디스 클러스터를 사용하면 다음 기능을 사용할 수 있다.

* 여러 노드 사이의 데이터셋을 자동으로 분리할 수 있다.

* 노드의 서브셋에 오류가 발생하거나 클러스터의 나머지 부분과 통신할 수 없을 때 작업을 계속할 수 있다.

  ​																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																													

레디스 클러스터를 쿠버네티스에서 어떻게 구성하는지 한번 알아보자.



## Redis Cluster TCP Port

일반적으로 레디스는 6379번 포트 하나만 사용하지만, 레디스 클러스터를 구성하는 모든 레디스 노드들은 두개의 TCP 포트를 열어야 한다. 하나는 클라이언트와 통신하기 위한 일반적인 포트이고*(default: 6379)*, 나머지 하나는 클러스터 버스용 포트이다. 보통 클러스터 버스용 포트는 일반 포트에서 10000을 추가한다. *(16379)*

클러스터 버스용 포트는 binary protocol을 이용하는 노드 사이의 통신 채널인 클러스터 버스에 사용된다. 클러스터 버스는 실패 감지, 구성 업데이트, 장애 대응, 인가 등을 위해 노드에서 사용된다. 클라이언트는 클러스터 버스 포트를 사용하지 않고 항상 일반 레디스 포트를 통해 통신한다. 하지만 방화벽에서 두 포트 모두 열어야 한다. 그렇지 않으면 레디스 클러스터 노드끼리 통신이 불가능하기 때문이다.

레디스 클러스터가 잘 돌아가기 위해서는 각 노드에 대해서 다음이 필요하다.

* 클라이언트와 통신하기 위한 일반 포트는 클러스터에 도달해야 하는 모든 클라이언트와 다른 클러스터 노드에 대해 열려있어야 한다.
* 클러스터 버스 포트는 모든 클러스터 노드에 대해 열려있어야 한다.



## Master-Replica 모델

마스터 노드의 서브셋이 실패하거나 다른 노드들과 통신할 수 없을 때 계속 사용할 수 있도록 하기 위해 레디스 클러스터는 모든 해시 슬롯이 1에서 N 개의 레플리카가 있는 마스터-레플리카 모델을 사용한다.

만약 A, B, C 노드가 있을 때, B 노드가 실패하게 되면 [5501, 11000] 범위의  B 노드의 해시슬롯을 제공할 수 없기 때문에 클러스터가 지속되지 못한다. 하지만, 클러스터를 생성할 때, 모든 마스터 노드에 대해 레플리카를 추가한다면 하나의 노드가 망가져도 망가진 마스터노드에 대응되는 레플리카 노드가 마스터로 승격하여 서비스를 지속시킬 수 있다.



## 데이터 샤딩

레디스 클러스터는 일관된 해싱을 사용하지 않지만 모든 키가 개념적으로 해시 슬롯이라고 불리는 것의 일부인 다른 형태의 샤딩을 사용한다.

레디스 클러스터에 16384(2^14)개의 해시 슬롯이 있다. 그리고 주어진 키의 해시 슬롯이 무엇인지 계산하기 위해 `key %. 16384` 의 CRC16을 사용한다.

레디스 클러스터의 모든 노드들은 해시 슬롯의 서브셋을 담당한다. 예를 들어, 클러스터에 3개의 노드가 있다고 가정하자.

* Node A: [0, 5500]
* Node B: [5501, 11000]
* Node C: [11001, 16383]



이런식으로 구성하면 노드의 추가 및 삭제가 쉽다. 예를 들어, Node D를 추가한다고 가정하면, Node A, B, C에서 D로 해시 슬롯을 이동해야 한다. 그리고 Node A를 클러스터에서 없애고 싶으면 A에 있는 해시 슬롯을 B와 C로 이동하면 된다. A의 해시슬롯이 없어진다면 클러스터에서 노드를 삭제할 수 있다.

노드에서 다른 노드로 해시 슬롯을 이동할 때 작업을 중지할 필요가 없기 때문에 노드를 추가 및 제거하거나 노드가 보유한 해시 슬롯의 비율을 변경하는 데 가동 중지 시간이 필요하지 않는다. 

레디스 클러스터는 단일 명령 실행에 관련된 모든 키가 모두 동일한 해시 슬롯에 속하는 한 여러 키 작업을 지원한다. 사용자는 해시 태그라는 개념을 사용하여 여러 키를 동일한 해시 슬롯의 일부로 만들 수 있다.







## Consistency Guarantees

레디스 클러스터는 강한 일관성을 보장하지 않는다. 이는 특정 조건에서 레디스 클러스터가 시스템에서 클라이언트에 대해 승인한 쓰기를 잃을 수 있음을 의미한다. 

레디스 클러스터가 쓰기를 잃을 수 있는 이유는 비동기 복제를 사용하기 때문이다. 이는 쓰기 중에 다음이 발생할 수 있다는 뜻이다.

* 클라이언트가 마스터 B에 데이터를 쓴다.
* 마스터 B는 클라이언트에 OK를 응답한다.
* 마스터 B는 레플리카 B1, B2, B3에게 작성된 데이터를 전파한다.



여기서, B는 latency를 방지하기 위해 클라리언트에 응답하기 전에 B1, B2, B3의 ack를 기다리지 않는다. 클라이언트가 어떤 데이터를 쓸 때, B는 쓴 데이터에 대해 ack를 날리지만 레플리카에 데이터를 보내기 전에 마스터에서 크래쉬가 발생하면 복제본 중 하나가 마스터로 승격되어 쓰기가 영구적으로 손실될 수 있다. (복제본은 데이터를 안받은 상황) 



## 레디스 클러스터 생성 (for K8S)

레디스 클러스터를 쿠버네티스로 띄워보자. 먼저 레디스 클러스터를 구성하는 모든 쿠버네티스 오브젝트를 하나의 네임스페이스로 관리하기 위해 네임스페이스를 정의하자.

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: redis-cluster
```



레디스 클러스터를 생성하기 위해서는 가장 먼저  `cluster mode` 로 구동 중인 몇 개의 빈 레디스 인스턴스가 필요하다. 일단 레디스의 최소 구성 파일 예시는 아래와 같다.

```
port 7000
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
```

* `cluster-enabled yes` 설정을 하면 클러스터 모드를 활성화한다.

해당 구성파일에서는 노드에 대한 추가 구성파일을 `cluster-config-file` 에 명시하였다. 이 파일은 사용자가 임의로 변경이 불가능하다. 이는 레디스 클러스터 인스턴스가 시작하면 자동으로 생성되고 필요할 때 마다 자동으로 업데이트된다.

예상대로 작동하는 클러스터에는 최소 3개의 마스터 노드가 있어야 한다. 테스트를 위해서 마스터 3대와 레플리카 3대로 구성하는 것이 좋다.



일단 위에서 설명한 최소 구성파일을 `ConfigMap`으로 만들어보자.

**redis-config.yaml**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
  namespace: redis
data:
  redis-config: |
    port 6379
    cluster-enabled yes
    cluster-config-file nodes.conf
    cluster-node-timeout 5000
    appendonly yes
  redis-cluster-servers: node-0.redis-hs.redis-cluster.svc.cluster.local:6379 node-1.redis-hs.redis-cluster.svc.cluster.local:6379 node-2.redis-hs.redis-cluster.svc.cluster.local:6379 node-3.redis-hs.redis-cluster.svc.cluster.local:6379 node-4.redis-hs.redis-cluster.svc.cluster.local:6379 node-5.redis-hs.redis-cluster.svc.cluster.local:6379
```

* `redis-cluster-servers` 는 레디스 클러스터를 구성하는 노드의 DNS(IP) 항목을 나열한 것으로, 나중에 클러스터를 생성하기 위해 필요하다.



그리고 레디스 클러스터 노드를 StatefulSet으로 생성하자.

**redis-cluster.yaml**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: redis-cluster
  namespace: redis-cluster
spec:
  selector:
    app: redis-cluster
  clusterIP: None
  ports:
    - protocol: TCP
      port: 6379
      name: client
    - protocol: TCP
      port: 16379
      name: cluster-bus
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: redis-cluster
  namespace: redis-cluster
spec:
  selector:
    matchLabels:
      app: redis-cluster
  maxUnavailable: 1
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: node
  namespace: redis-cluster
  labels:
    app: redis-cluster
spec:
  selector:
    matchLabels:
      app: redis-cluster
  serviceName: redis-cluster
  replicas: 6
  template:
    metadata:
      name: redis-cluster
      labels:
        app: redis-cluster
    spec:
      containers:
      - name: redis
        image: redis:5.0.4
        command:
          - redis-server
          - "/redis-config/redis.conf"
        ports:
        - containerPort: 6379
          name: client
        - containerPort: 16379
          name: cluster-bus
        resources:
          limits:
            cpu: "0.1"
            memory: "256Mi"
        volumeMounts:
        - mountPath: /redis-cluster-data
          name: data
        - mountPath: /redis-config
          name: config
      volumes:
        - name: data
          emptyDir: {}
        - name: config
          configMap:
            name: redis-config
            items:
            - key: redis-config
              path: redis.conf
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                  - key: "app"
                    operator: In
                    values:
                    - redis-cluster
              topologyKey: "kubernetes.io/hostname"
```



### Headless Service + StatefulSet

셀렉터가 있는 헤드리스 서비스는 셀렉터에 일치하는 파드 각각에 대하여 DNS를 부여한다. DNS는 아래와 같이 구성된다. 

`${pod-name}.${svc-name}.${namespace}.svc.cluster.local`

스테이트풀셋은 파드 이름을 `${statefulset-name}-${seq}` 로 짓는다. seq는 0부터 `replica`-1 까지 생성 순서대로 부여된다.

레디스 클러스터 노드를 구성하기 위해 DNS를 만들기 위해 Headless Service + StatefulSet으로 구성하였다.



메니페스트 파일로 오브젝트를 생성 한 후 파드가 레플리카 개수만큼 모두 띄워졌다면 다음 명령어를 입력해보자.

```shell
$ for i in {0..5}; do kubectl logs -n redis-cluster node-$i | grep I\'m; done
1:M 13 Dec 2021 01:02:31.956 * No cluster configuration found, I'm 344fe1bb9705e4c4a741f405d346a84d10c9ce44
1:M 13 Dec 2021 01:02:35.263 * No cluster configuration found, I'm ea29392d5f9eeea51b8023137c80fbce956ab98f
1:M 13 Dec 2021 01:02:37.843 * No cluster configuration found, I'm 7f435a6f315846931ffa3607b3342bda112f2b4f
1:M 13 Dec 2021 01:02:40.331 * No cluster configuration found, I'm cc25494d5379718b0617c30d9b76988749841ed7
1:M 13 Dec 2021 01:02:41.748 * No cluster configuration found, I'm d6f525d3d8e74dc21f51751d717ecba21ba741fe
1:M 13 Dec 2021 01:02:44.559 * No cluster configuration found, I'm 56e18682c6caf973b3709413695bf50168692fd4
```

6개의 클러스터에 대해 Id가 있는 로그를 출력하는 명령어이다. 모든 노드는 `node.conf` 파일이 없기 때문에 모든 노드는 각자 새 ID를 할당한다. 이 ID는 인스턴스가 클러스터 컨텍스트에서 고유한 이름을 가지기 위해 특정 인스턴스에서 영구히 사용된다. 모든 노드는 IP, 포트가 아닌 이 ID로 다른 모든 노드를 기억한다. IP주소와 포트는 변경될 수 있지만 ID는 고유하기 때문에 변경되지 않기 때문이다. 이 id를 `NodeID` 라고 불린다.



### 클러스터 생성

일단 여러 노드가 구동된 상황이다. 여기서 노드에 의미있는 구성을 작성함으로써 클러스터를 생성해야 한다. 

만약 Redis 5버전 이상 사용 중이라면 `redis-cli`에 포함된 레디스 클러스터 커맨드라인 유틸리티의 도움을 받아 쉽게 클러스터를 구성할 수 있다. 

레디스 클러스터를 구성하는 명령어는 아래와 같다.

```shell
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 \
127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
--cluster-replicas 1
```



쿠버네티스에서는 이렇게 실행하면 된다.

```shell
$ kubectl exec -it node-0 -n redis-cluster -- redis-cli --cluster create --cluster-replicas 1 $(kubectl get pods -l app=redis-cluster -n redis-cluster -o jsonpath='{range.items[*]}{.status.podIP}:6379 {end}')
>>> Performing hash slots allocation on 6 nodes...
Master[0] -> Slots 0 - 5460
Master[1] -> Slots 5461 - 10922
Master[2] -> Slots 10923 - 16383
Adding replica 10.244.139.87:6379 to 10.244.139.85:6379
Adding replica 10.244.108.92:6379 to 10.244.139.86:6379
Adding replica 10.244.18.7:6379 to 10.244.108.95:6379
M: 07191232d82b45042c10fa3644377cd9799214e9 10.244.139.85:6379
   slots:[0-5460] (5461 slots) master
M: 72fa9faf4ea969a570b5c4dcc3bdec33aa384e5b 10.244.139.86:6379
   slots:[5461-10922] (5462 slots) master
M: 469013fae202b02c1fb3927d2bf19f26d8c13cfa 10.244.108.95:6379
   slots:[10923-16383] (5461 slots) master
S: 50edfc1b976cf8edcfa3f00137ffa45aff306aeb 10.244.18.7:6379
   replicates 469013fae202b02c1fb3927d2bf19f26d8c13cfa
S: 733277200975da04eb0277cd6a8d52bb3d8f2683 10.244.139.87:6379
   replicates 07191232d82b45042c10fa3644377cd9799214e9
S: fb4f2ebbc3120a82e3c1cfb9f18edc63c623cdc3 10.244.108.92:6379
   replicates 72fa9faf4ea969a570b5c4dcc3bdec33aa384e5b
Can I set the above configuration? (type 'yes' to accept): yes
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join
..
>>> Performing Cluster Check (using node 10.244.139.85:6379)
M: 07191232d82b45042c10fa3644377cd9799214e9 10.244.139.85:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
M: 469013fae202b02c1fb3927d2bf19f26d8c13cfa 10.244.108.95:6379
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
M: 72fa9faf4ea969a570b5c4dcc3bdec33aa384e5b 10.244.139.86:6379
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: 733277200975da04eb0277cd6a8d52bb3d8f2683 10.244.139.87:6379
   slots: (0 slots) slave
   replicates 07191232d82b45042c10fa3644377cd9799214e9
S: fb4f2ebbc3120a82e3c1cfb9f18edc63c623cdc3 10.244.108.92:6379
   slots: (0 slots) slave
   replicates 72fa9faf4ea969a570b5c4dcc3bdec33aa384e5b
S: 50edfc1b976cf8edcfa3f00137ffa45aff306aeb 10.244.18.7:6379
   slots: (0 slots) slave
   replicates 469013fae202b02c1fb3927d2bf19f26d8c13cfa
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```





## 마스터 노드 추가

기존 레디스 클러스터에 새 마스터노드를 추가하는 방법을 알아보자. 마스터노드를 추가하기 위해 일단 empty node를 먼저 생성하자. empty node를 생성한 후 `redis-cli` 에서 클러스터 노드를 추가하는 명령어를 사용하자.

```
redis-cli --cluster add-node ${new_node} ${existing_cluster_node}

// 예시
redis-cli --cluster add-node 127.0.0.1:7006 127.0.0.1:7000
```



쿠버네티스에서는 다음과 같이 수행하면 될 것 같다.

```shell
$ kubectl scale --replicas=8 -n redis-cluster sts/node // node 개수 추가 (6개 -> 8개, master1, slave1 용 empty node 추가)

// 6번째 node가 새로 생긴 노드의 첫 번째 노드
$ kubectl exec -it node-0 -n redis-cluster -- redis-cli --cluster add-node $(kubectl get pods -l app=redis-cluster -n redis-cluster -o jsonpath='{range.items[6, 0]}{.status.podIP}:6379 {end}') 
```



수행 결과는 다음과 같다.

```shell
>>> Adding node 10.244.139.112:6379 to cluster 10.244.139.111:6379
>>> Performing Cluster Check (using node 10.244.139.111:6379)
M: 87cb47654e690e5b73b2fcf73ff6029015fcb483 10.244.139.111:6379
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
S: 5f1485e19002344779aa7331166e241ae11796c8 10.244.108.114:6379
   slots: (0 slots) slave
   replicates 87cb47654e690e5b73b2fcf73ff6029015fcb483
S: 4c71fcaddbb0d82b2cead4e3109571b70eccee4d 10.244.139.113:6379
   slots: (0 slots) slave
   replicates b1c2d8b0f4f03b07c9fcc164a3c55313325f2fc5
M: fdcccee39f0d8571ae1db0235e1b351bae85936b 10.244.108.109:6379
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: 0c7c6ef2787833208c7883e528da6cc45e1515e7 10.244.18.2:6379
   slots: (0 slots) slave
   replicates fdcccee39f0d8571ae1db0235e1b351bae85936b
M: b1c2d8b0f4f03b07c9fcc164a3c55313325f2fc5 10.244.18.3:6379
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Send CLUSTER MEET to node 10.244.139.112:6379 to make it join the cluster.
[OK] New node added correctly.
```



클러스터 노드 정보를 확인해보자.

```shell
kubectl exec -it node-0 -n redis-cluster -- redis-cli cluster nodes
5f1485e19002344779aa7331166e241ae11796c8 10.244.108.114:6379@16379 slave 87cb47654e690e5b73b2fcf73ff6029015fcb483 0 1639451641551 5 connected
b0c8950469431d954ff383d6a6b1cf043c521efb 10.244.139.112:6379@16379 master - 0 1639451642651 0 connected
87cb47654e690e5b73b2fcf73ff6029015fcb483 10.244.139.111:6379@16379 myself,master - 0 1639451642000 1 connected 0-5460
4c71fcaddbb0d82b2cead4e3109571b70eccee4d 10.244.139.113:6379@16379 slave b1c2d8b0f4f03b07c9fcc164a3c55313325f2fc5 0 1639451642000 4 connected
fdcccee39f0d8571ae1db0235e1b351bae85936b 10.244.108.109:6379@16379 master - 0 1639451642552 2 connected 5461-10922
0c7c6ef2787833208c7883e528da6cc45e1515e7 10.244.18.2:6379@16379 slave fdcccee39f0d8571ae1db0235e1b351bae85936b 0 1639451642000 6 connected
b1c2d8b0f4f03b07c9fcc164a3c55313325f2fc5 10.244.18.3:6379@16379 master - 0 1639451641651 3 connected 10923-16383
```

기존 마스터 노드는 `connected` 뒤에 슬롯 번호가 명시되어있다. 그리고 새로 생긴 마스터노드는 슬롯이 할당이 되어있지 않아 번호가 명시되어있지 않는다. 슬롯이 할당되지 않은 마스터노드는 레플리카가 마스터가 되기 원할 때 선택 프로세스에 참여하지 않는다.

마스터 노드를 제대로 사용하기 위해서는 `redis-cli` 기능 중 하나인 리샤딩을 사용하여 노드에게 해쉬슬롯을 할당해야 한다. 



### 리샤딩

다음 명령어로 리샤딩이 가능하다.

```
redis-cli --cluster reshard <host>:<port> --cluster-from <node-id> --cluster-to <node-id> --cluster-slots <number of slots> --cluster-yes
```

자주 리샤딩할 경우 자동화를 구현할 수는 있지만, 현재 redis-cli에서는 클러스터 노드 간 키 분포를 확인하고 필요에 따라 슬롯을 지능적으로 이동하는 클러스터 균형을 자동으로 재조정 할 수 있는 방법은 없다. (추후 개발 예정이라고 한다.)



### 리밸런스

다음 명령어로 마스터 해시 슬롯을 리밸런스 할 수 있다.

```
redis-cli --cluster rebalance 127.0.0.1:6379
```

슬롯을 리밸런스 할 때 슬롯이 할당되지 않은 마스터노드는 리밸런스 대상이 되지 않는다. 슬롯이 없는 마스터도느도 리밸런스 대상이 되려면 `--cluster-use-empty-masters` 플래그를 추가하면 된다.





## 레플리카 노드 추가

레플리카 노드는 다음 명령어로 추가할 수 있다.

```
redis-cli --cluster add-node ${replica-host}:${replica-port} ${cluster-host}:${cluster-port} --cluster-slave
```



## 요약



### 클러스터 생성

**redis-cluster.yaml**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: redis-hs
  namespace: redis-cluster
spec:
  selector:
    app: redis-cluster
  clusterIP: None
  ports:
    - protocol: TCP
      port: 6379
      name: client
    - protocol: TCP
      port: 16379
      name: cluster-bus
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: redis-cluster
  namespace: redis-cluster
spec:
  selector:
    matchLabels:
      app: redis-cluster
  maxUnavailable: 1
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: node
  namespace: redis-cluster
  labels:
    app: redis-cluster
spec:
  selector:
    matchLabels:
      app: redis-cluster
  serviceName: redis-hs
  replicas: 6
  template:
    metadata:
      name: redis-cluster
      labels:
        app: redis-cluster
    spec:
      containers:
      - name: redis
        image: redis:5.0.4
        command:
          - redis-server
          - "/redis-config/redis.conf"
        ports:
        - containerPort: 6379
          name: client
        - containerPort: 16379
          name: cluster-bus
        resources:
          limits:
            cpu: "0.1"
            memory: "256Mi"
        volumeMounts:
        - mountPath: /redis-cluster-data
          name: data
        - mountPath: /redis-config
          name: config
      volumes:
        - name: data
          emptyDir: {}
        - name: config
          configMap:
            name: redis-config
            items:
            - key: redis-config
              path: redis.conf
```



**create-redis-cluster.sh**

```shell
# statefulset, headless service 등 생성
kubectl apply -f redis-cluster.yaml

sleep 3

curReplicas=0
desiredReplicas=6

echo 'curReplicas:'$curReplicas', desiredReplicas: '$desiredReplicas

# 스테이트풀셋이 모두 Ready 상태일 때 까지 대기
while [ $curReplicas -lt $desiredReplicas ]
do
        curReplicas=$(kubectl get sts -n redis-cluster node -o jsonpath='{range.items[0]}{.status.readyReplicas}');

	echo 'replicas: '$curReplicas'/'$desiredReplicas
	sleep 1
done;

sleep 1

# 레디스 클러스터 생성 명령어
kubectl exec -it node-0 -n redis-cluster -- redis-cli --cluster create --cluster-replicas 1 $(kubectl get pods -l app=redis-cluster -n redis-cluster -o jsonpath='{range.items[*]}{.status.podIP}:6379 {end}') --cluster-yes
```



### 스케일아웃

마스터 1, 레플리카 1씩 스케일아웃하는 쉘스크립트이다.

```shell
curReplicas=$(kubectl get sts -n redis-cluster node -o jsonpath='{range.items[0]}{.status.currentReplicas}');

# master, replica 각각 하나씩 추가
desiredReplicas=$(($curReplicas + 2)) 
kubectl scale --replicas=$desiredReplicas -n redis-cluster sts/node;

# 추가된 레플리카가 모두 ready 상태일 때 까지 대기
while [ $curReplicas -lt $desiredReplicas ]
do
	curReplicas=$(kubectl get sts -n redis-cluster node -o jsonpath='{range.items[0]}{.status.readyReplicas}');
	sleep 0.5
done;

# 확인
kubectl get sts -n redis-cluster node
echo "all replicas are running"

master=$(($curReplicas - 2))
slave=$(($master + 1))


# 마스터 노드 추가
kubectl exec -it node-0 -n redis-cluster -- redis-cli --cluster add-node $(kubectl get pods -l app=redis-cluster -n redis-cluster -o jsonpath='{range.items['$master', 0]}{.status.podIP}:6379 {end}')

sleep 2

# 레플리카 노드 추가
kubectl exec -it node-0 -n redis-cluster -- redis-cli --cluster add-node $(kubectl get pods -l app=redis-cluster -n redis-cluster -o jsonpath='{range.items['$slave', 0]}{.status.podIP}:6379 {end}') --cluster-slave

sleep 2

# 노드 리밸런싱
kubectl exec -it node-0 -n redis-cluster -- redis-cli --cluster rebalance $(kubectl get pods -l app=redis-cluster -n redis-cluster -o jsonpath='{range.items[0]}{.status.podIP}:6379{end}') --cluster-use-empty-masters
```



















