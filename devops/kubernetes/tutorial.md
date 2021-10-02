# Tutorial



## 클러스터

쿠버네티스는 컴퓨터들을 연결하여 단일 형상으로 동작하도록 컴퓨팅 클러스터를 구성하고 높은 가용성을 제공하도록 조율한다. 그리고 컨테이너 기반 애플리케이션을 클러스터에 분산시키고 스케쥴링하는 일을 더욱 효율적으로 자동화한다. K8s는 오픈소스 플랫폼이며 운영 수준의 안정성을 제공한다. 

클러스터는 크게 두 가지 형태의 자원으로 이루어진다.

* 컨트롤플레인: 클러스터를 조율
* 노드: 애플리케이션을 구동하는 워커





### 컨트롤 플레인

컨트롤 플레인은 클러스터 관리를 담당한다. (애플리케이션 스케줄링, 항상성 유지, 스케일아웃, 롤링아웃 등등..)

애플리케이션을 쿠버네티스에 배포하기 위해서는 컨트롤 플레인에 애플리케이션 컨테이너를 구동하는 명령을 내리면 된다. 그러면 컨트롤 플레인은 컨테이너의 클러스터의 어느 노드에 구동시킬지 스케줄링한다. 



### 노드

노드는 쿠버네티스 클러스터 내 워커 머신이다. 각 노드는 노드를 관리하고 컨트롤 플레인과 통신하는 kubelet이라는 에이전트를 갖는다. 그리고 컨테이너 운영을 담당하는 툴(containerd, 도커)도 갖는다. 운영 트래픽을 처리하는 k8s 클러스터는 최소 세 대의 노드를 가져야 한다.



### 클러스터 설치

클러스터는 물리머신과 가상머신 둘 다 설치가 가능하다. 간단하게는 minikube를 설치할 수 있는데 minikube는 로컬 머신에 VM을 만들고 하나의 노드로 구성된 간단한 클러스터를 생성한다.

간단히 homebrew로 설치 가능하다. 

```shell
$ brew install minikube
```

설치했으면 버전확인을 하자.

```shell
$ minikube version
minikube version: v1.23.2
commit: 0a0ad764652082477c00d51d2475284b5d39ceed
```

minikube로 클러스터를 구동하기 위해서는 아래 명령어를 입력하면 된다. kubectl도 사용할 수 있게 되었다.

```shell
$ minikube start
...
🏄  끝났습니다! kubectl이 "minikube" 클러스터와 "default" 네임스페이스를 기본적으로 사용하도록 구성되었습니다.
```

Kubectl 버전도 확인해보자.

```shell
$ kubectl version
Client Version: version.Info{Major:"1", Minor:"21", GitVersion:"v1.21.2", GitCommit:"092fbfbf53427de67cac1e9fa54aaa09a28371d7", GitTreeState:"clean", BuildDate:"2021-06-16T12:59:11Z", GoVersion:"go1.16.5", Compiler:"gc", Platform:"darwin/amd64"}
Server Version: version.Info{Major:"1", Minor:"22", GitVersion:"v1.22.2", GitCommit:"8b5a19147530eaac9476b0ab82980b4088bbc1b2", GitTreeState:"clean", BuildDate:"2021-09-15T21:32:41Z", GoVersion:"go1.16.8", Compiler:"gc", Platform:"linux/amd64"}
```

여기서는 버전이 Client version과 Server version이 두 가지 있는데 Client version은 kubectl 버전이고, Server version은 마스터에 설치되어있는 쿠버네티스 버전이다.



이제 클러스터 세부사항을 살펴보자. 이 커맨드는 애플리케이션을 호스팅할 수 있는 모든 노드들을 보여준다. (minikube는 노드가 하나 뿐이라 하나만 나온다.) 여기서는 노드의 상태가 `running` 으로 출력된다.

```shell
$ kubectl cluster-info
Kubernetes control plane is running at https://127.0.0.1:61857
CoreDNS is running at https://127.0.0.1:61857/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```





## 앱 배포하기

쿠버네티스 클러스터를 구동하면 그 위에 컨테이너화된 애플리케이션을 배포할 수 있다. 배포를 하기 위해서는 `디플로이먼트` 설정을 만들어야 한다. 디플로이먼트는 쿠버네티스가 애플리케이션의 인스턴스를 어떻게 생성하고 업데이트해야 하는지를 지시한다. 디플로이먼트가 만들어지면 쿠버네티스 컨트롤 플레인이 해당 디플로이먼트에 포함된 애플리케이션 인스턴스가 클러스터의 개별 노드에서 실행되도록 스케줄한다.

애플리케이션 인스턴스가 생성되면 디플로이먼트 컨트롤러는 지속적으로 인스턴스를 모니터링한다. 인스턴스를 구동 하고 있는 노드가 다운되거나 삭제되면 디플로이먼트 컨트롤러가 인스턴스를 클러스터 내부의 다른 노드의 인스턴스로 교체시켜준다. (Self-healing)

kubectl을 통해 디플로이먼트를 생성하고 관리할 수 있다. 디플로이먼트를 생성할 때 애플리케이션에 대한 컨테이너 이미지와 구동시키고자 하는 복제 수를 지정해야 한다. 생성 후에도 이런 정보들을 변경할 수도 있다. 



### 노드 확인

kubectl로 클러스터 내 모든 노드들을 확인할 수 있다.

```shell
$ kubectl get nodes
NAME       STATUS   ROLES                  AGE   VERSION
minikube   Ready    control-plane,master   53m   v1.22.2
```



### 디플로이먼트 생성

디플로이먼트를 생성해보자. 디플로이먼트를 생성하기 위해 디플로이먼트 이름과 앱 이미지 경로를 제공해야 한다.

```shell
$ kubectl create deployment kubernetes-bootcamp --image=gcr.io/google-samples/kubernetes-bootcamp:v1

deployment.apps/kubernetes-bootcamp created
```

디플로이먼트를 생성하기 전에 먼저 애플리케이션을 구동시키기 적합한 노드를 탐색한다. 그리고 그 적합한 노드에서 실행되도록 애플리케이션을 스케줄링한다. 그리고 필요할 때 새로운 노드에서 인스턴스를 재스케줄링 하기 위해 클러스터를 구성한다.



### 디플로이먼트 조회

클러스터에 내 디플로이먼트를 조회해보자.

```shell
$ kubectl get deployments
NAME                  READY   UP-TO-DATE   AVAILABLE   AGE
kubernetes-bootcamp   1/1     1            1           3m38s
```



쿠버네티스에서 실행되는 파드들은 개인의 고립된 네트워크에서 실행된다. 기본적으로 파드는 같은 클러스터 내 다른 파드와 서비스에서 볼 수 있지만 네트워크 밖에서는 볼 수 없다. kubectl을 사용하면 다른 애플리케이션과 상호작용하기 위해 API 엔드포인트를 통해 상호작용한다. 

파드가 클러스터 외부에서는 통신할 수 없기 때문에 파드만으로는 애플리케이션을 외부로부터 서비스를 할 수 없다. 외부와 통신하기 위해서는 다른 방법들을 사용해야 한다. 이는 추후에..

kubectl 커맨드는 클러스터 전체의 개인 네트워크로 통신을 전달하는 프록시를 만들 수 있다. 이 프록시는 Crtl+C를 누르면 종료되고 프록시가 실행되는 동안 어떠한 output도 보여주지 않는다.

하나의 터미널에서 프록시를 하나 만들어보자.

```shell
$ kubectl proxy
```

프록시를 만들면 호스트와 쿠버네티스 클러스터와 연결이 된다. 프록시는 터미널로부터 API를 직접 접근할 수 있다. 새 터미널을 하나 켜서 프록시로 요청을 하나 보내보자.

```shell
$ curl http://localhost:8001/version


{
  "major": "1",
  "minor": "22",
  "gitVersion": "v1.22.2",
  "gitCommit": "8b5a19147530eaac9476b0ab82980b4088bbc1b2",
  "gitTreeState": "clean",
  "buildDate": "2021-09-15T21:32:41Z",
  "goVersion": "go1.16.8",
  "compiler": "gc",
  "platform": "linux/amd64"
}
```

 API 서버는 각 파드별로 파드 이름을 기반으로 엔드포인트를 자동적으로 생성할 것이다. 이 역시 프록시를 통해 접근 가능하다.



먼저 클러스터 내 실행 중인 파드 이름이 팔요하므로 파드 이름을 가져와 환경변수 `POD_NAME`에 저장해보자.

```shell
export POD_NAME=$(kubectl get pods -o go-template --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}')
echo Name of the Pod: $POD_NAME
```

그 후 API를 요청해보자.

```shell
$ curl http://localhost:8001/api/v1/namespaces/default/pods/$POD_NAME

{
  "kind": "Pod",
  "apiVersion": "v1",
  "metadata": {
    "name": "kubernetes-bootcamp-57978f5f5d-v7ckv",
    "generateName": "kubernetes-bootcamp-57978f5f5d-",
    "namespace": "default",
    "uid": "6ab584a7-3740-4276-937d-c30e6ba9183f",
    "resourceVersion": "1940",
    "creationTimestamp": "2021-10-01T06:27:00Z",
    "labels": {
      "app": "kubernetes-bootcamp",
      "pod-template-hash": "57978f5f5d"
    },
    ...
}
```

프록시를 사용하지 않고 새 디플로이먼트에 접근할 수 있으려면 서비스가 필요하다.



## 파드와 노드

### 파드

파드는 하나 이상의 애플리케이션 컨테이너 그룹을 나타내는 쿠버네티스의 추상적 개념으로 일부는 컨테이너에 대한 자원을 공유한다. 자원은 아래와 같다.

* 볼륨과 같은 공유 스토리지
* 클러스터 IP 주소와 같은 네트워크 자원
* 컨테이너 이미지 버전 또는 포트 등 각 컨테이너가 동작하는 방식에 대한 정보



파드는 로컬호스트 애플리케이션 모형을 만들어서 서로 다른 애플리케이션 컨테이너들을 포함할 수 있다. 예를 들어, 파드는 Node.js 앱과 함께 Node.js 웹서버에 의해 생성되는 데이터를 저장하는 다른 컨테이너와 함께 할 수 있다. 파드 내 컨테이너는 IP주소와 포트 스페이스를 공유하고 항상 같이 존재하고, 같이 스케줄링되고 동일 노드상의 컨텍스트를 공유하면서 동작한다.

파드는 쿠버네티스 상의 최소 단위이다. 쿠버네티스에서 디플로이먼트를 생성할 때 디플로이먼트는 컨테이너 내부에서 컨테이너와 함께 파드를 생성한다. 그리고 각 파드는 스케줄링 된 노드에 결합되며 종료되거나 삭제될 때 까지 해당 노드에서 살아간다. 노드가 종료되거나 제거된다면, 파드는 클러스터 내 가능한 다른 노드로 재스케줄링 된다.



### 노드

노드는 쿠버네티스에서의 워커 머신이다. 파드는 이 노드에서 동작한다. 각 노드는 컨트롤 플레인에 의해 관리된다. 하나의 노드는 여러 개의 파드를 가질 수 있고 컨트롤 플레인은 클러스터 내 노드를 통해 파드에 대한 스케줄링을 자동으로 처리한다. 스케줄링은 각 노드의 사용 가능한 리소스를 고려하여 작동된다.

모든 쿠버네티스 노드는 최소한 다음과 같이 동작한다.

* kubelet은 쿠버네티스 컨트롤 플레인과 노드 간 통신을 책임지는 프로세스이며 하나의 머신 상에서 동작하는 파드와 컨테이너를 관리한다.
* 컨테이너 런타임은 레지스트리에서 컨테이너 이미지를 가져와 묶여있는 것을 풀고 애플리케이션을 동작시키는 책임을 맡는다.



### 파드 조회

```shell
$ kubectl get pods
NAME                                   READY   STATUS    RESTARTS   AGE
kubernetes-bootcamp-57978f5f5d-v7ckv   1/1     Running   0          94m
```



### 파드 정보 확인

파드 내의 어떤 컨테이너가 있는지, 컨테이너를 빌드할 때 어떤 이미지를 사용했는지 볼 수 있는 명령어도 있다.

```shell
$ kubectl describe pods
Name:         kubernetes-bootcamp-57978f5f5d-v7ckv
Namespace:    default
Priority:     0
Node:         minikube/192.168.49.2
...
```

이 명령어를 사용하면 파드의 세부정보를 확인할 수 있다. (IP주소, 포트, 살고있는 노드, 파드의 상태 등등)





### 외부에서 파드 통신하기

파드는 고립된 개인 네트워크를 사용하여 실행되기 때문에 파드와 클러스터 외부에서 통신하기 위해서는 프록시가 필요하다. 그래서 `kubectl proxy` 명령어를 사용하여 프록시를 만들어서 파드와 통신할 수 있다. 



### 파드 로그 확인

애플리케이션이 STDOUT으로 전송한 모든 결과들은 파드 내의 컨테이너의 로그로 남는다. 이 로그를 볼 수 있는 명령어가 있다.

```shell
$ kubectl logs $POD_NAME
Kubernetes Bootcamp App Started At: 2021-10-01T08:18:05.821Z | Running On:  kubernetes-bootcamp-57978f5f5d-72dmd 
```



### 파드 내 컨테이너 접속

파드가 실행되고 있다면 컨테이너에서 직접 명령어를 실행시킬 수 있다. 파드 내에서 명령어를 실행시키려면 `exec` 명령어를 사용하면 된다. 

```shell
$ kubectl exec $POD_NAME -- env

PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
HOSTNAME=kubernetes-bootcamp-57978f5f5d-72dmd
KUBERNETES_PORT_443_TCP_PORT=443
...
```

도커 처럼 컨테이너 내의 bash에 접속할 수도 있다.

```shell
$ kubectl exec -it $POD_NAME -- bash
```





## 서비스

파드는 생명 주기를 가지는데, 워커노드가 죽으면 노드 상에서 동작하는 파드들 또한 종료된다. `레플리카셋` 으로 애플리케이션이 지속적으로 동작할 수 있도록 파드가 죽으면 새로운 파드들을 생성하여 동적으로 미리 지정해둔 상태로 복원시켜줄 수도 있다. 하지만 파드가 죽고 다른 파드가 새로 생성되면 이전에 죽은 파드와 새로 생긴 파드의 IP는 다른 IP가 될 수 있다. 그렇게 된다면 기존의 파드와 통신하고 있는 곳에서 문제가 발생할 수 있다. (IP가 변경되므로) 그래서 애플리케이션들이 지속적으로 서비스를 하려면 파드들 속에서 발생하는 변화에 대해 자동으로 조정해 줄 방법이 있어야 한다.

쿠버네티스에서 서비스는 하나의 논리적인 파드 셋과 그 파드들에 접근할 수 있는 정책을 정의하는 추상적 개념이다. 서비스는 종속적인 파드들 사이를 느슨하게 결합되도록 해준다. 그리고 서비스는 YAML이나 JSON을 이용하여 정의된다. 서비스가 대상으로 하는 파드 셋은 보통 `라벨 셀렉터`에 의해 결정된다.

그리고 파드들이 고유 IP를 가지지만 그 IP들은 단독으로 클러스터 외부로 노출될 수 없다. 서비스는 파드들이 클러스터 외부로 노출될 수 있도록 해준다. 서비스는 `ServiceSpec` 에서 `type`을 지정함으로써 다양한 방식으로 파드를 노출시킬 수 있다.

* **ClusterIP**(default): 클러스터 내에서 내부 IP에 대해 서비스를 노출해준다. 이 방식은 오직 클러스터 내에만 서비스를 접근할 수 있게 해준다.
* **NodePort**: NAT이 이용되는 클러스터 내에서 각각 선택된 노드들의 동일한 포트에 서비스를 노출시켜준다. `<NodeIP>:<NodePort>` 를 이용하여 클러스터 외부로부터 서비스가 접근할 수 있도록 해준다.
* **LoadBalancer**: 기존 클라우드에서 외부용 로드밸런서를 생성하고 서비스에 고정된 공인 IP를 할당해준다.
* **ExternalName**: `CNAME` 레코드 및 값을 반환함으로써 서비스를 `externalName` 필드의 내용에 매핑한다. 이 방식은 어떠한 종류의 프록시도 설정되지 않는다.



### 서비스와 레이블

서비스는 파드 셋에 걸쳐서 트래픽을 라우트한다. 애플리케이션에 영향을 주지 않으면서 쿠버네티스에서 파드들이 죽게도 하고 복제도 되게 해주는 추상적 개념이다. 서비스 내 종속적인 파드들 사이에서의 디스커버리와 라우팅은 쿠버네티스 서비스들에 의해 처리된다.

서비스는 쿠버네티스의 객체들에 대해 논리 연산을 허용해주는 기본 그루핑 단위인 `레이블` 과 `셀렉터` 를 이용하여 파드 셋과 매치시킨다. 레이블은 오브젝트들에 붙여진 key-value 쌍으로 다양한 방식으로 이용 가능하다.

* 개발, 테스트, 상용 환경에 대한 객체들의 지정
* 임베디드된 버전 태그
* 태그들을 이용하는 객체들에 대한 분류



### 서비스 조회

서비스를 조회하는 명령어는 다음과 같다.

```shell
$ kubectl get services
NAME         TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP   4h3m
```

결과를 보면 생성되어있는 서비스의 이름이 `kubernetes` 인 서비스가 있다. 이 이름의 서비스는 minikube가 클러스터를 만들 때 기본으로 생성한 서비스이다. 



### 서비스 생성-노출

새로운 서비스를 생성하고 외부로 노출시켜보자. 외부로 노출시키기 위해서는 `NodePort` 타입으로 노출시켜야 한다. (minikube는 LoadBalancer 옵션을 제공하지 않는다.)

```shell
$ kubectl expose deployment/kubernetes-bootcamp --type="NodePort" --port 8080

service/kubernetes-bootcamp exposed
```

다시 서비스를 조회하면 아래와 같이 결과가 나온다.

```shell
$ kubectl get services
NAME                  TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
kubernetes            ClusterIP   10.96.0.1      <none>        443/TCP          4h6m
kubernetes-bootcamp   NodePort    10.101.95.82   <none>        8080:30828/TCP   4s
```

새로 생성된 서비스를 보면 서비스에 고유한 ClusterIP가 할당되어있다. 



### 서비스 상세정보 보기

서비스가 할당받은 고유 Cluster IP, Internal Port, External IP등을 볼 수 있다. 

```shell
$ kubectl describe services/kubernetes-bootcamp
Name:                     kubernetes-bootcamp
Namespace:                default
Labels:                   app=kubernetes-bootcamp
Annotations:              <none>
Selector:                 app=kubernetes-bootcamp
Type:                     NodePort
IP Family Policy:         SingleStack
IP Families:              IPv4
IP:                       10.101.95.82
IPs:                      10.101.95.82
Port:                     <unset>  8080/TCP
TargetPort:               8080/TCP
NodePort:                 <unset>  30828/TCP
Endpoints:                172.17.0.4:8080
Session Affinity:         None
External Traffic Policy:  Cluster
Events:                   <none>
```









