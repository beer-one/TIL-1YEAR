# Taint & Toleration

노드 어피티느는 노드 셋을 끌어들이는 속성이지만 Taint는 그 반대로 노드가 파드 셋을 제외할 수 있다. Toleration은 파드에 적용되며 일치하는 Taint가 있는 노드에 스케줄되게 하지만 필수는 아니도록 구성할 수 있다.

Taint와 Toleration은 함께 작동하며 파드가 부적절한 노드에 스케줄링되지 않게 한다. 하나 이상의 Taint가 노드에 적용되는데 이는 노드가 Taint를 용인하지 않는 파드를 수용해서는 안된다는 것을 의미한다.



## Taint 

아래 명령어를 이용하여 노드에 Taint를 추가할 수 있다.

```shell
$ kubectl taint nodes node1 key1=value1:NoSchedule
```

* `node1` 에 Taint를 배치한다.
* Taint에는 `key1` - `value1` 및 Taint 이펙트인 `NoSchedule` 이 있다.
* 일치하는 Toleration이 없으면 파드를 node1에 스케줄할 수 없다는 의미이다.



Taint를 제거하는 방법은 아래와 같다.

```shell
$ kubectl taint nodes node1 key1=value1:NoSchedule-
```



## Toleration

PodSpec에서 파드에 대한 Toleration을 지정할 수 있다.

아래 Toleration이 있는 Taint (key1, value1, NoSchedule) 이 있는 노드에 스케줄링 될 수 있다.

```yaml
tolerations:
- key: "key1"
  operator: "Equal"
  value: "value1"
  effect: "NoSchedule"
```

```yaml
tolerations:
- key: "key1"
  operator: "Exists"
  effect: "NoSchedule"
```

* `operator`가 `Exists` 인 경우 `value` 를 지정하지 않는다.
* `operator`가 `Equal` 인 경우 `value` 를 지정한다.



## Taint Effect

위의 예시에서는 Taint Effect로 `NoSchedule` 을 사용하였는데 이펙트로는 여러가지가 있다.

* `NoSchedule` : 해당 노드에 파드를 스케줄링 하지 않는다.
* `PreferNoSchedule` : 파드를 해당 노드에 스케줄링 하지 않으려고 한다. (우선순위 최하)
* `NoExecute` : 이미 노드에 실행 중인 파드에 이에 맞는 Toleration이 없으면 즉시 축출되고 노드에 스케줄링 되지 않는다.

