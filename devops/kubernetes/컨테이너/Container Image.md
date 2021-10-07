# Container Image

컨테이너 이미지는 애플리케이션과 모든 소프트웨어 의존성을 캡슐화하는 바이너리를 나타낸다. 컨테이너 이미지는 독립적으로 실행할 수 있고 런타임 환경에 대해 잘 정의된 가정을 만드는 실행 가능한 소프트웨어 번들이다. 일반적으로 파드에서 참조하기 전에 애플리케이션의 컨테이너 이미지를 생성하여 레지스트리로 푸쉬한다.



## 이미지 이름

컨테이너 이미지는 일반적으로 `pause`, `example/mycontainer`, `kube-apiserver` 와 같은 형식으로 `/`, `-` 구분으로 이름을 부여한다. 그리고 레지스트리 호스트 이름과 포트번호도 포함할 수 있다. (`registry.example:12345/imagename`) 레지스트리 호스트 이름을 지정하지 않으면 k8s는 도커 퍼블릭 레지스트리를 의미한다고 가정한다. 

이미지 이름 부분 뒤에 태그를 추가할 수 있다. 태그를 사용하면 동일한 이미지의 다른 버전을 식별할 수 있다. 이미지 태그는 소문자와 대문자, 숫자, 구분문자  `-`, `.`, `-` 로 구성된다. 구분문자를 배치할 수 있는 위치에 대한 추가 규칙도 있다. 태그를 지정하지 않는다면 k8s는 태그 latest를 의미한다고 가정한다. (프로덕션 환경에서는 latest는 지양해야 한다. 이미지 버전 추적이 어렵고 이전 버전으로 롤백이 어렵기 때문)



## 이미지 업데이트

디플로이먼트, 스테이트풀셋, 파드, 파드 템플릿을 포함하는 오브젝트를 처음 만들 때 기본적으로 해당 파드에 있는 모든 컨테이너의 풀 정책은 `IfNotPresent` 로, 이미 이미지가 존재하면 이미지 풀을 생략한다. 



### 이미지 풀 정책

컨테이너의 `imagePullPolicy`과 이미지의 태그는 kubelet이 특정 이미지 풀을 시도할 때 영향을 끼친다. `imagePullPolicy` 종류에 대해 설명하겠다.



* **IfNotPresent**: 로컬에 이미지가 존재하지 않을 때만 이미지를 풀한다.
* **Always**: kubelet이 컨테이너를 실행할 때 마다 kubelet은 레지스트리에서 해당 이미지를 찾아서 이름을 이미지 digest로 확인한다. kubelet에 정확한 digest가 로컬에 캐시된 컨테이너 이미지가 있는 경우, kubelet은 캐시된 이미지를 사용한다. 그렇지 않으면, kubelet은 확인된 digest로 이미지를 풀하고 컨테이너를 실행할 때 이미지를 사용한다.
* **Never**: kubelet은 이미지 페치를 시도하지 않는다. 이미지가 이미 로컬에 존재한다면 kubelet은 컨테이너 시작을 시도한다. 그렇지 않으면 컨테이너 실행이 실패한다.



기본 이미지 공급자의 캐싱 의미는 레지스트리가 신뢰할 수 있는 한 항상 효율적으로 `imagePullPolicy` 를 균일하게 만든다. 컨테이너 런타임은 이미지 레이어가 항상 노드에 존재한다면 다운로드를 하지 않아도 된다는 것을 알고 있다.

파드가 항상 같은 버전의 컨테이너 이미지를 사용한다는 것을 보장하기 위해서는 `<image-name>:<tag>`를 `<image-name>@<digest>` 로 변경하여 이미지의 digest를 지정할 수 있다. 

이미지 태그를 사용한다면, 이미지 레지스트리에서 해당 태그의 이미지가 나타내느 코드를 변경하는 경우, 이전 코드와 새 코드가 혼합된 파드가 실행될 수 있다. (파드는 동적으로 죽었다 살아나기 때문에..) 하지만 이미지 다이제스트는 이미지의 특정 버을 고유하게 식별하기 때문에 kubernetes는 이미지 이름과 다이제스트가 지정된 컨테이너가 시작할 때 마다 같은 코드로 실행되는 것이 보장된다. 이미지를 지정하면 레지스트리를 변경해도 이러한 버전이 혼합되지 않도록 실행하는 코드가 고정된다.



### 기본 이미지 풀 정책

apiserver로 새로운 파드를 제출할 때, 클러스터는 특정 조건이 충족되면 `imagePullPolicy` 필드를 설정한다.

* `imagePullPolicy` 필드를 생략하고 컨테이너 이미지 태그가 `:latest` 라면, `imagePullPolicy` 는 자동으로 `Always`로 설정된다.
* `imagePullPolicy` 필드를 생략하고 컨테이너 이미지 태그를 지정하지 않는다면, `imagePullPolicy` 는 자동으로 `Always`로 설정된다.
* `imagePullPolicy` 필드를 생략하고 컨테이너 이미지 태그를 `:latest` 외 다른 버전으로 지정한다면, `imagePullPolicy` 는 자동으로 `IfNotPresent`로 설정된다.



### 이미지 풀 강제

항상 이미지 풀을 강제하고 싶으면 이 들 중 하나를 수행하면 된다.

* `imagePullPolicy` 를 `Always`로 설정한다.
* `imagePullPolicy`를 생략하고 태그를 `:latest`로 설정한다.
* `imagePullPolicy`와 이미지 태그를 생략한다.
* **AlwaysPullImages admission controller**를 사용가능하도록 한다. (?)



### ImagePullBackOff

kubelet이 컨테이너 런타임 사용하여 파드용 컨테이너를 만들기 시작할 때 `ImagePullBackOff` 때문에 컨테이너가 waiting 상태가 될 수 있다. `ImagePullBackOff` 상태는 쿠버네티스가 컨테이너 이미지를 풀하지 못했기 때문에 시작하지 못한다는 것을 의미한다. `BackOff` 부분은 쿠버네티스가 이미지 풀을 지속적으로 시도할 것을 의미한다. 쿠버네티스는 컴파일된 제한인 300초에 도달할 때 까지 각 풀 시도 사이의 지연시간을 증가시킨다.



## 프라이빗 레지스트리 사용

프라이빗 레지스트리는 해당 레지스트리에서 이미지를 읽기 위한 키를 요구할 것이다. 자격증명은 아래와 같이 여러 방법으로 제공된다.

* 프라이빗 레지스트리에 대한 인증을 위한 노드 구성
  * 모든 파드들은 구성된 프라이빗 레지스트리를 읽을 수 있다.
  * 클러스터 운영자에 의한 노드 구성이 필요하다.
* 미리 풀 된 이미지
  * 모든 파드들은 노드에 캐시된 이미지를 사용할 것이다.
  * 설정하기 위해 모든 노드에 대해 루트 접근이 필요하다.
* ImagePullSecret을 파드에 지정
  * 자신의 키를 제공하는 파드만이 프라이빗 레지스트리에 접근할 수 있다.
* 벤더 업체별 또는 로컬 익스텐션
  * 커스텀 노드 구성을 사용하고 있다면 노드에서 컨테이너 레지스트리로의 인증을 위한 메커니즘을 구현할 수 있다.



### 프라이빗 레지스트리에 대한 인증을 위한 노드 구성

노드에서 도커로 실행 중이라면, 프라이빗 레지스트리에 대한 인증을 위해 도커 컨테이너 런타임을 구성할 수 있다. 이 방법은 노드 구성을 제어할 수 있는 경우에 적합하다.

도커는 프라이빗 레지스트리를 위한 키를 `$HOME/.dockercfg` 또는 `$HOME/.docker/config.json` 에 저장한다. (도커 레지스트리에 접속하면 생긴다.) 아래 검색 경로 리스트 내 같은 파일이 들어있다면 kubelet은 이미지 풀 할 때 이를 자격증명 제공자로 사용한다. (있으면 된다는 건가?)

- `{--root-dir:-/var/lib/kubelet}/config.json`
- `{cwd of kubelet}/config.json`
- `${HOME}/.docker/config.json`
- `/.docker/config.json`
- `{--root-dir:-/var/lib/kubelet}/.dockercfg`
- `{cwd of kubelet}/.dockercfg`
- `${HOME}/.dockercfg`
- `/.dockercfg`



프라이빗 레지스트리를 사용하기 위해 노드를 구성하는 추천 단계가 아래에 있다.

1. 사용하려는 각 자격증명 셋에 대해서  `docker login <server>` 를 실행한다. 로그인이 성공하면 머신에 `$HOME/.docker/config.json`이 업데이트(생성) 될 것이다.
2. 에디터에서  `$HOME/.docker/config.json`를 보고 사용하고 싶은 자격증명만 포함하고 있는지 확인한다.
3. 노드의 리스트를 구한다.
   * 이름을 알고싶다면 `nodes=$(kubectl get nodes -o jsonpath='{range.items[*].metadata}{.name} {end}')`
   * IP 주소를 알고 싶다면 `nodes=$(kubectl get nodes -o jsonpath='{range.items[*].status.addresses[?(@.type="ExternalIP")]}{.address} {end}')`
4. 로컬 `.docker/config.json` 을 위에 나열된 검색 경로 리스트 중 하나에 복사한다.



클러스터 내 모든 노드가 같은 `.docker/config.json` 파일을 가지고있음을 보장해야 한다. 그렇지 않으면 특정 노드에서 실행된 파드는 다른 노드로 실행이 실패하게 된다. 



### 미리 풀된 이미지

기본적으로, kubelet은 각 이미지를 지정된 레지스트리에 풀 하도록 시도한다. 그러나 컨테이너 내 `imagePullPolicy` 프로퍼티가 `IfNotPresent` 또는 `Never` 로 설정되어 있다면 로컬 이미지가 사용될 것이다. 레지스트리 인증 대신 미리 풀된 이미지를 사용하려면 클러스터 내 모든 노드에 동일한 미리 풀된 이미지가 있는지 확인해야 한다. 이 방법은 속도 때문이나 프라이빗 레지스트리에 대한 인증 대신에 특정 이미지를 미리 로딩하는 데 사용할 수 있다. 모든 파드는 사전에 풀된 이미지에 대한 읽기 접근 권한을 가진다.



###  ImagePullSecret을 파드에 지정

쿠버네티스는 파드에 컨테이너 이미지 레지스트리 키 지정을 지원한다.



#### Docker config로 시크릿 생성

아래 명령어를 사용하면 시크릿을 생성할 수 있다. 

```shell
$ kubectl create secret docker-registry <name> --docker-server=DOCKER_REGISTRY_SERVER --docker-username=DOCKER_USER --docker-password=DOCKER_PASSWORD --docker-email=DOCKER_EMAIL
```

이미 도커 자격증명 파일이 있다면, 이 명령어를 사용하는 것 보다 자격증명 파일을 쿠버네티스 시크릿으로 [임포트](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#registry-secret-existing-credentials) 할 수도 있다. 해당 명령어는 오직 하나의 프라이빗 레지스트리에서만 작동하는 시크릿을 생성하므로 여러개의 프라이빗 컨테이너 레지스트리를 사용한다면 이 방법(import)이 특히 유용하다. 



#### 파드에 imagePullSecrets 참조

파드 정의의 `imagePullSecrets` 섹션을 추가하여 시크릿을 참조하는 파드를 만들 수 있다.

```shell
$ cat <<EOF > pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: foo
  namespace: awesomeapps
spec:
  containers:
    - name: foo
      image: janedoe/awesomeapp:v1
  imagePullSecrets:
    - name: myregistrykey
EOF

$ cat <<EOF >> ./kustomization.yaml
resources:
- pod.yaml
EOF
```

이는 프라이빗 레지스트리를 사용하는 각 파드에 대해 수행해야 한다. 그러나, **ServiceAccount** 리소스에서 imagePullSecrets을 설정하여 이 필드의 설정을 자동화할 수 있다.











