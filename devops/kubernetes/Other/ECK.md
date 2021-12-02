# ECK

Elastic Cloud on Kubernetes (ECK)를 사용하면 기본 쿠버네티스 오케스트레이션 기능을 확장하여 Elasticsearch 클러스터 등을 쉽게 배포, 보호, 업그레이드 하는 작업을 수행할 수 있다.



## 설치



### 쿠버네티스 클러스터에 ECK 배포

1. RBAC rule과 함께 CRD와 오퍼레이터를 설치한다.

```shell
$ kubectl create -f https://download.elastic.co/downloads/eck/1.8.0/crds.yaml
$ kubectl apply -f https://download.elastic.co/downloads/eck/1.8.0/operator.yaml
```

2. 오퍼레이터 로그를 모니터링한다. (설치가 잘 되는지 확인)

```shell
$ kubectl -n elastic-system logs -f statefulset.apps/elastic-operator
```



### Elastic cluster 배포

1. Elastic cluster를 배포한다. 일단 배포를 위해 Namespace, Elasticsearch(with PVC), PersistentVolume를 메니페스트로 만들었다.

**elastic-ns.yaml**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: jaeger
```

**pv.yaml**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elastic-pv
  namespace: jaeger
spec:
  capacity:
    storage: 1Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Recycle
  storageClassName: standard
  mountOptions:
    - hard
    - nfsvers=4.1
  nfs:
    path: /mnt/data/elasticsearch
    server: {nfs-server-ip}
```

**es.yaml**

```yaml
apiVersion: elasticsearch.k8s.elastic.co/v1
kind: Elasticsearch
metadata:
  name: quickstart
  namespace: jaeger
spec:
  version: 7.15.2
  nodeSets:
  - name: default
    count: 1
    config:
      node.store.allow_mmap: false
    volumeClaimTemplates:
    - metadata:
        name: elasticsearch-data # Do not change this name unless you set up a volume mount for the data path.
        namespace: jaeger
      spec:
        accessModes:
        - ReadWriteOnce
        resources:
          requests:
            storage: 1Gi
        storageClassName: standard
```



생성한 메니페스트 yaml파일로 오브젝트를 생성하자.

```shell
$ kubectl apply -f elastic-ns.yaml
$ kubectl apply -f pv.yaml
$ kubectl apply -f es.yaml
```



생성하였다면 elasticsearch가 잘 떠있는지 확인하자.

```shell
$ kubectl get elasticsearch -n jaeger
NAME         HEALTH   NODES   VERSION   PHASE   AGE
quickstart   green    1       7.15.2    Ready   97m
```

* 최초로 생성하고 바로 확인하면 HEALTH, PHASE가 빈 값으로 설정될건데 성공적으로 배포되었다면 HEALTH=green, PHASE=Ready로 변경된다.

```shell
$ kubectl get pods --selector='elasticsearch.k8s.elastic.co/cluster-name=quickstart' -n jaeger
NAME                      READY   STATUS    RESTARTS   AGE
quickstart-es-default-0   1/1     Running   0          98m
```

```shell
$ kubectl logs -f quickstart-es-default-0 -n jaeger
```



### Elasticsearch 접근

Elasticsearch가 배포되었다면 아래 서비스가 생성될 것이다.

```shell
$ kubectl get svc -n jaeger
NAME                      TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
quickstart-es-default     ClusterIP   None             <none>        9200/TCP   99m
quickstart-es-http        ClusterIP   10.107.199.158   <none>        9200/TCP   99m
quickstart-es-transport   ClusterIP   None             <none>        9300/TCP   99m
```

그 중 `quickstart-es-http` 서비스로 접근할건데 이 서비스는 ClusterIP로 생성되었다. 접근하기 위해서는 다음 단계를 거쳐야 한다.

1. Credential 얻기: elasticsearch의 기본 유저는 `elastic` 이며, 이는 자동으로 쿠버네티스 시크릿에 저장된 패스워드를 가진다.

   ```shell
   $ kubectl get secret quickstart-es-elastic-user -o go-template='{{.data.elastic | base64decode}}' -n jaeger
   ```

2. 쿠버네티스 클러스터 내에서 elasticsearch 엔드포인트로 요청하자.

   ```shell
   $ kubectl run curl-test --image=radial/busyboxplus:curl -i --tty --rm
   If you don't see a command prompt, try pressing enter.
   [ root@curl-test:/ ]$ curl -u "elastic:D409w306qa7no2jsqLyGg55Y" -k "https://quickstart-es-http.jaeger:9200"
   {
     "name" : "quickstart-es-default-0",
     "cluster_name" : "quickstart",
     "cluster_uuid" : "I6YFVzHuR06pvN5ZjngClA",
     "version" : {
       "number" : "7.15.2",
       "build_flavor" : "default",
       "build_type" : "docker",
       "build_hash" : "93d5a7f6192e8a1a12e154a2b81bf6fa7309da0c",
       "build_date" : "2021-11-04T14:04:42.515624022Z",
       "build_snapshot" : false,
       "lucene_version" : "8.9.0",
       "minimum_wire_compatibility_version" : "6.8.0",
       "minimum_index_compatibility_version" : "6.0.0-beta1"
     },
     "tagline" : "You Know, for Search"
   }
   ```

   

### Kibana 배포

Kibana 인스턴스를 배포하자.

1. elasticsearch 클러스터와 관련된 kibina 인스턴스를 정의하자.

**kibana.yaml**

```yaml
apiVersion: kibana.k8s.elastic.co/v1
kind: Kibana
metadata:
  name: quickstart
  namespace: jaeger
spec:
  version: 7.15.2
  count: 1
  elasticsearchRef:
    name: quickstart
```

```shell
$ kubectl apply -f kibana.yaml
```



2. kibana가 잘 배포되었는지 확인하자.

```shell
$ kubectl get kibana -n jaeger
NAME         HEALTH   NODES   VERSION   AGE
quickstart   green    1       7.15.2    3m28s
```



3. Kibana service에 접근하자. kibana를 배포하였다면 `quickstart-kb-http` 서비스가 생성될 것이다.

```shell
$ kubectl get svc -n jaeger
NAME                      TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
quickstart-es-default     ClusterIP   None             <none>        9200/TCP   125m
quickstart-es-http        ClusterIP   10.107.199.158   <none>        9200/TCP   125m
quickstart-es-transport   ClusterIP   None             <none>        9300/TCP   125m
quickstart-kb-http        ClusterIP   10.96.182.201    <none>        5601/TCP   4m
```

포트포워딩을 해도 되고 NodePort로 외부 노출시켜도 된다. (이건 자유) 

로그인이 필요한데 로그인을 하려면 elastic 계정으로 하면 된다.





















