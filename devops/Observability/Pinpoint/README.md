# Pinpoint

Pinpoint는 Java/PHP 기반 대규모 분산 시스템을 위한 APM 툴이다. Pinpoint는 전반적인 시스템 구조를 분석하고 분산 애플리케이션 사이에서 트랜잭션을 추적하여 컴포넌트가 상호 연결되는 방법에 대한 솔루션을 제공한다. 

Pinpoint는 크게는 다음 기능(특징)을 가진다.

* 애플리케이션 토폴로지
* 실시간 애플리케이션 모니터링
* 모든 트랜잭션에 대한 코드레벨의 가시성 제공
* APM 에이전트를 설치하여 애플리케이션 코드 변경 없이 APM 기능 제공
* 퍼포먼스에 대한 최소한의 오버헤드 (resource usage가 3%정도 늘어남)



## 구성

Pinpoint는 컴포넌트 사이의 트랜잭션을 추적하고 문제가 발생한 지점과 병복점을 확인하기 위해 View를 제공한다. Pinpoint가 제공하는 View는 다음과 같다.



* ServerMap: 어떤 컴포넌트가 상호 연결되는지 시각화 함으로써 모든 분산 시스템의 토폴로지를 이해할 수 있도록 뷰를 제공한다. 노드를 클릭하면 현재 상태와 트랜잭션 횟수와 같은 컴포넌트에 대한 세부사항을 볼 수 있다.
* Realtime Active Thread Chart: 실시간으로 애플리케이션 내의 active thread를 모니터링할 수 있다.
* Request/Response Scatter Chart: 어떤 문제를 확인하기 위해 특정 시간대의 Request 횟수와 Response 패턴을 시각화 한다. 차트를 드래그하면 선택된 요청에 대한 세부 트랜잭션 내용을 볼 수 있다.
* CallStack: 분산 환경에서의 모든 트랜잭션에 대한 코드레벨의 시각화를 얻을 수 있다. 이 뷰를 통해서 병목지점이나 실패지점을 확인할 수 있다.
* Inspector: CPU 사용량, 메모리/GC, TPS, JVM arguments 등의 애플리케이션 세부 정보를볼 수 있다.

