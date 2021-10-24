# Cluster

K8s는 컨테이너를 파드 내에 배치하고 노드에서 실행하여 워크로드를 구동한다. 여기서 **노드**는 클러스터에 따라 가상머신이 될 수도 있고 물리머신이 될 수도 있다. 각 노드는 `컨트롤 플레인` 에 의해 관리되며 파드를 실행하는데 필요한 서비스를 포함한다. **컨트롤 플레인 노드** 는 etcd와 API Server를 포함한 컨트롤 플레인 컴포넌트가 실행되는 머신이다.

일반적으로 클러스터를 관리할 때 여러 개의 노드를 둔다. 하지만 학습할 때는 머신이 하나뿐이면 하나만 써도 가능하긴 하다..

노드의 컴포넌트에는 kubelet, 컨테이너 런타임, kube-proxy가 포함된다.



## 클러스터 설치

쿠버네티스 클러스터는 물리머신과 가상머신 모두 설치할 수 있다. 쿠버네티스를 설치하는 방법은 여러가지가 있는데 **minikube** 를 설치하는 방식과 **kubeadm** 을 직접 설치하는 방식이 있다. minikube는 가벼운 쿠버네티스 구현체이며, 로컬 머신에 VM을 만들고 하나의 노드로 구성된 간단한 클러스터를 생성한다. minikube CLI는 클러스터에 대해 시작, 중지, 상태 조회 및 삭제 등의 기본적인 부트스트래핑 기능을 제공한다.



### kubeadm 설치 (리눅스)

kubeadm을 설치해보자. 사실 클러스터를 구축하기 위해서는 kubeadm 뿐 아니라 다른 것도 설치해야 한다.

* kubeadm: 클러스터를 부트스트랩하는 명령어이다.
* kubelet: 클러스터의 모든 머신에서 실행되는 파드와 컨테이너 시작과 같은 작업을 수행하는 컴포넌트이다.
* kubectl: 클러스터와 통신하기 위한 커맨드라인 유틸리티이다.

참고로 **kubeadm** 은 `kubelet`과 `kubectl`을 설치하거나 관리하지 않기 때문에 이 둘은 kubeadm이 설치하려는 쿠버네티스 컨트롤 플레인의 버전과 일치하는지 확인해야 한다. 그렇지 않으면 버그가 발생할 수도 있다. 

kubeadm, kubelet, kubectl 설치 방법은 아래와 같다.

```shell
# apt 패키지 색인 업데이트, k8s apt 리포지터리 패키지 설치
$ sudo apt-get update
$ sudo apt-get install -y apt-transport-https ca-certificates curl

# gcloud 공개 사이닝 키 설치
$ sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg

# k8s apt 리포지터리 추가, apt 패키지 색인 업데이트
$ echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
$ sudo apt-get update

# kubelet, kubectl, kubeadm 설치 및 해당 버전 고정
$ sudo apt-get install -y kubelet kubeadm kubectl
$ sudo apt-mark hold kubelet kubeadm kubectl
```





### 도커 설치 (컨테이너 런타임)

도커를 설치하는 방법은 아래와 같다. 노드를 초기화하기 전에 설치해야 한다.

```shell
$ sudo apt update
$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
$ sudo add-apt-repository \
"deb [arch=amd64] https://download.docker.com/linux/ubuntu \
$(lsb_release -cs) \
stable"
$ sudo apt-get update && sudo apt-get install docker-ce docker-ce-cli containerd.io -y
$ sudo systemctl enable docker && sudo service docker start
$ sudo usermod -a -G docker $USER
```

도커를 설치했다면 컨테이너의 cgroup 관리에 systemd를 사용하도록 도커 데몬을 구성한다.

```shell
$ sudo mkdir /etc/docker
$ cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF
```

그 후 도커를 재시작한다.

```shell
$ sudo systemctl enable docker
$ sudo systemctl daemon-reload
$ sudo systemctl restart docker
```





### 컨트롤 플레인 노드 초기화

kubeadm을 설치했다면 컨트롤 플레인 노드를 초기화하는 방법을 알아보자.

1. 하나의 컨트롤 플레인 kubeadm 클러스터를 고가용성으로 업그레이드 할 계획이 있다면 공유 엔드포인트를 설정하기 위해  모든 컨트롤 플레인 노드에 대해 `--control-plane-endpoint`를 지정해야 한다. 이러한 엔드포인트는 DNS 이름이나 로드밸런서의 IP 주소가 될 수 있다.
2. 파드 네트워크 애드온을 선택하고 `kubeadm init` 명령어에 전달되는 인자가 필요한지 확인한다. 선택한 서드파티 프로바이더에 따라 `--pod-network-cidr` 을 프로바이더가 지정한 값으로 설정이 필요할 수도 있다.
3. 버전 1.14부터 kubeadm은 잘 알려진 도메인 소켓 경로 리스트를 사용하여 리눅스에서의 런타임 컨테이너를 감지하려고 한다. 다른 컨테이너 런타임을 사용하거나 프로비저닝된 노드에 두 개 이상의 컨테이너가 설치된 경우 kubeadm init에 `--cri-socket` 인수를 지정한다. *(Optional)*
4. 따로 지정하지 않는 한 kubeadm은 기본 게이트웨이와 연결된 네트워크 인터페이스를 사용하여 특정 컨트롤 플레인 노드의 API 서버에 대한 advertise address를 설정한다. 다른 네트워크 인터페이스를 사용하고 싶다면`kubeadm init`에  `--apiserver-advertise-address=<ip-address>` 를 인자로 지정하자. *(Optional)*
5. gcr.io 컨테이너 이미지 레지스트리와의 연결을 확인하기 위해 `kubeadm init` 이전에 `kubeadm config images pull` 명령어를 사용하자. *(Optional)*



간단하게 컨트롤플레인 노드를 생성하는 방법은 init만 하면 된다. 네트워크를 flannel으로 두려면 `--pod-network-cidr=10.244.0.0/16` 플래그를 추가해야 한다.

```shell
$ sudo kubeadm init
---
$ sudo kubeadm init --pod-network-cidr=10.244.0.0/16
```



그럼 아래와 같은 문구가 뜬다.

```shell
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

You should now deploy a Pod network to the cluster.
Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
  /docs/concepts/cluster-administration/addons/

You can now join any number of machines by running the following on each node
as root:

  kubeadm join <control-plane-host>:<control-plane-port> --token <token> --discovery-token-ca-cert-hash sha256:<hash>
```

일단 시키는대로 다음 명령어를 사용하자.

```shell
$ mkdir -p $HOME/.kube
$ sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
$ sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

그리고 파드 네트워크를 꾸릴 [CNCF](https://kubernetes.io/ko/docs/concepts/cluster-administration/addons/)를 하나 선택해서 적용시키자. 일단 [flannel](https://github.com/flannel-io/flannel#deploying-flannel-manually) 을 선택하여 적용시켜보겠다.

```shell
$ kubectl apply -f [podnetwork].yaml
$ kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```

 

그리고 마지막 명령어는 k8s 워커 노드를 컨트롤 플레인 노드에 조인하기 위해 `token`, `discovery-token-ca-cert-hash` 를 사용하는데, 나중에 노드들을 조인하려면 이 값을 기억하고 있어야 한다.

```shell
You can now join any number of machines by running the following on each node
as root:

  kubeadm join <control-plane-host>:<control-plane-port> --token <token> --discovery-token-ca-cert-hash sha256:<hash>
```



물론 토큰을 조회할 수도 있다.

```shell
$ kubeadm token list
```



그리고 해당 파일로 클러스터 정보를 볼 수 있는데 루트계정만 볼 수 있다.

```shell
$ sudo vi /etc/kubernetes/admin.conf
```

그리고 `discovery-token-ca-cert-hash` 를 조회하려면 아래 명령어를 입력하면 된다.

```shell
$ openssl x509 -pubkey -in /etc/kubernetes/pki/ca.crt | openssl rsa -pubin -outform der 2>/dev/null | \
   openssl dgst -sha256 -hex | sed 's/^.* //'
```



그런데 토큰은 24시간이 지나면 만료된다. 그래서 추가로 노드를 조인할 일이 생긴다면 토큰을 별도로 생성해야 한다.

```shell
$ kubeadm token create
```





### Control plan node isolation

기본적으로, 클러스터는 보안상의 이유로 컨트롤 플레인 노드에 파드를 스케줄링 하지 않는다. 만약 컨트롤 플레인 노드에 파드를 스케줄링 하고 싶다면 다음 명령어를 실행하면 된다.

```shell
$ kubectl taint nodes --all node-role.kubernetes.io/master-
```



### 노드 조인

노드는 워크로드가 실행되는 곳이다. 클러스터에 새 노드를 조인하려면 각 머신에 대해 다음을 수행해야 한다.

* SSH

* 루트계정으로 (`sudo su -`)

* 필요하다면 컨테이너 런타임 설치

* Kubeadm init에서 출력된 명령 실행

  ```shell
  $ kubeadm join <control-plane-host>:<control-plane-port> --token <token> --discovery-token-ca-cert-hash sha256:<hash>
  ```




조인에 성공하면 다음과 같이 출력될 것이다.

```shell
This node has joined the cluster:
* Certificate signing request was sent to apiserver and a response was received.
* The Kubelet was informed of the new secure connection details.

Run 'kubectl get nodes' on the control-plane to see this node join the cluster.
```



컨트롤 플레인 노드에서 노드 조회 명령어를 입력하면 조인한 노드도 같이 조회될 것이다.

```shell
kube-cp:~$ kubectl get nodes
NAME         STATUS   ROLES                  AGE   VERSION
kube-cp      Ready    control-plane,master   37m   v1.22.2
kube-node1   Ready    <none>                 44s   v1.22.2
```





### apiserver-advertise-address, ControlPlaneEndpoint

`--apiserver-advertise-address` 플래그는 특정 컨트롤 플래인 노드의 API server로 advertise address를 설정하기 위해 사용될 수 있고 `--control-plane-endpoint` 플래그는 모든 컨트롤 플레인 노드에 대해 공유 엔드포인트를 설정하기 위해 사용될 수 있다.

`--control-plane-endpoint` 플래그는 IP주소에 매핑할 수 있는 IP 주소와 DNS 이름 모두 허용한다. 매핑 예시는 다음과 같다.

```
192.168.0.102 cluster-endpoint
```

`192.168.0.102` 는 노드의 IP주소이고 `cluster-endpoint` 는 IP와 매핑되는 커스텀 DNS 이름이다. 





### 컨트롤 플레인 노드가 아닌 다른 머신에서 클러스터를 제어하는 방법

클러스터가 아닌 다른 머신에서 클러스터를 제어하기 위해서는 컨트롤 플레인 노드에 있는 `administrator kubeconfig` 파일을 클러스터를 제어하려는 다른 머신에 복사하는 것이 필요하다.

```shell
$ scp root@<control-plane-host>:/etc/kubernetes/admin.conf .
kubectl --kubeconfig ./admin.conf get nodes
```





## 클러스터 제거

클러스터에 일회용 서버를 사용한 경우, 테스트를 위해 서버를 끄고 자원을 정리하지 않을 수 있다. `kubectl config delete-cluster` 명령어를 사용하여 클러스터에 대한 로컬 참조를 삭제할 수 있다.

그러나, 클러스터를 조금 더 깨끗하게 프로비저닝을 해지하려면 먼저 노드를 drain하고 노드가 비어있는지 확인한 후 노드 구성을 해제해야 한다.

### 노드 제거

먼저 **컨트롤 플레인 노드**에서 다음 명령어를 실행한다. 

```shell
$ kubectl drain <node name> --delete-emptydir-data --force --ignore-daemonsets
```

Drain 되는 노드는 스케줄링이 허용되지 않는다.

```shell
$ kubectl get nodes
NAME         STATUS                     ROLES                  AGE   VERSION
kube-cp      Ready                      control-plane,master   79m   v1.22.2
kube-node1   Ready,SchedulingDisabled   <none>                 42m   v1.22.2
```



노드를 지우기 전에 **대상 노드**에서 kubeadm을 리셋한다.

```shell
$ kubeadm reset
```

reset 프로세스는 iptable rule 또는 IPVS 테이블을 리셋하거나 정리하지 않는다. iptable을 정리하고 싶다면 다음 명령어를 입력하자.

```shell
$ iptables -F && iptables -t nat -F && iptables -t mangle -F && iptables -X
```

IPVS 테이블을 리셋하고 싶다면 아래 명령어를 입력하자.

```shell
ipvsadm -C
```

reset이 완료되었다면 **컨트롤 플레인 노드** 에서 대상 노드를 삭제하자.

```shell
$ kubectl delete node <node name>
```



### 컨트롤 플레인 노드 제거

컨트롤 플레인 노드를 제거하는 방법은 일단 모든 노드를 제거 한 후 컨트롤 플레인에서 kubeadm을 리셋하면 된다.

```shell
$ kubeadm reset
$ rm -rf ~/.kube
```












### 설치 트러블슈팅

1. 스왑 지원 안함.

```shell
sudo kubeadm init
 
[init] Using Kubernetes version: v1.22.2
[preflight] Running pre-flight checks
error execution phase preflight: [preflight] Some fatal errors occurred:
	[ERROR Swap]: running with swap on is not supported. Please disable swap
[preflight] If you know what you are doing, you can make a check non-fatal with `--ignore-preflight-errors=...`
To see the stack trace of this error execute with --v=5 or higher
```

* k8s는 메모리 스왑을 고려하지 않고 설계했기 때문에 클러스터 노드로 사용할 서버 머신들은 모두 스왑메모리를 비활성화 해줘야 한다.

* 비활성화 방법:

  ```shell
  $ sudo swapoff -a
  $ sudo sed -i '/swap/d' /etc/fstab
  ```



2. Preflight error

kubeadm init을 한 후 어떠한 이유로 실패하고 다시 init을 하면 포트 사용중이라고 에러가 뜰 때가 있는데 이는 이전에 실패했을 때 preflight 과정에서 특정 프로세스가 띄워진 상태이고, 실패할 때 프로세스 정리를 하지 않기 때문이다.

![image-20211005172432786](/Users/nhn/Library/Application Support/typora-user-images/image-20211005172432786.png)

이런 경우 kubeadm을 리셋한 후 다시 시작하면 된다.

```shell
$ sudo kubeadm reset
$ sudo kubeadm init
```

