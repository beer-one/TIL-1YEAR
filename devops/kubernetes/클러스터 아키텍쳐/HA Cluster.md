# 고가용성 클러스터 구성

kubeadm으로 고가용성 쿠버네티스 클러스터를 구성하는 방법에 대해 알아보자. 고가용성 쿠버네티스 클러스터를 구성하는 방법은 두 가지가 있다.

* Stack형 컨트롤 플레인 노드: 적은 인프라스트럭쳐 자원이 요구된다. etcd 멤버와 컨트롤 플레인 노드가 같이 위치해있다.
* External etcd 클러스터: 많은 인프라스트럭쳐 자원이 요구된다. 컨트롤 플레인 노드와 etcd member가 분리되어있다.



## 구성하기 전에

* 3개 이상의 컨트롤플레인용 머신
* 3개 이상의 노드용 머신
* 해당 머신 모두 네트워크가 상호 연결되어있어야 함
* 머신 모두 sudo 권한
* 한 머신에서 다른 모든 머신에 대해 SSH 접근 가능
* `kubeadm`, `kubelet` 설치, `kubectl`은 옵션



External etcd 클러스터를 만드려면 아래 항목이 더 요구된다.

* etcd member용으로 3개의 추가 머신



## kube-apiserver용 LB 생성

먼저 DNS로 확인되는 이름을 가진 kube-apiserver용 로드밸런서를 생성한다.

* 클라우드 환경에서는 컨트롤 플레인 노드를 TCP 포워딩 로드밸런서 뒤에 배치해야 한다. 로드밸런서는 타겟 리스트 내 모든 건강한 컨트롤 플레인으로 가는 트래픽을 분산시킨다. apiserver로의 헬스체크는 kube-apiserver가 듣는 포트로 향하는 TCP 체크이다.
* 클라우드 환경에서는 IP 주소를 직접 사용하지 않는 것이 좋다.

* 로드밸런서는 모든 컨트롤플레인 노드의 apiserver 포트와 통신이 되어야 하고 listening port로 들어오는 트래픽을 모두 허용해야 한다.
* 로드밸런서의 주소는 kubeadm의 `ControlPlaneEndpoint` 의 주소와 일치해야 한다.



```shell
$ sudo apt-get install haproxy
```



그 후 `/etc/haproxy/haproxy.cfg` 에 haproxy 설정정보를 추가한다.

```shell
$ vi /etc/haproxy/haproxy.cfg
```

```cfg
#---------------------------------------------------------------------
# Global settings
#---------------------------------------------------------------------
global
    log /dev/log local0
    log /dev/log local1 notice
    daemon

#---------------------------------------------------------------------
# common defaults that all the 'listen' and 'backend' sections will
# use if not designated in their block
#---------------------------------------------------------------------
defaults
    mode                    http
    log                     global
    option                  httplog
    option                  dontlognull
    option http-server-close
    option forwardfor       except 127.0.0.0/8
    option                  redispatch
    retries                 1
    timeout http-request    10s
    timeout queue           20s
    timeout connect         5s
    timeout client          20s
    timeout server          20s
    timeout http-keep-alive 10s
    timeout check           10s

#---------------------------------------------------------------------
# apiserver frontend which proxys to the control plane nodes
#---------------------------------------------------------------------
frontend apiserver
    bind *:${APISERVER_DEST_PORT}
    mode tcp
    option tcplog
    default_backend apiserver

#---------------------------------------------------------------------
# round robin balancing for apiserver
#---------------------------------------------------------------------
backend apiserver
    option httpchk GET /healthz
    http-check expect status 200
    mode tcp
    option ssl-hello-chk
    balance     roundrobin
        server ${HOST1_ID} ${HOST1_ADDRESS}:${APISERVER_SRC_PORT} check
        # [...]
```

* `${APISERVER_DEST_PORT}` : K8S가 API Server와 통신할 포트
* `${APISERVER_SRC_PORT}` : API Server 인스턴스에서 사용하는 포트
* `${HOST1_ID}` : 처음 로드밸런스되는 API Server 호스트의 심볼릭 이름
* `${HOST1_ADDRESS}` : 처음 로드밸런스 되는 API Server 호스트에 대한 확인 가능한 주소 (DNS, IP주소 등..)
* `server` 라인 밑에서 추가적으로 로드밸런싱할 서버정보를 추가할 수 있다.



그리고 첫 번째 컨트롤 플레인 노드에 로드밸런서를 추가하고 연결을 테스트한다.

```shell
$ sudo systemctl restart haproxy 
$ sudo systemctl enable haproxy
```

```shell
$ nc -v ${LOAD_BALANCER_IP} ${PORT}
Connection to ${LOAD_BALANCER_IP} ${PORT} port [tcp/*] succeeded!
```

* connection refused 에러가 만약 발생한다면 apiserver가 아직 실행되지 않아서 발생하는 것이다.
* timeout이 발생한다면 컨트롤플레인 노드와 통신을 할 수 없다는 뜻이기 때문에 네트워크 설정을 확인해봐야 한다.



그 후 컨트롤플레인 노드가 추가된다면 추가된 컨트롤플레인도 로드밸런싱 대상에 포함시키면 된다.



### 스택형 컨트롤플레인, etcd node

먼저 컨트롤 플레인은 설치한다.

```shell
$ sudo kubeadm init --control-plane-endpoint ${LOAD_BALANCER_DNS}:${LOAD_BALANCER_PORT} --upload-certs
```

* `--control-plane-endpoint` : 로드밸런서의 DNS(IP주소):Port 로 설정해야 한다.
* `--upload-certs` : 클러스터에서 모든 컨트롤 플레인 인스턴스끼리 공유해야하는 인증서를 업로드할 때 해당 플래그를 사용한다. 이를 사용하지 않고 직접 수동적으로 컨트롤플레인 노드에 인증서를 복사하거나 자동 툴을 사용하는 경우에는 해당 플래그를 사용하지 않아도 된다.



설치가 완료되었다면 아래와 같이 화면에 출력될 것이다.

```shell
...
You can now join any number of the control-plane node running the following command on each as root:

  kubeadm join ${LOAD_BALANCER_DNS}:${LOAD_BALANCER_PORT} --token ${TOKEN} \
	--discovery-token-ca-cert-hash sha256:${DISCOVERY_TOKEN_CA_CERT_HASH} \
	--control-plane --certificate-key ${CERTIFICATE_KEY}

Please note that the certificate-key gives access to cluster sensitive data, keep it secret!
As a safeguard, uploaded-certs will be deleted in two hours; If necessary, you can use
"kubeadm init phase upload-certs --upload-certs" to reload certs afterward.

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join ${LOAD_BALANCER_DNS}:${LOAD_BALANCER_PORT} --token ${TOKEN} \
	--discovery-token-ca-cert-hash sha256:${DISCOVERY_TOKEN_CA_CERT_HASH}
```

* 첫 번째 명령어로 컨트롤플레인 노드를 추가로 붙일 수 있다. (`--certificate-key` 추가)

* 두 번째 명령어로 일반 노드를 추가로 붙일 수 있다.

* `--upload-certs`가 `kubeadm init` 와 함께 사용된다면 기본 컨트롤 플레인의 인증서가 암호화되어 `kubeadm-certs` 시크릿에 업로드된다.

* 인증서를 재 업로드하거나 새로운 decryption key를 생성하려고 한다면 컨트롤 플레인 노드에서 아래 명령어를 사용하면 된다.

  ```shell
  $ sudo kubeadm init phase upload-certs --upload-certs
  ```

* 나중에 조인해서 사용할 수 있는 사용자 지정 `--certificate-key` 를 초기화 중에 지정할 수도 있다.

  ```shell
  $ kubeadm certs certificate-key
  ```



그 후 CNI 플러그인을 적용하자. CNI를 선택할 때는 설정한 Pod CIDR에 일치하는 CNI를 선택해야 한다. (일단 예시는 flannel로, flannel은 cidr = 10.244.0.0/16)

```shell
$ kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
```



이제 클러스터 환경이 잘 돌아가는지 확인하기 위해 pod를 조회해보자. 아래 항목 모두 running이면 잘 돌아가는 것이다.

```shell
$ kubectl get po -n kube-system -w
NAME                                        READY   STATUS    RESTARTS   AGE
coredns-78fcd69978-hgrnj                    1/1     Running   0          18m
coredns-78fcd69978-wmtmn                    1/1     Running   0          18m
etcd-k8s-sandbox-sw-cp                      1/1     Running   1          18m
kube-apiserver-k8s-sandbox-sw-cp            1/1     Running   1          18m
kube-controller-manager-k8s-sandbox-sw-cp   1/1     Running   1          18m
kube-flannel-ds-whkhj                       1/1     Running   0          9m14s
kube-proxy-mxnd6                            1/1     Running   0          18m
kube-scheduler-k8s-sandbox-sw-cp            1/1     Running   1          18m
```



잘 돌아가는지 확인하였으면 위의 노드 join 명령어를 이용하여 컨트롤플레인 / 워커노드를 조인하자. 조인 후 새로운 컨트롤 플레인 노드에서 노드와 파드를 조회해보자.

```shell
$ kubectl get no
NAME                 STATUS   ROLES                  AGE    VERSION
k8s-sandbox-sw-cp    Ready    control-plane,master   23m    v1.22.2
k8s-sandbox-sw-cp2   Ready    control-plane,master   39s    v1.22.2
k8s-sandbox-sw-no1   Ready    <none>                 109s   v1.22.2
k8s-sandbox-sw-no2   Ready    <none>                 85s    v1.22.2
```

```shell
$ kubectl get po -n kube-system
NAME                                         READY   STATUS    RESTARTS      AGE
coredns-78fcd69978-hgrnj                     1/1     Running   0             23m
coredns-78fcd69978-wmtmn                     1/1     Running   0             23m
etcd-k8s-sandbox-sw-cp                       1/1     Running   1             23m
etcd-k8s-sandbox-sw-cp2                      1/1     Running   0             50s
kube-apiserver-k8s-sandbox-sw-cp             1/1     Running   1             23m
kube-apiserver-k8s-sandbox-sw-cp2            1/1     Running   0             53s
kube-controller-manager-k8s-sandbox-sw-cp    1/1     Running   2 (39s ago)   23m
kube-controller-manager-k8s-sandbox-sw-cp2   1/1     Running   0             54s
kube-flannel-ds-bvpg2                        1/1     Running   0             101s
kube-flannel-ds-cqxt6                        1/1     Running   0             2m5s
kube-flannel-ds-vf7fq                        1/1     Running   0             55s
kube-flannel-ds-whkhj                        1/1     Running   0             14m
kube-proxy-lwrlx                             1/1     Running   0             2m5s
kube-proxy-mxnd6                             1/1     Running   0             23m
kube-proxy-nt8lc                             1/1     Running   0             101s
kube-proxy-t48gq                             1/1     Running   0             55s
kube-scheduler-k8s-sandbox-sw-cp             1/1     Running   2 (39s ago)   23m
kube-scheduler-k8s-sandbox-sw-cp2            1/1     Running   0             53s
```



## External etcd nodes

external etcd node로 클러스터를 구성하는 방법은 위에서 했던 스택형 노드 클러스터 구성방법과 유사하다. 대신, 해당 클러스터를 구성하기 위해서는 etcd 설정을 먼저 해야 한다. 그리고 etcd 정보를 kubeadm config 파일에 전달해야 한다.



### etcd 클러스터 설치

kubeadm은 기본적으로 컨트롤 플레인 노드에서 kubelet에 의해 관리되는 static pod에서 단일 멤버 etcd cluster를 실행한다. 이러한 기본 설정은 etcd 클러스터에 오직 하나의 멤버만 포함되어있고 사용할 수 없게 되는 멤버를 유지할 수 없으므로 고가용성(HA) 설정이 아니다. 
