# Cookie

HTTP에서 쿠키는 서버가 클라이언트의 웹 브라우저에 전송하는 작은 데이터 조각이다. 브라우저는 서버에서 쿠키를 받아 저장해두었다가 동일한 서버에 재 요청 시 쿠키와 함께 데이터를 전송한다. 

https://developer.mozilla.org/ko/docs/Web/HTTP/Cookies



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



**Strict**: 무조건 first-party context에서만 보내진다. 



**None**: 모든 context에 대해 보내진다. 만약에 None으로 설정했다면 Cookie에 Secure 속성을 설정해야한다. sameSite=None이지만 secure 속성이 없는 쿠키를 전달받으면 다음과 같은 오류가 발생한다.

> Cookie "myCookie" rejected because it has the "SameSite=None" attribute but is missing the "secure" attribute.
> 
> This Set-Cookie was blocked because it had the "SameSite=None" attribute but did not have the "Secure" attribute, which is required in order to use "SameSite=None".





# Chrome의 입장?

원문은 여기에.. https://help.salesforce.com/s/articleView?id=000351874&type=1



