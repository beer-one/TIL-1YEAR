# Jaeger UI

Jaeger UI를 구성하는 방법에 대해 알아보자.



## 구성

UI는 몇 가지에 대해서 구성할 수 있다.

* 디펜던시 섹션을 활성화하거나 구성할 수 있다.
* 애플리케이션 분석 트래킹을 활성화하거나 구성할 수 있다.
* gloval nav에 추가 메뉴 옵션을 추가할 수 있다.
* Input limit 검색을 구성할 수 있다.



이 옵션들은 JSON 구성 파일에서 구성할 수 있다.  그런 다음에 query service가 시작될 때 query service의 `--query.ui-config` 커맨드라인 파라미터를 JSON 파일의 경로가 설정해야 한다.



기본적으로 simple product로 생성된 jaeger UI는 이렇게 구성되어있다.

```shell
$ kubectl exec -n jaeger simple-prod-query-7749558bcd-wrg88 -c jaeger-query -{"dependencies":{"menuEnabled":false}}
```



구성 파일의 예시는 다음과 같다.

```json
{
  "dependencies": {
    "dagMaxNumServices": 200,
    "menuEnabled": true
  },
  "archiveEnabled": true,
  "tracking": {
    "gaID": "UA-000000-2",
    "trackErrors": true
  },
  "menu": [
    {
      "label": "About Jaeger",
      "items": [
        {
          "label": "GitHub",
          "url": "https://github.com/jaegertracing/jaeger"
        },
        {
          "label": "Docs",
          "url": "http://jaeger.readthedocs.io/en/latest/"
        }
      ]
    }
  ],
  "search": {
    "maxLookback": {
      "label": "2 Days",
      "value": "2d"
    },
    "maxLimit": 1500
  },
  "linkPatterns": [{
    "type": "process",
    "key": "jaeger.version",
    "url": "https://github.com/jaegertracing/jaeger-client-java/releases/tag/#{jaeger.version}",
    "text": "Information about Jaeger release #{jaeger.version}"
  }]
}
```



### Dependency

`dependencies.dagMaxNumService` 는 DAG 디펜던시 뷰(그래프)가 비활성화 되기 전에 허용되는 최대 서비스 수를 정의한다. *(default=200)*

`dependencies.menuEnabled` 는 디펜던시 메뉴 버튼을 활성화(`true`)/비활성화(`false`) 하는 변수이다. *(default=true)*

 

### Archive Support

`archiveEnabled` 는 archive trace 버튼을 활성화(`true)`하거나 비활성화(`false`)한다. *(default=false)* 이는 query service 내 archive storage의 구성을 필요로 한다. Archived trace는 오직 ID를 통해 직접 접근할 수 있으며 Archived trace는 검색 불가능하다.



### App Analytics Tracking

`tracking.gaID` 는 Google Analytics tracking ID를 정의한다. 이는 Google Analytics tracking을 필요로 하며 이 값을 non-null로 설정하는 것은 Google Analytics tracking을 활성화 시킨다. *(default=null)*

`tracking.customWebAnalytics` 는 커스텀 트래킹 플러그인에 대한 팩토리 함수를 정의한다. (오직 Javascript 형태의 UI 구성을 사용)

`tracking.trackError` 는 에러 트래킹을 활성화(`true`)하거나 비활성화(`false`)한다. 유효한 분석 트래커가 구성될 때에만 에러가 트래킹 될 수 있다. *(default=true)*



### Custom Menu Items

`menu`는 gloval nav에 링크를 추가시키는 것을 허용한다. 추가 링크는 오른쪽 정렬 된다.

샘플 JSON 구성에서, 구성된 메뉴는 "GitHub" 및 "Docs" 에 대한 하위 옵션이 있는 "About Jaeger" 드롭다운이 있다. 가장 우측 메뉴의 링크에 대한 포멧은 아래와 같다.

```json
{
  "label": "Some text here",
  "url": "https://example.com"
}
```

링크는 메뉴 배열의 멤버가 되거나 드롭다운 메뉴 옵션으로 그루핑될 수 있다. 그룹에 대한 포멧은 아래와 같다.

```json
{
  "label": "Dropdown button",
  "items": [ ]
}
```

`item` 배열은 하나 이상의 링크 구성이다.



### Search Input Limit

`search.maxLimit`은 입력에서 검색할 수 있는 결과 데이터의 최대 결과 수를 구성한다.

`search.maxLookback` 은 현재 유저가 추적을 쿼리할 수 있기 전에 최대 시간을 구성한다. 이 값보다 큰 Lookback 드롭다운 옵션은 표시되지 않는다.

* label: 검색 양식 드롭다운에 표시되는 텍스트
* value: 라벨이 선택된 경우 검색쿼리에 제출된 값



### Link Patterns

`linkPatterns` 노드는 Jaeger UI에서 보여지는 필드로부터 링크를 생성하기 위해 사용된다.

* type: 링크가 추가되는 메타데이터 섹션, (process, tags, logs, traces)
* key: 값이 링크로 표시될 tag/process/log 속성의 이름. 이 필드는 `traces` 타입이 필요하지 않는다.
* url: 링크가 가리키는 URL. 이는 Jaeger UI의 외부 사이트 또는 상대 경로일 수 있다.
* text: 링크에 대한 툴팁이 표시되는 텍스트



`url`과 `text`는 템플릿으로써 정의될 수 있다. Jaeger UI는 tag/log/traces 데이터를 기반으로 값을 동적으로 대체한다.

Traces에 대해서, 지원되는 템플릿 필드는 다음과 같다. `duration`, `endTime`, `startTime`, `traceName`, `traceID`















































