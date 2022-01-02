# Kafka WebView 비교

Kafka Web View를 비교하자.



* [yahoo/CMAK](https://github.com/yahoo/CMAK)
* [obsidiandynamics/kafdrop](https://github.com/obsidiandynamics/kafdrop)

* [cloudhut/kowl](https://github.com/cloudhut/kowl)
* [provectus/kafka-ui](https://github.com/provectus/kafka-ui)

* [SourceLabOrg/kafka-webview](https://github.com/SourceLabOrg/kafka-webview)





## CMAK

CMAK (Cluster Manager for Apache Kakfa, aka Kafka Manager) 는 카프카 클러스터를 관리하는 툴이다. CMAK이 지원하는 기능은 다음과 같다.

* 다중 클러스터 관리
* 토픽, 컨슈머, 오프셋, 브로커, 레플리카 분산, 파티션 분산 등 클러스터 상태 검사
* 기본 레플리카 선출 실행
* 사용할 브로커를 선택하는 옵션과 함께 파티션 할당 생성
* 생성된 할당을 기반으로 파티션 재할당 실행
* 옵셔널 토픽 구성으로 토픽 생성
* 토픽 삭제 및 삭제 표시된 토픽 표시
* 사용할 브로커를 선택하는 옵션으로 여러 토픽에 대한 파티션 할당을 일괄 생성
* 다중 토픽에 대한 파티션의 재할당 일괄 실행
* 기존 토픽에 파티션 추가
* 기존 토픽의 구성 업데이트
* 브로커 수준 및 토픽 수준 메트릭에 대해 선택적으로 JMX 폴링 활성화
* 주키퍼에 id, 소유자, 오프셋, 디렉토리를 가지고 있지 않은 컨슈머를 선택적으로 필터링



## Kafdrop

kafdrop은 카프카 토픽과 컨슈머 그룹을 볼 수 있도록 하는 Web UI 프로그램이다. kafdrop은 브로커, 토픽, 파티션, 컨슈머 등의 정보를 확인할 수 있고 브로커에 쌓인 메시지도 볼 수 있다.

* 카프카 브로커 확인 - 토픽과 파티션 할당, 컨트롤러 상태 등 확인
* 토픽 확인 - 파티션 개수, 레플리카 상태, 커스텀 구성 등..
* 메시지 검색 - JSON, plain text, avro, protobuf 인코딩 지원
* 컨슈머 그룹 확인 - 파티션당 랙을 포함한 파티션당 오프셋
* 새 토픽 생성
* ACL 확인



## Kowl

Kowl은 카프카 클러스터의 메시지를 쉽게 검색하고 편리한 방법으로 카프카 클러스터에서 실제로 일어나는 일에 대한 것들을 얻는데 도움이 되는 웹 애플리케이션이다.

* 메시지 뷰어: ad-hoc 쿼리와 다이나믹 필터를 통한 메시지 뷰에서 토픽 메시지를 검색할 수 있다. Javascrip 기능을 사용하여 메시지를 필터링하여 원하는 메시지를 찾을 수 있다. 지원되는 인코딩 양식은 Json, avro, protobuf, xml, messagePack, text, binary가 있다.
* 컨슈머 그룹: 활성 그룹 오프셋과 함께 모든 활성 컨슈머 그룹을 나열하거나 그룹 오프셋을 수정하거나 컨슈머 그룹을 삭제할 수 있다.
* 토픽 개요: 카프카 토픽 목록을 탐색하고 구성, 공간 사용량을 확인하고 단일 토픽을 사용하는 컨슈머를 나열하거나 파티션 세부정보를 확인하고, 깃 레포지토리에서 토픽 문서를 포함하는 등의 작업을 수행한다.
* 클러스터 개요: ACL, 이용 가능한 브로커, 브로커의 공간 사용량, rack id와 다른 정보를 나열하여 클러스터 브로커에 대한 높은 수준의 개요를 볼 수 있다.
* 스키마 레지스트리: 모든 avro, protobuf, json 스키마를 스키마 레지스트리에 나열한다.
* 카프카 연결: 다중 연결 클러스터로부터 커넥터를 관리하고 구성을 패치하고 현재 상태를 보거나 작업을 다시 시작한다.



## kafka-ui

Kafka-ui는 카프카 클러스터를 관리하고 모니터링하는 웹 UI 오픈소스이다. 

* 멀티 클러스터 관리: 하나의 공가에서 모든 클러스터를 관리하고 모니터링한다.
* 메트릭 대시보드로 성능 모니터링: 경량 대시보드로 주요 카프카 메트릭 추적
* 카프카 브로커 확인: 토픽과 파티션 할당, 컨트롤러 상태 확인
* 카프카 토픽 확인: 파티션 개수, 레플리카 상태, 커스텀 구성 확인
* 컨슈머 그룹 확인: 파티션당 오프셋, 결합 및 파티션당 지연 확인
* 메시지 검색: JSON, plan text, avro 인코딩으로 메시지 검색
* 동적 토픽 구성: 동적 구성으로 새로운 토픽을 생성하고 구성한다.
* 구성 가능한 인증: 선택적 Github/Gitlab/Google OAuth 2.0으로 설치 보안



## kafka-webview

Kafka webView는 카프카 토픽에서 데이터를 읽고 기본 필터링 및 검색 기능을 제공하기 위한 사용하기 쉬운 웹 기반 인터페이스를 제공한다.

* 여러 카프카 클러스터와 연결 가능
* SSL, SASL 인증 클러스터에 연결
* 표준 key, value deserializer 지원
* 업로딩 커스텀 key, value deserializer 지원
* 토픽에 대해 사용자 정의 및 강제 필터링 지원
* 다중 사용자 관리와 접근 제어 옵션 제공
  * 앱 정의 유저 사용 (default)
  * 인증/인가를 위한 LDAP 서버 사용
  * 사용자 인가를 완전 비활성화
* 웹 기반 컨슈머 지원
  * 오프셋 찾기
  * 타임스탬프 찾기
  * 파티별 필터링
  * 구성 가능한 서버사이드 필터링 로직
* 스트리밍 컨슈머 기반 라이브 웹 소켓
* 컨슈머 그룹 상태 모니터링



## 간단 비교





|                         | CMAK | Kafdrop | Kowl | Kafka-ui | Kafka-webview |
| :---------------------- | :--- | :------ | ---- | -------- | ------------- |
| 다중 클러스터 연결      | O    | O       | O    | O        | O             |
| 클러스터 상태 확인      | O    | O       | O    | O        | O             |
| 파티션 재할당           | O    | X       | X    | X        | X             |
| 토픽 생성/삭제          | O    | O       | X    | O        | O             |
| 토픽 구성 확인          | O    | O       | O    | O        | O             |
| 토픽 구성 업데이트      | O    | X       | X    | X        | O             |
| 기존 토픽에 파티션 추가 | O    | X       | X    | X        | X             |
| 컨슈머 그룹 확인        | X    | O       | O    | O        | O             |
| 토픽 메시지 확인        | X    | O       | O    | O        | O             |
| 토픽 메시지 프로듀싱    | X    | X       | X    | O        | X             |

​	

* UI: kowl = kafka-webview > CMAC = kafka-ui > kafdrop

* GitStar: CMAC(10.5K) > kafdrop(3.2K) > kowl = kafka-ui(1.9K) > kafka-webview(344)

* 검색 속도: kafka-ui = kafdrop > kowl

  * Kowl: timestamp, offset, 필터(검색) 기능, limit 그런데 페이징이 구림 (limit만큼 페이지가 정해진따..)
  * kafdrop: offset 검색만 가능, limit 있다. 페이징 없다.
  * kafka-ui: newest first 기능이 있고, 검색 기능있고 offset/timestamp 있다. offset으로 할 때에는 페이징이 잘 되는데 timestamp는 이상하다. 뒤로가기는 없다;

  









