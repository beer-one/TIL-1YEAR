# 값 타입 컬렉션

JPA에서 값 타입을 하나 이상 저장하려면 컬렉션에 보관하고 @ElementCollection, @CollectionTable 어노테이션을 사용하면 된다.

윈래 RDB는 컬렉션 자체를 하나의 컬럼에 담을 수 없기 때문에 값 타입 컬렉션을 만드려면 다른 테이블을 매핑해서 만들어야 한다.



```kotlin
@Entity
class Member(
	@Id @GeneratedValue
  val id: Long,
  
  ...
  
) {
  @ElementCollection
  @CollectionTable(
    name = "ADDRESS",
    joinColumns = [JoinColumn(name = "MEMBER_ID")]
  )
  val addressHistories: MutableList<Address> = mutableListOf()
}

@Embeddable
class Address(
	val city: String,
  val street: String,
  val zipCode: String, 
  
  ...
)
```

* @ElementCollection: 값 타입 컬렉션임을 명시
* @CollectionTable: 컬렉션을 가져올 테이블
  * name: 테이블 이름
  * JoinColumns: 컬렉션을 조인으로 가져오기 위한 조인 컬럼



## 조회

* 조회 시 기본적으로 FetchType = LAZY을 가진다. 

* 값 객체를 반드시 조회해야 할 경우에는 레포지토리의 @EntityGraph를 이용하거나 QueryDsl의 join  / fetchJoin을 이용하여 조회하는 것이 바람직하다고 생각한다.



## 수정(추가 / 삭제)

* 값 타입 컬렉션을 수정하면 전체 값 타입 컬렉션 삭제(delete) 후 모든 값 타입 컬렉션을 새로 추가(insert) 한다. 즉, N개의 값이 있는데 하나를 변경하면 하나의 delete 쿼리와 N개의 insert 쿼리가 발생한다.
* 추가 및 삭제도 마찬가지이다.



## 제약사항

먼저, 값 타입 컬렉션은 `불변` 객체를 전제로 한다.

엔티티는 식별자가 있기 때문에 엔티티 값을 변경해도 식별자로 데이터베이스에 저장된 원본 데이터를 가져와 수정할 수 있지만, 값 타입은 식별자라는 개념이 없고 단순한 값들의 모음이므로 값을 변경해버리면 데이터베이스에 저장된 데이터를 찾기 힘들다. 그렇기 때문에 변경이 발생하면 모두 삭제하고 다시 추가하게 된다.

따라서 값 타입 컬렉션에 있는 값이 상당히 많게 된다면 값 타입 대신 엔티티를 사용하는 것이 적절하다.



## 특징

값 타입 컬렉션은 기본적으로 영속성 전이(cascade) + 고아객체 제거(ORPHAN REMOVE) 기능이 있다.