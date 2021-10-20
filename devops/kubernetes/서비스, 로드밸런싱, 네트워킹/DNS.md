# DNS

K8s는 파드와 서비스를 위한 DNS 레코드를 생성하며 사용자는 IP 주소 대신에 DNS이름을 통해 서비스에 접속할 수 있다.

클러스터 내 모든 서비스에는 DNS 이름이 할당된다. 기본적으로 클라이언트 파드의 DNS 검색 리스트는 파드 자체의 네임스페이스와 클러스터 기본 도메인을 포함한다.



## 서비스 DNS

헤드리스(클러스터 IP가 없는 서비스) 가 아닌 서비스는 서비스 IP 계열에 따라 `<서비스이름>.<namespace이름>.svc.cluster.local` 형식의 이름을 가진 DNS A 또는 AAAA 레코드가 할당된다. 이는 서비스의 클러스터 IP로 해석된다.

물론 헤드리스 서비스도 위와 마찬가지로 레코드가 할당되는데 일반적인 서비스와 다르게 이는 서비스에 의해 선택된 파드들의 IP 집합으로 해석된다. 컨테이너가 서비스이름만 사용하는 경우, 네임스페이스 내에 국한된 서비스로 연결된다 (default)



### DNS 확인

curl 애플리케이션을 실행하여 DNS를 확인할 수 있다.

```shell
$ kubectl run curl --image=radial/busyboxplus:curl -n <namespace> -i --tty 
```

* 특정 네임스페이스의 서비스 DNS를 확인하려면 -n은 필수이다.



위의 명령어를 친 후 curl 애플리케이션이 뜨는데 nslookup으로 확인할 수 있다.

```shell
If you don't see a command prompt, try pressing enter.
[ root@curl:/ ]$ nslookup <service-name>
Server:    10.96.0.10
Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local

Name:      <service-name>
Address 1: 10.97.236.165 <service-name>.<namespace>.svc.cluster.local
```

