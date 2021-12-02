# ProxySQL



## 설치

설치는 [공식 홈페이지](https://proxysql.com/documentation/installing-proxysql/)를 따라하면 될 것 같다. 여러 환경에서 (우분투, 도커, 쿠버네티스 등등..) 에서 설치방법이 다양해보인다. 쿠버네티스 환경이라면 [github](https://github.com/proxysql/kubernetes) 페이지에서 설치 방법을 확인할 수 있다.



## 구성

https://proxysql.com/documentation/ProxySQL-Configuration/

일단 proxysql의 어드민 계정으로 접속하자.

```shell
$ mysql -u admin -padmin -h 127.0.0.1 -P6032 --prompt='Admin> '
```



처음에는 로드밸런싱 될 mysql 서버를 구성해야 한다. 일단 mysql 서버와 관련된 테이블을 조회해보자. 초기에는 empty set이 정상이다.

```mysql
Admin> SELECT * FROM mysql_servers;
Empty set (0.00 sec)

Admin> SELECT * from mysql_replication_hostgroups;
Empty set (0.00 sec)

Admin> SELECT * from mysql_query_rules;
Empty set (0.00 sec)
```



### Mysql backend 서버 추가

Mysql 백엔드 서버를 추가해보자. 로드밸런싱할 mysql 백엔드 서버는 `mysql_server` 테이블에서 관리한다.

```mysql
INSERT INTO mysql_servers(hostgroup_id,hostname,port) VALUES (1,'master-server',3306);
INSERT INTO mysql_servers(hostgroup_id,hostname,port) VALUES (1,'slave-server',3306);
```

* Mysql 백엔드 서버에는 모든 복제본에 대해 `read_only = 1`이 구성되어 있다. 
* ProxySQL은  `read_only = 0` 값을 가진 백엔드 인스턴스는 WRITER 인스턴스로 여기기 때문에 MySQL 서버에서 세팅해줘야 한다. 



### 모니터링 설정

ProxySQL은 Mysql 백엔드 서버의 상태를 지속적으로 모니터링한다. 백엔드를 모니터링하기 위한 credential은 MySQL에서 생성해야 하며 환경별 검사 간격과 함께 ProxySQL에서도 구성해야 한다.

Mysql에서 유저를 생성하려면 PRIMARY에 연결하고 다음 명령어를 실행하자.

```mysql
mysql> UPDATE global_variables SET variable_value='monitor' WHERE variable_name='mysql-monitor_username';
```



그 후 모니터링 유저의 credential을 ProxySQL에 추가하자.

```mysql
Admin> UPDATE global_variables SET variable_value='monitor' WHERE variable_name='mysql-monitor_username';
Query OK, 1 row affected (0.00 sec)

Admin> UPDATE global_variables SET variable_value='monitor' WHERE variable_name='mysql-monitor_password';
Query OK, 1 row affected (0.00 sec)
```



그 후 모니터링 interval을 구성하자.

```mysql
Admin> UPDATE global_variables SET variable_value='2000' WHERE variable_name IN ('mysql-monitor_connect_interval','mysql-monitor_ping_interval','mysql-monitor_read_only_interval');
Query OK, 3 rows affected (0.00 sec)
```



그 다음, `global_variables` 의 Mysql Monitor 변경 사항은 `LOAD MYSQL VARIABLES TO RUNTIME` 명령어가 실행된 후 적용될 것이다. 재시작 시 구성 변경사항을 유지하려면 `SAVE MYSQL VARIABLES TO DISK` 명령어를 실행해야 한다.

```mysql
Admin> LOAD MYSQL VARIABLES TO RUNTIME;
Query OK, 0 rows affected (0.00 sec)

Admin> SAVE MYSQL VARIABLES TO DISK;
Query OK, 54 rows affected (0.02 sec)
```



### Backend 헬스체크

구성이 활성화 되었다면 ProxySQL 어드민의 `monitor` 테이블에 있는 Mysql 백엔드의 상태를 확인해보자.

```mysql
ProxySQL Admin> SHOW TABLES FROM monitor;
```





### Mysql replication hostgroups

클러스터 토폴로지 변경사항은 ProxySQL에 구성된 Mysql replication hostgroups에 기반하여 모니터링된다. ProxySQL은 `mysql_replication_hostgroups` 에 구성된 호스트그룹에 구성된 서버에서 `read_only` 값을 모니터링하여 replication topology를 이해한다.

해당 테이블은 기본적으로 비어있고 READER와 WRITER 호스트그룹 쌍을 지정하여 구성해야 한다.

```mysql
Admin> SHOW CREATE TABLE mysql_replication_hostgroups\G
*************************** 1. row ***************************
       table: mysql_replication_hostgroups
Create Table: CREATE TABLE mysql_replication_hostgroups (
writer_hostgroup INT CHECK (writer_hostgroup>=0) NOT NULL PRIMARY KEY,
reader_hostgroup INT NOT NULL CHECK (reader_hostgroup<>writer_hostgroup AND reader_hostgroup>0),
check_type VARCHAR CHECK (LOWER(check_type) IN ('read_only','innodb_read_only','super_read_only','read_only|innodb_read_only','read_only&innodb_read_only')) NOT NULL DEFAULT 'read_only',
comment VARCHAR,
UNIQUE (reader_hostgroup))
1 row in set (0.00 sec)

Admin> INSERT INTO mysql_replication_hostgroups (writer_hostgroup,reader_hostgroup,comment) VALUES (1,2,'cluster1');
Query OK, 1 row affected (0.00 sec)
```

여기서는 호스트그룹이 1이나 2로 구성된 모든 Mysql 백엔드 서버는 `read_only` 값에 따라 해당 호스트그룹에 배치된다.

*  `read_only = 0` -> hostgroup 1
* `read_only = 1` -> host group 2



`LOAD MYSQL SERVERS TO RUNTIME` 가 `mysql_servers` 와 `mysql_replication_hostgroups` 테이블 모두 처리하기 때문에 replication hostgroup을 활성화하려면 Mysql 서버에서 사용된 것과 같은 `LOAD` 명령어를 사용하여  `mysql_replication_hostgroups` 를 런타임에 로드한다. 

```mysql
Admin> LOAD MYSQL SERVERS TO RUNTIME;
Query OK, 0 rows affected (0.00 sec)
```



`read_only` 확인 결과는 `monitor` DB의 `mysql_servers_read_only_log` 테이블에 로깅된다.

```mysql
Admin> SELECT * FROM monitor.mysql_server_read_only_log ORDER BY time_start_us DESC LIMIT 3;
```



마지막으로 구성을 디스크에 유지하자.

```mysql
Admin> SAVE MYSQL SERVERS TO DISK;
Query OK, 0 rows affected (0.01 sec)

Admin> SAVE MYSQL VARIABLES TO DISK;
Query OK, 54 rows affected (0.00 sec)
```



### MySQL Users

`mysql_servers` 내의 MySQL 서버 백엔드를 구성하였다면 다음 스텝은 MySQL Users를 구성하는 것이다.

User를 구성하는 방법은 `mysql_users` 테이블에 값을 추가하면 된다.

```mysql
Admin> INSERT INTO mysql_users(username,password,default_hostgroup) VALUES ('root','',1);
Query OK, 1 row affected (0.00 sec)

Admin> INSERT INTO mysql_users(username,password,default_hostgroup) VALUES ('stnduser','stnduser',1);
Query OK, 1 row affected (0.00 sec)
```

* `default_hostgroup` 을 정의하여 사용자가 BY DEFAULT에 연결해야 하는 백엔드 서버를 지정한다.



```mysql
ProxySQL Admin> LOAD MYSQL USERS TO RUNTIME;
Query OK, 0 rows affected (0.00 sec)

ProxySQL Admin> SAVE MYSQL USERS TO DISK;
Query OK, 0 rows affected (0.01 sec)
```

























