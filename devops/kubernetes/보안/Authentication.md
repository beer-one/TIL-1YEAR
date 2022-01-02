# Authentication



## Kuberentes 에서의 User

모든 쿠버네티스 클러스터에는 두 가지 유형의 user가 있다.

* Service Account (K8s에서 관리)
* normal user



일단 클러스터와 독립적인 서비스는 다음과 같은 방식으로 normal user를 관리한다고 가정한다.

* private key를 배포하는 관리자
* Google Account와 같은 사용자 저장소
* 사용자 이름과 password 목록을 가진 파일



이와 관련하여 쿠버네티스에는 일반 사용자 계정을 나타내는 오브젝트가 없다. (K8s에서 관리하지 않는다는 뜻) 비록 API 호출을 통해 일반 사용자를 추가할 수 없지만 클러스터의 인증 기관 (CA)에서 서명한 유효한 인증서를 제시하는 모든 사용자는 인증된 것으로 간주한다. 이 구성에서, 쿠버네티스는 인증서의 `subject` 에서 common name 필드로부터 사용자 이름을 결정한다. 여기서, RBAC (Role Based Access Control) 서브 시스템은 사용자가 리소스에 대한 특정 연산을 수행할 권한이 있는지 여부를 결정한다. 

이에 반해 Service Account는 쿠버네티스 API에 의해 관리된다. Service Account는 특정 네임스페이스에 바인딩되고 API server에 의해 자동으로 생성되거나 수동으로 API를 호출하여 생성할 수 있다. Service Account는 Secret으로 저장된 자격 증명 집합에 연결되며 이는 클러스터 내 프로세스가 쿠버네티스 API와 통신할 수 있도록 파드에 탑재된다.

API 요청은 일반 사용자나 서비스 어카운트에 연결되거나 익명 요청으로 취급된다. 즉, 워크스테이션에서 `kubectl` 을 사용하는 사용자부터 노드의 `kubelet`, 컨트롤 플레인의 멤버에 이르기까지 클러스터 내부 또는 외부의 모든 프로세스가 API server에 요청할 때 인증되어야 하며 아니면 익명 사용자로 취급된다.



## 인증 전략

쿠버네티스는 인증 플러그인을 통해 API 요청을 인증하기 위해 `client certificates`, `bearer tokens`, `authentication proxy` 를 사용한다. API server에 HTTP 요청이 오면 플러그인은 다음 속성을 요청과 연결하려고 시도한다.

* Username: 최종 사용자를 식별하기 위한 String이다. 
* UID: 최종 사용자를 식별하고 사용자 이름보다 더 일관성 있고 고유성이 있는 String
* Groups: 명명된 논리적 사용자 컬렉션에서 사용자의 구성원임을 나타내는 String 집합
* Extra fields: 권한 부여자가 사용할 수 있는 추가 정보를 포함하는 문자열 리스트에 대한 문자열 맵



인증 시스템에서의 모든 값은 불투명하며 권한 부여자가 해석할 때만 의미를 갖는다. 

인증 방법을 한번에 여러 개 활성화 할 수 있다. 보통은 최소 두 개 이상의 방법을 사용한다.

* Service Account tokens
* 그 외 다른 하나의 인증 방식



LDAP, SAML, Kerberos, alternate x509 등과 같은 다른 인증 프로토콜과의 통합은 `authentication proxy` 또는 `authentication webhook` 을 사용하여 수행할 수 있다.



### Static Token File

API server는 커맨드라인에서 `--token-auth-file=SOMEFILE` 옵션이 주어지면 파일로부터 bearer token을 읽는다. 현재 토큰은 무기한 지속되며 API server를 다시 시작하지 않고는 토큰 목록을 변경할 수 없다. 

토큰 파일은 csv 파일이며 최소 3개의 컬럼(`token`, `user name`, `user uid`)을 가진다. 또한 선택적으로 `group names` 도 받을 수 있다.



HTTP client로부터 Bearer token 인증을 사용할 경우, API server는 `Bearer TOKEN` 값을 가진 `Authorization` 헤더를 예상한다. Bearer token은 HTTP의 인코딩 및 인용 기능만 사용하여 HTTP 헤더 값에 넣을 수 있는 문자 시퀀스이어야 한다.





### Service Account Tokens

Service Account는 서명된 bearer token을 사용하여 요청을 확인하는 자동으로 활성화된 인증자이다. 이 플러그인은 두 개의 optional flag를 가진다

* `--service-account-key-file` : 서명된 bearer token에 대한 PEM 인코딩된 키를 포함한 파일. 지정되어있지 않다면, API server의 TLS private key가 사용된다.
* `--server-account-lookup` : 이 flag가 활성화되면 API로부터 삭제된 토큰은 취소된다.



Service Account는 보통 API server에 의해 자동으로 생성되며 `ServiceAccount` Admission Controller를 통해 클러스터에서 구동 중인 파드와 연관된다. Bearer tokens은 잘 알려진 위치의 파드에 마운팅되고 클러스터 내 프로세스가 API 서버와 통신할 수 있다. 계정은 `PodSpec`의 `serviceAccountName` 필드를 사용하여 명시적으로 파드와 연관지을 수 있다.

일단 ServiceAccount를 하나 만들고 조회해보자.

```yaml
kind: ServiceAccount
apiVersion: v1
metadata:
  name: test
  namespace: default
```

```shell
$ kubectl apply -f sa.yaml

$ kubectl get sa test -o yaml
```

```yaml
 apiVersion: v1
kind: ServiceAccount
metadata:
  # ...
secrets:
- name: test-token-dnj9m
```

secrets의 이름을 통해 시크릿을 조회해보자.

```shell
$ kubectl get secret test-token-dnj9m -o yaml
```

```yaml
apiVersion: v1
data:
  ca.crt: (APISERVER'S CA BASE64 ENCODED)
  namespace: ZGVmYXVsdA==
  token: (BEARER TOKEN BASE64 ENCODED)
kind: Secret
metadata:
  # ...
type: kubernetes.io/service-account-token
```

주어진 Service Account에서 인증을 위한 bearer token으로 서명된 JWT를 사용할 수 있다. 일반적으로 이 secret은 클러스터 내에서 API 서버에 접근하기 위해 파드에 마운팅 될 수 있다. 하지만 클러스터 외부에서도 사용가능하다.

Service Account는 사용자 이름 `system:serviceaccount:(NAMESPACE):(SERVICEACCOUNT)` 을 사용하여 인증하고 그룹 `system:serviceaccounts` 과 `system:serviceaccounts:(NAMESPACE)`  할당된다. 



### Authenticating Proxy

`X-Remote-User` 와 같이 요청 헤더 값으로부터 사용자를 식별하기 위해 API server를 구성할 수 있다. 요청 헤더 값을 설정하는 인증 프록시와 함께 사용하도록 설계되었다.

* `--requestheader-username-headers` (필수값). 사용자 id를 순서대로 확인할 헤더 이름이다. 값을 구성하는 첫 번째 헤더는 username으로 사용된다.
* `--requestheader-group-headers` (1.6+, 필수X). `X-Remote-Group` 이 제안된다. 사용자의 그룹을 확인하기 위한 헤더 이름이다. 헤더에 지정된 모든 값이 그룹 이름으로 사용된다.
* `--requestheader-extra-headers-prefix` (1.6+, 필수X). `X-Remote-Extra` 가 제안된다. 사용자에 관한 추가 정보를 확인하기 위해 확인할 헤더 prefix이다. 지정된 모든 prefix 로 시작하는 모든 헤더는 prefix가 제거된다. 헤더의 나머지 부분은 소문자로 변하며 percent-decoded되고 extra key가 된다. 그리고 헤더 값은 extra value가 된다.

예를 들어 다음과 같이 구성할 수 있다.

```
--requestheader-username-headers=X-Remote-User
--requestheader-group-headers=X-Remote-Group
--requestheader-extra-headers-prefix=X-Remote-Extra-
```



이렇게 구성할 경우 다음의 요청에 대해

```
GET / HTTP/1.1
X-Remote-User: fido
X-Remote-Group: dogs
X-Remote-Group: dachshunds
X-Remote-Extra-Acme.com%2Fproject: some-project
X-Remote-Extra-Scopes: openid
X-Remote-Extra-Scopes: profile
```



사용자 정보의 결과값은 다음과 같이 된다.

```yaml
name: fido
groups:
- dogs
- dachshunds
extra:
  acme.com/project:
  - some-project
  scopes:
  - openid
  - profile
```

