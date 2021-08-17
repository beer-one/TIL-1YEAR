# JWT

JWT(Json Web Token)은 Json 객체로서 사용자의 정보를 안전하게 전송하기 위한 방법을 정의하는 RFC7519 표준이다. JWT는 디지털 서명이 되어있기 때문에 검증되고 신뢰할 수 있다. 

JWT는 HMAC 알고리즘의 secret이나 RSA 또는 ECDSA 를 사용한 public/private key를 사용하여 서명이 된다.



## JWT는 언제 사용되나?

JWT는 다음과 같은 상황에서 사용할 수 있다.

* **Authorization**: 보통 이 때 많이 사용한다. 사용자가 한번 로그인을 한 후의 요청들은 JWT를 포함하여 요청한다. 사용자 정보를 조회하거나 조작할 때 JWT로 권한을 확인한다. JWT로 권한을 확인한다면 세션을 통해 권한을 확인하는 것 보다 오버헤드가 줄어들고 다른 도메인에서도 권한 확인이 쉬워진다.
* **Information Exchange**: JWT는 외부 서비스들 사이에서 정보를 안전하게 전송하는 방법 중 하나이다. 



## JWT 구조

JWT의 구조는 3가지 부분으로 이루어져있다.

* Header
* Payload
* Signature



3가지 부분으로 나뉘어져있고, JWT를 생성하면 다음과 같이 생긴다.

<img width="629" alt="스크린샷 2021-08-13 오후 6 22 59" src="https://user-images.githubusercontent.com/35602698/129751763-c4c0baef-1135-4790-b934-f8a317595637.png">

* 출처 - https://jwt.io/introduction



### Header

헤더는 typ(타입)과 alg(알고리즘) 두개의 부분으로 나뉘어진다. 헤더는 다음과 같은 Json 객체를 Base64Url로 인코딩한 값이다.

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

* typ은 `JWT`로 고정이다.
* alg는 `HMAC SHA256`, `RSA` 와 같은 서명 알고리즘이다.



### Payload

Payload는 claim들로 구성되어있다. Claim은 전반적인 데이터에 관한 설명이다. claim은 3가지 타입 (`registered`, `public`, `private`이 있다.

* **Registered claims**: 해당 claim에는 사전 정의된 클레임이 열거되어있다. 이는 의무는 아니지만 유용하고 상호 운용 가능한 claim을 제공하기 위해 권장되는 방식이다. 이들 중 몇개는 `iss(issuer)`, `exp(expiration time)`, `sub(subject)`, `aud(audience)` 등이 있다. (others: [link](https://tools.ietf.org/html/rfc7519#section-4.1)) 
* **Public claims**: 해당 claim은 JWT를 사용하는 사람들에 의해 마음대로 정의할 수 있다. 그러나 충돌을 방지하기 위해 [IANA JWT Registry](https://www.iana.org/assignments/jwt/jwt.xhtml)에 정의되거나 충돌 방지 네임스페이스를 포함하는 URI로 정의되어야 한다.
* **Private claims**: claim 사용에 동의한 외부 서비스 사이에 정보를 공유하기 위해 생성된 custom claim이다. 

claim의 예시로는 다음과 같다. Payload도 마찬가지로 Json을 Base64Url 인코딩된 형식으로 되어있다.

```json
{ 
	"sub": "1234567890",
  "name": "beer1",
  "admin": true
}
```



### Signature

Signature를 생성하기 위해서는 인코딩된 header, 인코딩된 payload, secret, 헤더에 정의된 알고리즘을 가져와 서명해야한다. 예를 들어 HMAC SHA256 알고리즘을 사용하려면 signature는 다음과 같은 방식으로 생성되어야 한다.

```kotlin
HMACSHA256(
	base64UrlEncode(header) + "." +
	base64UrlEncode(payload),
	secret
)
```



signature는 메시지가 도중에 변하지 않았는지 검증하기 위해 사용된다. 그리고 private key로 서명된 토큰에 대해서는 JWT를 보낸 사용자가 토큰에 서명된 사용자인지도 검증한다.



## JWT는 어떤 방식으로 작동하나?

인증 단계에서 사용자가 성공적으로 로그인을 한다면 JWT가 반환된다. 반환되는 JWT는 민증과 같은 사용자 증명서기 때문에 보안 이슈 대비와 함께 아주 조심히 다뤄야한다. 보통, 필요이상으로 길게 JWT를 가지고있지 않아야 한다. 또는 민감한 세션 데이터를 [browser storage](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html#local-storage)에 저장하면 안된다. 

사용자가 보호된 route나 자원에 접근하려고 할 때 마다 사용자는 요청할 때 JWT를 함께 보내야 한다. 보통 JWT를 `Bearer` 스키마를 사용한 **Authorization headear** 에 담아 보낸다.

```
Authorization: Bearer <token>
```



이러한 JWT 방식은 어떻게 보면 무상태(stateless)의 인가 방식 메커니즘이다. 서버의 보호된 route는 Authorization header에 있는 JWT가 유효한지 확인할 것이다. 그리고 JWT가 유효하면 보호된 자원에 대한 접근을 허용한다. JWT에 필요한 데이터가 포함되어있을 경우 특정 작업에 대해 DB를 조회할 필요가 줄어들 수도 있다. 만약 토큰이 Authorization 헤더로 전송될 경우, 쿠키를 사용하지 않기 때문에 CORS가 문제되지 않는다.





## JWT는 어디에 저장하는게 좋나?

사실 이에 대한 고민을 많이하고 관련 자료를 많이 찾아봤는데 완전한 답은 없는 듯 하다. 일단 웹 애플리케이션에서의 저장소는 대표적으로 Local Storage, Cookie, Session Storage







### MSA 관점에서 보는 Cookie와 CORS, JWT

