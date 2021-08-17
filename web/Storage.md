# Storage

브라우저에서 제공하는 여러 웹 스토리지에 대해서 공부한 내용을 정리해보자. Chrome 개발자 도구에 나와있는 스토리지 목록은 다음과 같다.

![스크린샷 2021-08-14 오전 12.41.07](/Users/yunseowon/Desktop/스크린샷 2021-08-14 오전 12.41.07.png)



## Local Storage

key-value 구조로 되어있는 스토리지이다. LocalStorage에 데이터를 저장하면 저장한 데이터를 명시적으로 지우지 않는 이상 영구적으로 보관이 된다. Local Storage는 도메인별로 관리가 된다. 

![image-20210814010415506](/Users/yunseowon/Library/Application Support/typora-user-images/image-20210814010415506.png)



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



