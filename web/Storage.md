# Storage

브라우저에서 제공하는 여러 웹 스토리지에 대해서 공부한 내용을 정리해보자. Chrome 개발자 도구에 나와있는 스토리지 목록은 다음과 같다.
<img width="188" alt="스크린샷 2021-08-14 오전 12 41 07" src="https://user-images.githubusercontent.com/35602698/129751949-a208e922-7e0b-4ca4-8b9c-ac8980db7b5c.png">



## Local Storage

key-value 구조로 되어있는 스토리지이다. LocalStorage에 데이터를 저장하면 저장한 데이터를 명시적으로 지우지 않는 이상 영구적으로 보관이 된다. Local Storage는 도메인별로 관리가 된다. 

<img width="532" alt="스크린샷 2021-08-14 오전 1 04 12" src="https://user-images.githubusercontent.com/35602698/129752006-6a26b420-15c1-436a-9e4e-7583dd55f49f.png">



Local Stroage 접근은 window.localStorage로 접근하여 조회하거나 저장할 수 있다. 

**값 넣기**

```javascript
localStorage.setItem('foo', 1);
localStorage.setItem('bar', 2);
```

**값 조회**

```javascript
localStorage.getItem('foo'); // "1"
```

**값 삭제**

```javascript
localStorage.removeItem('foo');

localStorage.clear();
```



## Session Storage

Session Storage도 Local Storage와 마찬가지로 key-value 구조로 되어있는 저장소이다. 하지만 Session Storage는 Local Storage와는 다르게 영구적으로 저장되지 않고 페이지 세션이 끝날 때 제거된다. Session Storage는 세션이나 창별로 독립적으로 구성된다.



### 페이지 세션

* 페이지 세션은 새로고침과 페이지 복구를 해도 유지된다.
* 페이지를 새로운 탭이나 창에서 열면 새로운 세션을 생성한다.
* 탭이나 창을 닫으면 세션이 끝나고 Session Storage 안의 객체를 초기화한다.



Session Storage 접근은 window.sessionStorage로 접근하여 조회하거나 저장할 수 있다.



**값 넣기**

```javascript
sessionStorage.setItem('foo', 1);
sessionStorage.setItem('bar', 2);
```



**값 조회**

```javascript
sessionStorage.getItem('foo'); // 1
```



## Indexed DB

Indexed DB는 파일이나 블롭 등의 많은 양의 구조화된 데이터를 저장하기 위한 로우레벨 API이다. Index를 사용하여 데이터를 고성능으로 탐색할 수 있다. 따라서 많은 양의 구조화된 데이터를 저장하여 사용할 때 유용하다.

Indexed DB는 SQL을 사용하는 RDBMS와 같이 트랜잭션을 사용하는 데이터베이스 시스템이다. 그러나 Indexed DB는 RDBMS의 고정 컬럼 대신 javascript 기반의 객체지향 DB이다. 



