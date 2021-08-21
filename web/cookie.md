# Cookie

HTTP에서 쿠키는 서버가 클라이언트의 웹 브라우저에 전송하는 작은 데이터 조각이다. 브라우저는 서버에서 쿠키를 받아 저장해두었다가 동일한 서버에 재 요청 시 쿠키와 함께 데이터를 전송한다. 

**참고자료**

https://developer.mozilla.org/ko/docs/Web/HTTP/Cookies

https://help.salesforce.com/s/articleView?id=000351874&type=1

https://seob.dev/posts/%EB%B8%8C%EB%9D%BC%EC%9A%B0%EC%A0%80-%EC%BF%A0%ED%82%A4%EC%99%80-SameSite-%EC%86%8D%EC%84%B1/

https://blog.chromium.org/2020/01/building-more-private-web-path-towards.html



## 사용 목적

쿠키는 주로 3가지 목적을 위해 사용한다.

* 세션 관리: 로그인 정보, 장바구니 등 사용자의 정보
* 개인화: 사용자 선호, 테마 등의 정보.
* 트래킹: 사용자 행동을 기록하고 분석하는 용도



## 쿠키는 왜 필요한가?

원래 HTTP는 비연결성(Connectionless)과 무상태성(Stateless)의 특징을 지니고 있다. 여기서 무상태성의 특징 때문에 HTTP 기반 웹 서버는 클라이언트의 요청이 들어오면 어떤 클라이언트의 요청이 들어왔는지 모른다. 하지만, 대부분의 웹 서비스는 로그인 기능이 있고, 로그인한 회원이 웹 서버에 요청을 하기 때문에 웹 서버는 어떤 회원(클라이언트)이 요청했는지를 알아야 한다.

그래서 어떤 회원인지에 대한 정보가 쿠키에 저장되어 쿠키와 함께 클라이언트가 웹 서버에 요청을 한다. 그러면 웹 서버는 어떤 클라이언트가 요청했는지를 알 수 있다.



## 쿠키 생성

웹 서버는 HTTP 요청을 받은 후 응답을 내려줄 때 `Set-Cookie` 헤더를 전송할 수 있다. 클라이언트에서는 서버의 응답을 받은 후 쿠키는 보통 브라우저에 저장되며, 그 후 같은 서버에 요청하면 쿠키가 HTTP 헤더안에 포함되어 전송된다.



### Set-Cookie, Cookie 헤더

Set-Cookie HTTP 응답 헤더는 서버로부터 클라이언트에게 전송된다. 

```
Set-Cookie: <cookie-name>=<cookie-value>
```

이 서버 헤더는 클라이언트에게 쿠키를 저장하라고 전달한다.

(예시:)

```
HTTP/1.0 200 OK
Content-type: text/html
Set-Cookie: beer1_token=abcd1234
Set-Cookie: beer1_other_value=1111
```



이 후, 서버로 전송되는 모든 요청과 함께 브라우저는 Cookie 헤더를 사용하여 쿠키 값을 전달한다.

```
GET /api/path HTTP/1.1
Host: www.beer1.com
Cookie: beer1_token=abcd1234; beer1_other_value=1111
```



### 라이프타임

* 세션 쿠키는 현재 세션이 끝날 때 삭제된다. 브라우저는 현재 세션이 끝나는 시점을 정의하고 어떤 브라우저는 재시작할 때 세션을 복원해 세션 쿠키가 무기한 존재할 수 있도록 한다.
* 영속적인 쿠키는 Expires 속성에 명시된 날짜에 삭제되거나 Max-Age 속성에 명시된 기간 이후에 삭제된다.

```
Set-Cookie: beer1_token=abcd1234; Expires=Wed, 21 Jul 2021 12:11:10 GMT;
```



## Secure 쿠키, HttpOnly 쿠키

Secure 쿠키는 HTTPS 프로토콜 상에서 암호화된 요청일 경우에만 전송된다. 

HttpOnly 쿠키는 Document.cookie API로 접근이 불가능하며 서버에 전송되기만 한다. 따라서 XSS 공격으로 쿠키를 탈취하는 것으로부터 막을 수 있다. (오직 서버에서 확인하기 위한 값이면 HttpOnly 사용)

`Set-Cookie: beer1_token=abcd1234; Expires=Wed, 21 Jul 2021 12:11:10 GMT; Secure; HttpOnly`



## SameSite

Set-Cookie HTTP Response header 의 SameSites 속성을 사용하면 쿠키를 동일 사이트의 컨텍스트로 제한해야하는지에 대한 여부를 설정할 수 있다. sameSite의 값은 여러가지가 있다.



### Value

sameSite 속성이 지원하는 값은 총 3가지가 있다.

**Lax**: Cookie가 일반적인 cross-site 하위 요청으로 전달되지 않지만 사용자가 origin site로 이동할 때 전송된다. 이 값은 최근 브라우저 버전에 sameSite가 명시되지 않은 브라우저에 대해서 default값이다. cookie에 default sameSite(Lax) 값을 제공하는 최신버전의 브라우저는 콘솔에 아래 메시지가 표시될 수 있다.

> Cookie "myCookie" has "SameSite" policy set to "Lax" because it is missing a "SameSite" attribute, and "SameSite=Lax" is the default value for this attribute.

sameSite를 지원하는 브라우저에서 쿠키의 sameSite 값이 없을 때 아래 메시지가 발생하는데, 이게 거슬린다면 sameSite값을 명시적으로 지정해주면 된다. (브라우저마다 sameSite 값이 다를 수 있기 때문에 waring message를 제공해주는 것 같다.)



**Strict**: 무조건 first-party context에서만 보내진다. first-party 가 아닌 곳으로는 절대 보내지 않는다.



**None**: 모든 context에 대해 보내진다. 만약에 None으로 설정했다면 Cookie에 Secure 속성을 설정해야한다. sameSite=None이지만 secure 속성이 없는 쿠키를 전달받으면 다음과 같은 오류가 발생한다.

> Cookie "myCookie" rejected because it has the "SameSite=None" attribute but is missing the "secure" attribute.
> 
> This Set-Cookie was blocked because it had the "SameSite=None" attribute but did not have the "Secure" attribute, which is required in order to use "SameSite=None".





# Chrome에서의 계획

Chrome은 Chrome 84를 출시하면서 cookie의 sameSite의 default 값을 변경하였다. Chrome 80을 출시하면서 2020년 2월부터 sameSite 변경사항을 적용했는데 2020년 여름까지 변경사항을 일시적으로 롤백했다. sameSite 변경사항은 보안과 프라이버스를 강화하였지만 쿠키에 의존하는 기존 서비스 로직들이 Chrome의 변경사항에 대해 대응하지 않는다면 문제가 될 수 있다. (롤백한 이유가 sameSite 관련된 이슈가 터졌기 때문이 아닐까..)



Cookie의 sameSite 속성은 cross-domain으로의 요청에 대한 제어를 담당한다. Chrome Platform Status에서는 sameSite 속성의 의도를 다음과 같이 설명한다.

> SameSite는 일부 CSRF 공격에 대해 상당히 강력한 방어수단이지만, 개발자는 sameSite 속성을 지정하여 CSRF 공격 보호에 신경써야 한다. 달리 말하면, 개발자는 기본적으로 CSRF 공격에 취약하다. 이러한 변화 (sameSite default 값 변경)은 개발자들에게 기본적으로 CSRF 공격에 대해 보호될 수 있을 뿐 아니라 사이트간 요청에서 상태가 필요한 사이트도 보안이 취약한 현상의 모델을 선택할 수 있다.



만약 sameSite 속성이 지정되지 않는다면, Chrome 84에서는 기본적으로 cookie에 `sameSite = Lax`로 지정해준다. Chrome 84가 릴리즈되지 전까지는 sameSite의 기본값은 `None`이었다. Chrome 84가 출시된 이후에는 개발자는 `sameSite = None; secure`를 명시적으로 설정하여 제한 없이 사용할 수 있다. (https가 제공되어야겠지?)



그리고 [chromium 블로그](https://blog.chromium.org/2020/01/building-more-private-web-path-towards.html)에서는 `Building a more private web: A path towards making third party cookies obsolete` 이라는 제목의 글이 있는데 앞으로 쿠키정책이 어떻게 바뀔 것인지와 관련된 내용을 소개하고 있다. (2020년 1월 글)



Chrome의 생각은 웹 애플리케이션(웹 브라우저)에서 개인정보 보호를 강화하기 위해서 여러 계획이 있는데, 그 중 하나는 2년 내로 chrome에 있는 third-party cookie에 대한 지원을 단계적으로 중단하는 것이다. 즉, sameSite = None 지원을 중단하려는 계획으로 보인다.

먼저, 개인정보 보안을 더욱 강화하기 위해서 sameSite가 포함되지 않는 쿠키를 first-party로만 취급하여 (sameSite = Lax) 보안되지 않은 cross-site tracking을 제한하고, third-party 쿠키에는 HTTPS 를 통해서만 접근할 수 있도록 할 예정이라고 한다. (secure 속성 추가 필수) (2020.02월, 이미 시작했음) 



현재는 thrid-party cookie를 사용해야만 하는 서비스들은 sameSite = None; secure를 설정하면 HTTPS 환경에서 쿠키를 사용할 수 있긴 하지만, 머지않아 쿠키를 third-party에서 사용할 수 없을 수도 있다. 브라우저 정책이 바뀌면 예상치 못한 변경사항이 생길 수 있다는게 참 끔찍하다고 생각이 든다. 나는 프론트와 백엔드가 분리되어있는 환경에서 백엔드는 MSA구조의 API 서버, 프론트는 Vue-js로 구현하여 토이프로젝트를 진행하고 있는데 JWT를 어디에 넣어야 할까 고민하다가 쿠키까지 공부하게 되었는데 결국 MSA 환경에서는 쿠키로 토큰을 넣으면 안된다 라는 결론이 나왔다. 먼저 토큰을 주는 서버의 도메인과 프론트의 도메인은 서로 다른 도메인이라 first-party가 아니며, 그렇게 하려면 third-party를 지원하도록 쿠키의 sameSite 값을 None으로 설정해야 하는데, 이는 chrome에서 deprecated 될거라고 예고했기 때문에 언젠가는 사용할 수 없는 방법이 될 것이다. 그래서 결론은 JWT를 LocalStorage에 저장하고 토큰을 RequestHeader에 넣기로 하였다. 대신 XSS에 취약하기 때문에 이를 방어하기 위해 많은 노력을 기울여야 할 듯 하다.











