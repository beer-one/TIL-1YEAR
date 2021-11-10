# Bare-metal 환경에서 고려해야할 점

K8S에서 애플리케이션을 외부로 노출시키기 위해서는 서비스가 필요하며, 클러스터 외부로 노출시키려면 서비스 타입을 `NodePort` 나 `LoadBalancer` 로 설정해야 한다. 클라우드 프로바이더가 제공되는 환경에서는 `LoadBalancer` 타입을 사용할 수 있지만 순수 베어메탈 환경에서는 제공되는 로드밸런서 가 없기 때문에 `LoadBalancer` 타입을 지정해봤자 `EXTERNAL-IP` 가 무한히 `pending` 상태로 남아 사용이 불가능할 것이다.

```shell
$ kubectl get svc
NAME              TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
gateway           LoadBalancer   10.100.245.184   <pending>     80:31548/TCP   19h
```



그러면 베어메탈에서 애플리케이션을 외부로 노출시키려면 어떻게 해야할까?



## NodePort 사용

당연히 가장 간단한 방법으로는 NodePort를 사용하는 것이다. 하지만 기본적으로 NodePort로 지정했을 경우 포트는 30000~32767 까지만 사용할 수 있다. 그리고 클러스터 내부에서는 80 / 443 포트를 통해 서비스와 통신할 수 있지만 외부 클라이언트는 `{NodeIP}:{NodePort}` 로 통신해야 한다. (URL에 NodePort를 무조건 붙여야 한다.) 

![NodePort request flow](https://kubernetes.github.io/ingress-nginx/images/baremetal/nodeport.jpg)

NodePort를 사용하면 몇 가지 불편한 점 (문제점) 이 있는데 하나는 호스트에 포트를 무조건 붙여야 한다는 점이다.

(`myapp.example.com` -> `http://myapp.example.com:30100`)

> `--service-node-port-range` 플레그를 변경하여 NodePort를 노출시킬 수 있는 범위를 조정한 뒤 80 / 443 포트를 바인딩 할 수는 있지만, 그렇게 구성을 변경하면 시스템 데몬에 예약되지 않은 포트 사용 및 필요하지 않을 수도 있는 kube-proxy 권한을 부여해야 하는 필요성을 포함하여 예상치 못한 문제가 생길 수도 있다. 따라서 이 방법은 별로 권장하는 방법은 아니다.



두 번째로는 NodePort 타입의 서비스는 기본적으로 source address 변환을 수행한다. 즉, HTTP 요청의 source IP는 항상 nginx 관점에서 요청을 수신한 쿠버네티스 노드의 IP 주소이다.

> 이 방법을 해결하기 위해서는 ingress-nginx 서비스의 `externalTrafficPolicy` 필드를 `Local` 로 설정하면 된다.



세 번째로는 NodePort 서비스는 정의에 의해 할당된 `LoadBalancerIP` 를 얻지 못하기 때문에 Nginx Ingress controller는 컨트롤러가 관리하는 인그레스 오브젝트의 상태를 업데이트하지 않는다. Nginx Ingress에게 Public IP 주소를 제공하는 로드밸런서가 없다는 사실에도 불구하고 `ingress-nginx` 서비스의 `externalIPs` 필드를 설정하여 관리되는 모든 인그레스 객체의 상태 업데이트를 강제할 수는 있다. (수동으로?)



마지막으로는 Nginx는 NodePort 서비스에서 운영하는 포트 변환을 인식하지 못하기 때문에 백엔드 에플리케이션은 NodePort를 포함하여 외부 클라이언트에서 사용하는 URL을 고려하는 리다이렉션 URL을 생성해야 한다.



## Using a self-provisioned edge

클라우드 환경과 비슷하게 쿠버네티스에 클러스터에 대한 공개 진입점을 제공하는 엣지 네트워크를 사용하여 해결할 수도 있다. 엣지 컴포넌트는 하드웨어(Vendor appliance)가 될 수도 있고 소프트웨어(HAproxy)가 될 수도 있다. 그리고 엣지 컴포넌트는 운영자에 의해 쿠버네티스 환경 밖에서 관리된다.

이렇게 구축하는 방식은 NodePort 서비스를 기반으로 하며  외부 클라이언트는 클러스터 노드를 직접 접근할 수 없고, 오직 엣지 컴포넌트를 통해 접근이 가능하다는 한 가지 중요한 차이점이 있다.  이는 노드에 Public IP 주소가 없는 개인 쿠버네티스 클러스터에 특히 적합하다.



![User edge](https://kubernetes.github.io/ingress-nginx/images/baremetal/user_edge.jpg)



엣지 컴포넌트는 모든 HTTP 트래픽을 쿠버네티스 환경으로 전달하며, 이는 Public IP 주소가 있어야 한다. 엣지 컴포넌트의 80 / 443 포트로 들어오는 모든 트래픽은 이에 대응하는 NodePort 서비스로 전달되는 구조이다.



## MetalLB

MetalLB는 클라우드 프로바이더를 지원하지 않는 쿠버네티스 클러스터 환경을 위한 네트워크 로드밸런서 구현체를 제공한다. 그래서 MetalLB를 사용하면 모든 클러스터 내에서 로드밸런서 서비스를 효과적으로 사용할 수 있다.

![MetalLB in L2 mode](https://kubernetes.github.io/ingress-nginx/images/baremetal/metallb.jpg)



위의 이미지는 Layer 2 mode를 사용하는 MetalLB를 Nginx Ingress Controller와 함께 사용하는 방법을 보여준다. 이 모드에서는 하나의 노드가 인그레스 서비스 IP에 대한 모든 트래픽을 끌어들인다. 

그리고 MetalLB는 쿠버네티스 메니페스트를 이용하거나 Helm을 이용하여 배포가 가능하다. 

MetalLB는 인그레스 서비스의 소유권을 얻기 위해 IP 주소 풀이 필요하다. 이 풀은 ConfigMap을 통해 구성이 가능하다. 해당 IP 풀은 MetalLB 전용이야 하며 쿠버네티스 노드 IP 또는 DHCP 서버에서 전달한 IP를 재사용할 수 없다.











































