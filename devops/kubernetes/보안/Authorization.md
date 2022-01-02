# Authorization

지원되는 권한 부여 모듈을 사용하여 정책을 만드는 방법에 대한 세부정보를 포함하여 쿠버네티스 권한 부여에 대해 알아보자.

쿠버네티스에서는, 인증 절차를 가진 후 권한이 부여되어있는지 확인한다. 쿠버네티스는 REST API 요청에 공통적인 속성을 예상한다. 이는 쿠버네티스 권한 부여가 쿠버네티스 API 외의 다른 API를 처리할 수 있는 기존 조직 전체 또는 클라우드 제공자 전체 접근 제어 시스템에서 작동함을 의미한다. 



## 요청 허용 여부 결정

쿠버네티스는 API server를 사용하여 API 요청을 승인한다. 모든 정책에 대해 요청 속성을 평가하고 요청을 허용하거나 거부한다. 계속 진행하려면 어떤 정책에 의해 API 요청의 모든 부분을 허용해야 한다. 이는 기본적으로 권한이 거부됨을 의미한다.

여러 권한 모듈이 구성되어있다면 순차적으로 각각의 모듈에서 권한을 확인한다. 승인자가 요청을 승인하거나 거부하는 경우 해당 결정은 즉시 반환되고 다른 승인자는 확인하지 않는다. 모든 승인자가 요청에 대한 의견이 없다면 요청이 거부된다. 요청이 거부되면 403을 반환한다.



## 요청 속성 검토

쿠버네티스는 다음의 API 요청 속성만을 검토한다.

* `user` : 인증에서 제공되는 user 문자열
* `group` : 인증에서 사용한 그룹 이름 목록
* `extra` : 임의의 key-value 맵, 인증 레이어에서 제공된다.
* `API` : API 리소스에 대한 요청인지 여부를 나타낸다.
* `Request path` : `/api` 또는 `/healthz` 와 같은 기타 비 리소스 엔드포인트에 대한 경로
* `API request verb` : API verb (`get`, `post` 등..)
* `Resource` : 접근할 리소스의 ID 또는 이름
* `Subresource` : 접근할 서브리소스
* `Namespace` : 접근할 오브젝트의 네임스페이스
* `API group` : 접근할 API 그룹



## Request Verb 결정

**Non-resource request** : `/api/v1/...` 또는 `/apis/<group>/<version>/...` 을 제외한 엔드포인트에 대한 요청이 Non-resourece request로 간주되며 요청의 소문자 HTTP 메서드를 동사로 사용한다. 

**Resource request** : 리소스 API 엔드포인트에 대한 request verb를 결정하기 위해, 사용된 HTTP verb를 검토하고 요청이 개별 리소스 또는 리소스 컬렉션에 대해 작동하는지 검토한다.

| HTTP verb | request verb                                                 |
| --------- | ------------------------------------------------------------ |
| POST      | create                                                       |
| GET, HEAD | get (개별 리소스), list (컬렉션), watch (리소스, 리소스 컬렉션을 지속적으로 감시) |
| PUT       | update                                                       |
| PATCH     | patch                                                        |
| DELETE    | delete (개별 리소스), deletecollection (컬렉션)              |



쿠버네티스는 특정 동사를 사용하여 추가 권한에 대해 권한을 확인한다. 추가 권한 및 확인하기 위한 특정 동사는 아래와 같다.

* PodSecurityPolicy
  * `use` : `policy` API 그룹에서 `podsecuritypolicies` 리소스에 대한 권한 확인
* RBAC
  * `bind`, `escalate` : `rbac.authorization.k8s.io` API 그룹에서 `roles`, `clusterrole` 리소스에 대한 권한 확인
* Authentication
  * `impersonate` : core API 그룹에서의 `users`, `groups`, `serviceaccounts` 리소스에 대한 권한 확인과 `authentication.k8s.io` API 그룹에서의 `userextras` 리소스에 대한 권한 확인



## 권한 모드

쿠버네티스 API 서버는 여러 권한 모드 중 하나를 사용하여 요청에 대한 권한을 확인할 수 있다.

* **Node** : 실행하도록 예약된 파드를 기반으로 kubelet에 권한을 부여하는 특수 목적 권한 부여 모드이다. 
* **ABAC**(Attribute-base access control) : 속성을 결합하는 정책을 사용하여 사용자에게 접근 권한을 부여하는 접근 제어 패러다임을 정의한다. 
* **RBAC**(Role-based access control) : 기업 내 개별 사용자의 역할에 따라 컴퓨터 또는 네트워크 리소스에 대한 접근을 규제하는 방법이다. 여기서 접근이라는 것은 파일을 생성하거나 조회하거나 변경하는 등의 특정 업무를 수행하는 개별 사용자에 대한 능력을 의미한다. 
* **Webhook** : 웹훅은 HTTP callback이다. 이는 문제가 발생했을 때 발생하는 HTTP POST이다(HTTP POST를 사용한 특정 이벤트 알람). WebHooks을 구현하는 웹 애플리케이션은 특정 이벤트가 발생했을 때 메시지를 URL로 POST 한다. 

























