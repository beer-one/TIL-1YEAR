# Calicoctl

calicoctl은 칼리코를 관리하기 위한 CLI이다. calicoctl을 통해 칼리코의 많은 기능을 사용할 수 있으며 칼리코 정책과 구성을 관리하기 위해 사용하며 세부적인 클러스터 상태를 보기 위해 사용하기도 한다.



## 개념

### API Group

모든 쿠버네티스 리소스는 API group에 속해있는다. API group은 리소스의 `apiVersion` 으로 표시된다. calico에서는 구성을 위해 `projectcalico.org/v3` API group의 리소스를 사용하며 오퍼레이터는 `operator.tigera.io/v1` API group의 리소스를 사용한다.



### calicoctl, kubectl

`projectcalico.org/v3` API group의 칼리코 API를 관리하기 위해서는 `calicoctl` 을 사용해야 한다. `kubectl` 에서는 사용할 수 없는 리소스의 중요한 검증과 기본값 설정을 `calicoctl` 에서 제공해주기 때문이다. 하지만, `kubectl` 은 다른 쿠버네티스 리소스를 관리하기 위해서는 필요하다.



### Datastore

Calico 오브젝트는 `etcd` 또는 `kubernetes`의 두가지 데이터 저장소 중 하나에 저장된다. 데이터 저장소 선택은 칼리코가 설치되는 시점에 결정된다. 일반적으로 kubernetes 설치의 경우 kubernetes 데이터 저장소가 기본값이다.

