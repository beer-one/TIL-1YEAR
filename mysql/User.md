# Mysql 유저

### 유저 확인

```sql
use mysql;
select host, user from user;
```



### 유저 생성

```sql
create user testuser;	// 유저 생성
create user testuser@'a.b.c.d' identified by '********'; // 특정 IP에서 접속할 수 있는 유저 생성. + 비밀번호 설정 
create user testuser@'%' identified by '********'; // 모든 IP에서 접속할 수 있는 유저 생성. + 비밀번호설정
```



### 유저 삭제

```sql
drop user testuser;
```



### 유저에게 권한 추가

```sql
grant all privileges on databaseName.* to testuser@'a.b.c.d' identified by '********';
```

