# RBAC Authroization

RBAC 권한은 권한 인가 결정을 하기 위해 `rbac.authorization.k8s.io` API 그룹을 사용한다. 따라서 쿠버네티스 API를 통해 정책을 동적으로 구성할 수 있다.

RBAC를 활성화하기 위해서는 `--authorization-mode` 플래그에 RBAC를 설정함과 함께 API server를 시작하자.

```shell
kube-apiserver --authorization-mode=Example,RBAC --other-options --more-options 
```



## API 객체

RBAC API는 `Role`, `ClusterRole`, `RoleBinding`, `ClusterRoleBinding` 이라는 4가지의 오브젝트를 선언한다. 다른 쿠버네티스 오브젝트처럼 `kubectl` 과 같은 툴을 사용하여 해당 오브젝트를 추가, 조회, 조작할 수 있다.



## Role, ClusterRole

RBAC Role과 ClusterRole은 일련의 권한을 나타내는 규칙이 포함되어 있다. 

`Role`은 항상 특정 네임스페이스 내의 권한을 설정한다. `Role`을 생성할 때는 `Role`이 속한 네임스페이스를 지정해야한다. 반면에 `ClusterRole` 은 네임스페이스에 소속되지 않는 리소스이다. 쿠버네티스에서는 네임스페이스에 소속된 리소스와 소속되지 않는 리소스를 구분한다.

`ClusterRole` 은 여러가지 용도로 사용할 수 있는데 대표적으로 다음과 같이 사용한다.

1. 네임스페이스에 소속된 리소스에 대한 권한을 정의하고 개별 네임스페이스 내에 부여한다.
2. 네임스페이스에 소속된 리소스에 대한 권한을 정의하고 모든 네임스페이스에 권한을 부여한다.
3. 클러스터 범위의 리소스에 대한 권한을 정의한다.



네임스페이스 내의 역할을 정의하고 싶다면 `Role` 을 사용하고 클러스터 전반에서 역할을 정의하고 싶다면 `ClusterRole` 을 사용하면 된다.



### Role

다음은  `default` 네임스페이스의 파드에 대한 읽기 권한을 부여하는 `Role` 을 정의하는 예제이다. 

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: pod-reader
rules:
- apiGroups: [""] # "" indicates the core API group
  resources: ["pods"]
  verbs: ["get", "watch", "list"]
```



### ClusterRole

ClusterRole은 Role과 같은 권한을 부여하기 위해 사용된다. ClusterRole은 대신에 권한 부여 범위가 클러스터 전반이기 때문에 다음 항목에 대한 접근 권한을 부여하는 데 사용할 수도 있다.

* 클러스터 범위의 리소 (node 등)
* Non-resource endpoint (`/healthz` 등)
* 모든 네임스페이스 전반의 네임스페이스 소속 리소스 (pod 등, ex. `kubectl get po -A`)



다음은 특정 네임스페이스 또는 모든 네임스페이스 전반에서 시크릿에 대한 읽기 권한을 부여하는 ClusterRole의 예제이다. 네임스페이스 범위는 **바인딩**에 따라 다르다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  # "namespace" omitted since ClusterRoles are not namespaced
  name: secret-reader
rules:
- apiGroups: [""]
  #
  # at the HTTP level, the name of the resource for accessing Secret
  # objects is "secrets"
  resources: ["secrets"]
  verbs: ["get", "watch", "list"]
```



## RoleBinding, ClusterRoleBinding

RoleBinding은 user에게 Role에 정의된 권한을 부여한다. RoleBinding에는 `subjects`(users, groups 또는 service accounts) 목록과 부여되는 역할에 대한 참조가 포함된다. RoleBinding은 특정 네임스페이스에 대한 권한을 부여하지만 ClusterRoleBinding은 클러스터 전반에 걸친 접근 권한을 부여한다.

RoleBinding은 같은 네임스페이스에 있는 모든 Role을 참조한다. 또는 RoleBinding이 ClusterRole을 참조하고 해당 ClusterRole을 RoleBinding의 네임스페이스에 바인딩할 수 있다. 클러스터 내의 모든 네임스페이스에 ClusterRole을 바인딩 하기를 원한다면 ClusterRoleBinding을 사용하면 된다.



### RoleBinding

다음은 default namespace에서 "pod-reader" 라는 `Role` 을 "jane" 이라는 `user` 에게 권한을 부여하는 `RoleBinding` 을 정의하는 예시이다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
# This role binding allows "jane" to read pods in the "default" namespace.
# You need to already have a Role named "pod-reader" in that namespace.
kind: RoleBinding
metadata:
  name: read-pods
  namespace: default
subjects:
# You can specify more than one "subject"
- kind: User
  name: jane # "name" is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  # "roleRef" specifies the binding to a Role / ClusterRole
  kind: Role #this must be Role or ClusterRole
  name: pod-reader # this must match the name of the Role or ClusterRole you wish to bind to
  apiGroup: rbac.authorization.k8s.io
```



또, `Rolebinding`은 `RoleBinding`의 네임스페이스 내의 리소스에 대해 `ClusterRole`에서 정의한 권한을 부여하기 위해 `ClusterRole`을 참조할 수 있다. 이러한 종류의 참조를 사용하면 클러스터 전체에서 공통 역할 세트를 정의한 다음 여러 네임스페이스 내에서 재사용할 수 있다.

다음은 "dave" 라는 사용자에게 Secret을 읽을 수 있는 `ClusterRole` 을 부여하는 `RoleBinding` 에 대한 예시인데 RoleBinding이 "development" 네임스페이스에 있기 때문에 이 RoleBinding은 "development" 네임스페이스의 Secret에 대한 읽기 권한만을 부여한다. 

```yaml
apiVersion: rbac.authorization.k8s.io/v1
# This role binding allows "dave" to read secrets in the "development" namespace.
# You need to already have a ClusterRole named "secret-reader".
kind: RoleBinding
metadata:
  name: read-secrets
  #
  # The namespace of the RoleBinding determines where the permissions are granted.
  # This only grants permissions within the "development" namespace.
  namespace: development
subjects:
- kind: User
  name: dave # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io

```



### ClusterRoleBinding

클러스터 전반의 권한을 부여하기 위해서는 `ClusterRoleBinding` 을 사용할 수 있다. 

다음은 "manager" 그룹에 있는 사용자에게 모든 네임스페이스의 Secret을 읽을 수 있는 `ClusterRole` 을 부여하는 `ClusterRoleBinding` 을 정의하는 예시이다.

```yaml
apiVersion: rbac.authorization.k8s.io/v1
# This cluster role binding allows anyone in the "manager" group to read secrets in any namespace.
kind: ClusterRoleBinding
metadata:
  name: read-secrets-global
subjects:
- kind: Group
  name: manager # Name is case sensitive
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: secret-reader
  apiGroup: rbac.authorization.k8s.io
```



바인딩을 생성한 후에는 바인딩이 참조하는 Role이나 ClusterRole을 변경하면 안된다. 만약 바인딩의 `roleRef` 를 변경하려고 시도한다면 에러를 반환할 것이다. 만약 바인딩의  `roleRef` 를 변경하고 싶다면 바인딩 객체를 삭제한 후 다시 변경본을 생성해야 한다.

이는 쿠버네티스에서 제한을 두고 있는데 이유는 다음과 같다.

1. `roleRef` 를 불변으로 하면 누군가에게 기존 바인딩 객체에 대한 `update` 권한을 부여할 수 있다. 그래서 해당 피대상자에 부여된 역할을 변경하지 않고도 피대상자 목록을 관리할 수 있다.
2. 다른 role에 대한 바인딩은 근본적으로 다른 바인딩이다. `roleRef` 를 변경하기 위해 바인딩을 삭제/재생성 하도록 요구하면 바인딩의 전체 피대상자 목록에 새 역할이 부여되도록 보장한다.































