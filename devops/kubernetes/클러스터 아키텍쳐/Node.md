# Cluster & Node

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



### 컨트롤 플레인 노드 초기화

kubeadm을 설치했다면 컨트롤 플레인 노드를 초기화하는 방법을 알아보자.

1. 하나의 컨트롤 플레인 kubeadm 클러스터를 고가용성으로 업그레이드 할 계획이 있다면 공유 엔드포인트를 설정하기 위해  모든 컨트롤 플레인 노드에 대해 `--control-plane-endpoint`를 지정해야 한다. 이러한 엔드포인트는 DNS 이름이나 로드밸런서의 IP 주소가 될 수 있다.
2. 파드 네트워크 애드온을 선택하고 `kubeadm init` 명령어에 전달되는 인자가 필요한지 확인한다. 선택한 서드파티 프로바이더에 따라 `--pod-network-cidr` 을 프로바이더가 지정한 값으로 설정이 필요할 수도 있다.