# ServiceAccount

사용자가 클러스터에 접근하게 되면  apiserver에 의해 특정 User Account로 인증된다. 파드 내 컨테이너의 프로세스 또한 apiserver에 접근할 수 있는데, 파드 내 컨테이너가 apiserver로 접근한다면 특정 Service Account로 인증된다. 



## Default ServiceAccount

ServiceAccount를 지정하지 않는 파드를 생성하게 되면 해당 네임스페이스 내 `default` ServiceAccount로 자동으로 할당된다. 파드 내용을 직접 조회해보면 (`kubectl get pods ${podName} -o yaml`) `spec.serviceAccountName` 필드가 자동으로 설정되어 있을 것이다.

파드 내에서는 자동으로 마운팅된 Service Account credential을 이용하여 API에 접근할 수 있다. ServiceAccount에 대한 API 권한은 [authorization plugin and policy](https://kubernetes.io/docs/reference/access-authn-authz/authorization/#authorization-modules) 에 의존한다.

버전 1.6부터는 `automountServiceAccountToken: false` 를 설정하여 ServiceAccount에 대한 API 자격 증명 자동 마운트를 해제할 수 있다.

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: build-robot
automountServiceAccountToken: false
...
```

마찬가지로 특정 파드에 대한 API 자격증명 자동 마운트 또한 해제할 수 있다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  serviceAccountName: build-robot
  automountServiceAccountToken: false
  ...
```



## 여러 ServiceAccount 사용

ServiceAccount를 여러 개 정의할 수 있다. 메니페스트 파일로 ServiceAccount를 정의한 후 등록할 수 있다.

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: build-robot
```

```shell
$ kubectl apply -f serviceaccount.yaml
```



## ServiceAccount API token 생성

ServiceAccount를 하나 생성하고 다음과 같이 새 시크릿을 수동으로 생성해보자.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: build-robot-secret
  annotations:
    kubernetes.io/service-account.name: build-robot
type: kubernetes.io/service-account-token
```



이 시크릿을 생성하면 새로 생성된 시크릿은 `build-robot` 이라는 ServiceAccount에 대한 API token이 된다는 것을 확인할 수 있다.