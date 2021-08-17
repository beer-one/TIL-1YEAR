# CORS

CORS(Cross-Origin Resource Sharing, 교차 출처 리소스 공유)는 HTTP 헤더를 하나 추가하여, 한 출처에서 실행 중인 웹 애플리케이션이 다른 출처의 선택한 자원에 접근할 수 있는 권한을 부여하도록 브라우저에서 알려주는 방식이다. 웹 애플리케이션은 리소스가 자신의 출처(도메인, 프로토콜, 포트)와 다를 때 교차 출처 HTTP 요청을 실행한다.

ex) https://beer1.com 프론트엔드에서 XMLHttpRequest를 사용하여 https://beer2.com/api를 요청하는 경우 도메인이 다른 출처에게 리소스를 요청하므로 교차 출처 요청의 한 사례가 된다.



## 보안 상 문제를 막기 위해..?

보안 상의 이유로 브라우저는 스크립트에서 시작한 교차출처 HTTP 요청을 제한한다. (postman이나 다른 환경에서 rest api 요청은 된다.) 교차출처 HTTP 요청을 제한하는 환경에서 다른 출처의 리소스를 불러오려면 그 출처에서 CORS 헤더를 포함한 응답을 반환해야 한다.

CORS 체제는 브라우저와 서버 간의 안전한 교차 출처 요청 및 데이터 전송을 지원한다. 



## 진행 과정

CORS 표준은 웹브라우저에서 해당 정보를 읽는 것이 허용된 출처를 서버에서 설명할 수 있는 HTTP 헤더를 추가함으로써 동작한다. 추가로, 서버 데이터를 조작할 수 있는 HTTP 요청 메서드(OPTION, GET을 제외) 에 대해 CORS는 브라우저가 요청을 OPTION 메서드로 freflight 하여 요청하고, 서버의 허가가 떨어지면 실제 요청을 보내도록 하고 있다. 그리고 서버는 클라이언트에게 요청에 인증정보를 쿠키에 담아 보내야한다고 알려줄 수도 있다.



 

### 단순 요청

일부 요청을 CORS preflight를 하지 않는다. 단순 요청은 아래 조건을 모두 충족하는 요청이다.

* GET, HEAD, POST 중 하나의 메서드
* 유저 에이전트가 자동으로 설정한 헤더 외에 수종으로 설정할 수 있는 헤더(Fetch 명세에서 CORS-safelisted request-header로 정의한 헤더)
  * Accept
  * Accept-Language
  * Content-Language
  * Content-Type (아래의 값만 허용)
    * application/x-www-form-urlencoded
    * multipart/form-data
    * text/plain
  * DPR
  * Downlink
  * Save-Data
  * Viewport-Width
  * Width



단순 요청의 예를 들면, https://beer1.com 에서 https://api.beer1.com 에서 제공하는 GET APIf를 요청한다고 하자. 그러면 preflight 없이 단순 요청으로 교차 출처 리소스 공유를 할 수 있다.

![스크린샷 2021-07-22 오전 12 56 10](https://user-images.githubusercontent.com/35602698/129751527-08c35545-78f4-4664-8a80-545f17ad087a.png)

* 클라이언트가 서버에 요청을 보낼 때 요청헤더에 Origin이 추가된다. 이 헤더로 서버(api.beer1.com)은 어떤 클라이언트로부터 요청이 왔는지 알 수 있다.
* 서버는 클라이언트의 요청을 받고 응답을 내려줄 때 Access-Control-Allow-Origin 헤더를 내려준다. 이 경우 Access-Control-Allow-Origin: * 으로 응답해야 하며 이는 모든 도메인에서 접근할 수 있음을 의미한다.
* api.beer1.com의 리소스 소유자가 오직 beer1.com 의 요청만 리소스에 대한 접근을 허용하려면 `Access-Control-Allow-Origin: beer1.com` 으로 전송하면 된다. 이러면 beer1.com 외의 도메인은 교차 출처 방식으로 리소스에 접근할 수 없다.



### 그 외

단순 요청을 제외한 나머지 요청에  대해서는 preflight 요청이 필요하다. preflighted request는 OPTIONS 메서드를 통해 다른 도메인의 리소스로 HTTP 요청을 보내 실제 요청이 전송하기에 안전한지 확인한다. 단순 요청을 제외한 요청은 서버측 자원을 조작할 수 있기 때문에 preflighted로 확인이 필요하다.

이번에는 자원을 생성하기 위해 https://beer1.com 에서 https://api.beer1.com 에서 제공하는 POST API를 요청한다고 하자. 여기서 Content-Type: application/json으로 요청한다고 하자. 그러면 Content-Type 때문에 preflight가 필요하다.



![스크린샷 2021-07-22 오전 1 09 18](https://user-images.githubusercontent.com/35602698/129751574-a300a81e-d38f-4183-b4f6-f512441a9a96.png)

* preflight에서는 OPTIONS 메서드로 요청하며 헤더에 실제 요청을 전송할 메서드를 Access-Control-Request-Method 헤더에 담아 보내고, 실제 요청을 전송할 때 같이 보낼 사용자 정의 헤더를 Access-Control-Request-Headers에 담아 보낸다.
* 위 두 가지 정보로 서버는 이러한 요청을 수락할지 결정한다. 클라이언트에게 OPTIONS 요청이 오면 요청에 허락할 메서드들을 Access-Control-Allow-Methods에 담아 보내고, 요청에 허락할 사용자 헤더들을 Access-Control-Allow-Headers에 담아 보낸다.
* Access-Control-Allow-Methods에 정의된 메서드와 같은 메서드고  Access-Control-Request-Headers에 정의된 헤더와 같은 헤더로 요청을 보내는거라면 본 요청을 보낼 수 있다.



