# Volume

도커 볼륨은 디스크에 있는 디렉터리이거나 다른 컨테이너에 있다. 도커는 볼륨 드라이버를 제공하지만 기능이 다소 제한적이다. K8s는 다양한 유형의 볼륨을 지원한다. 그 중 임시 볼륨은 파드의 생명주기에 따른수명을 가지지만 Persistent Volume은 파드의 수명과 관련 없이 영구적으로 존재한다. 파드가 죽으면 임시 볼륨을 삭제하지만 Persistent Volume은 삭제하지 않아서 파드가 재시작되어도 데이터는 보존된다.

볼륨을 사용하려면 `.spec.volumes`에서 파드에 제공할 볼륨을 지정하고 `.spec.containers[*].volumeMounts` 의 컨테이너에 해당하는 볼륨을 마운트할 위치를 선언한다. 컨테이너의 프로세스는 도커 이미지와 볼륨으로 구성된 파일 시스템 뷰를 본다. 볼륨은 이미지 내에 지정된 경로에 마운트된다. 파드 구성의 각 컨테이너는 각 볼륨을 마운트할 위치를 독립적으로 지정해야 한다.



## Persistent Volume

PV는 관리자가 제공하거나 스토리지 클래스를 사용하여 동적으로 제공하는 클러스터의 스토리지이다. PV는 노드와 마찬가지로 클러스터 리소스이다. PV는 해당 볼륨을 사용하는 개별 파드와 별개의 라이프사이클을 가진다. 해당 API 오브젝트는 NFS, iSCSI 또는 클라우드 공급자별 스토리지 시스템 등 스토리지 구현에 대한 세부 정보를 담아낸다.

**Persistent Volume Claim (PVC)** 는 사용자의 스토리지에 대한 요청이다. PVC는 PV 리소스를 사용하고 특정 크기 및 접근 모드를 요청할 수 있다. (ReadWriteOnce, ReadOnlyMany, ReadWriteMany ...) PVC를 사용하면 사용자가 추상화된 스토리지 리소스를 사용할 수 있다. 



## Volume과 Claim 라이프사이클

PV와 PVC 간의 상호작용은 아래 라이프사이클을 따른다.



### 프로비저닝 (Provisioning)

정적 프로비저닝과 동적 프로비저닝이 있다.

* 정적 프로지버닝:  클러스터 관리자가 여러 PV를 만든다. 클러스터 사용자가 사용할 수 있는 실제 스토리지의 세부사항을 제공한다.

* 동적 프로비저닝: 관리자가 생성한 정적 PV가 사용자의 PVC와 일치하지 않으면 클러스터는 PVC를 위해 특별히 볼륨을 동적으로 프로비저닝하려고 시도한다. 이 프로비저닝은 스토리지클래스 기반으로 한다. 동적 프로비저닝이 발생하도록 하려면 PVC는 스토리지 클래스를 요청해야 하며, 스토리지 클래스를 `""` 로 요청하는 PVC는 동적 프로비저닝을 비활성화한다.



### 바인딩 (Binding)

마스터의 컨트롤 루프는 새로운 PVC를 감시하고 일치하는 PV를 찾아 서로 바인딩한다. PV가 PVC에 대해 동적으로 프로비저닝 되는 경우, 루프는 항상 해당 PV를 PVC에 바인딩한다. 동적 프로비저닝이 되지 않은 경우, 사용자는 항상 최소한 그들이 요청한 것을 얻지만 볼륨은 요청된 것을 초과할 수 있다. 일단 바인딩 되면 PVC는 어떻게 바인딩 되었는지 상관없이 배타적으로 바인딩된다. PVC : PV 바인딩은 일대일 매핑으로, PV와 PVC 사이의 양방향 바인딩인 `claimRef` 를 사용한다.

일치하는 볼륨이 없는 경우, 클래임은 바인딩되지 않은 상태로 무한정 남아있다. 일치하는 볼륨이 제공되면 클레임이 바인딩 된다. 



### 사용 중 (Using)

파드는 클래임을 볼륨으로 사용한다. 클러스터는 클레임을 검사하여 바인딩된 볼륨을 찾고 해당 볼륨을 파드에 마운트한다. 일단 클레임이 바인딩되면, 바인딩된 PV는 사용자가 필요로 하는 한 사용자에게 속한다. 사용자는 파드의 `volumes` 블록에 `persistentVolumeClaim` 을 포함하여 파드를 스케줄링하고 클레임한 PV에 접근한다.



### 반환 (Reclaiming)

볼륨을 다 사용하고 나면 리소스를 반환할 수 있는 API를 사용하여 PVC 오브젝트를 삭제할 수 있다. 반환 정책으로는 **Retain**, **Recycle**, **Delete** 가 있다.



* **Retain**: 리소스를 수동으로 반환할 수 있도록 보존시킨다. PVC가 삭제되면 PV는 여전히 존재하고 볼륨은 릴리즈된 것으로 간주한다. 
* **Delete**: 관련된 스토리지 자산을 모두 삭제한다.
* **Recycle**: 기본 볼륨 플러그인에서 지원하는 경우, 볼륨에서 `rm -rf /volumes/*` 을 수행하고 새 클레임에 다시 사용할 수 있도록 한다. (볼륨 내용은 삭제, PV는 유지)



## 퍼시스턴트 볼륨 예약

컨트롤 플레인은 클러스터에서 PVC를 일치하는 PV에 바인딩할 수 있다. 대신, PVC를 특정 PV에 바인딩하려면 미리 바인딩해야 한다.

PVC에서 PV를 지정하여 특정 PV와 PVC간의 바인딩을 선언한다. PV가 존재하고 `claimRef` 필드를 통해 PVC를 예약하지 않은 경우 PV 및 PVC가 바인딩된다.

바인딩은 노드 선호도를 포함하여 일부 볼륨 일치 기준과 관계없이 발생한다. 컨트롤 플레인은 스토리지 클래스, 접근 모드 및 요청된 스토리지 크기가 유효한지 확인한다.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: foo-pvc
  namespace: foo
spec:
  storageClassName: "" # 빈 문자열은 명시적으로 설정해야 하며 그렇지 않으면 기본 스토리지클래스가 설정됨
  volumeName: foo-pv
  ...

```

이 방법은 PV에 대한 바인딩 권한을 보장하지는 않는다. 다른 PVC에서 지정한 PV를 사용할 수 있는 경우, 먼저 해당 스토리지 볼륨을 예약해야 한다. PV에 `claimRef` 필드에 관련 PVC를 지정하여 다른 PVC가 바인딩할 수 없도록 해야한다.

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: foo-pv
spec:
  storageClassName: ""
  claimRef:
    name: foo-pvc
    namespace: foo
  ...
```



## 실습

먼저 PV를 생성한다. nfs-server가 있다면 nfs로, 아무 여건이 없으면 hostpath로 하자. 만약 nfs로 한다면 모든 클러스터 노드에 `nfs-common`이 설치되어 있어야 한다.

```shell
$ sudo apt-get install -y nfs-common
```

**pv-test.yaml**

**pv-test.yaml**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-test
spec:
  capacity:
    storage: 1Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Recycle
  storageClassName: slow
  mountOptions:
    - hard
    - nfsvers=4.1
  nfs:
    path: /mnt/data/pv-test
    server: {nfs-server-ip}
```

```shell
$ kubectl apply -f pv-test.yaml
```



PV가 생성되었는지 확인하자.

```shell
$ kubectl get pv
NAME      CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS      CLAIM   STORAGECLASS   REASON   AGE
pv-test   1Gi        RWO            Recycle          Available           slow                    14m
```



그다음 PVC를 생성하자. 

**pvc-test.yaml**

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pvc-test
spec:
  storageClassName: slow
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 512Mi # < 1Gi
```

```shell
$ kubectl apply -f pvc-test.yaml
```



PVC를 생성한 후에 컨트롤 플레인은 PVC의 요구사항을 만족하는 PV를 찾는다. 

* 컨트롤 플레인이 동일한 스토리지클래스를 갖는 PV를 찾는다. (`spec.stroageClassName=slow`)
* 용량 등 PVC의 스펙 요구사항을 만족하는 PV를 찾는다. (`spec.resource.requests.storage` >= 512Mi)



PVC를 생성한 후 다시 PV를 확인하면 PV가 바운드된 것을 확인할 수 있다. (`STATUS=Bound`)

```shell
$ kubectl get pv
NAME      CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM              STORAGECLASS   REASON   AGE
pv-test   1Gi        RWO            Recycle          Bound    default/pvc-test   slow                    27m
```



마찬가지로 PVC를 확인하면 PVC에 `pv-test` 볼륨이 바인딩 된 것을 확인할 수 있다.

```shell
$ kubectl get pvc
NAME        STATUS    VOLUME    CAPACITY   ACCESS MODES   STORAGECLASS       AGE
pvc-test    Bound     pv-test   1Gi        RWO            slow               8m16s
```



그 다음은 볼륨으로 PVC를 사용하는 파드를 만들어보자.

**pv-pod-test.yaml**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pv-pod-test
spec:
  volumes:
    - name: pv-storage-test
      persistentVolumeClaim:
        claimName: pvc-test
  containers:
    - name: pv-container-test
      image: nginx
      ports:
        - containerPort: 80
          name: "http-server"
      volumeMounts:
        - mountPath: "/usr/share/nginx/html"
          name: pv-storage-test
```

```shell
$ kubectl apply -f pv-pod-test.yaml
```



그 다음, 파드에서 구동되고있는 컨테이너에 접근한다.

```shell
$ kubectl exec -it pv-pod-test -- /bin/bash
```



그 후 파일이 공유되어있는지 확인한다.

```shell
$ ls /usr/share/nginx
html
```



















