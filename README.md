# 인프런 강의 : 스프링 DB 2편 - 데이터 접근 활용기술 (part2.트랜잭션)
- 그룹 : spring-db
---
## 스프링 트랜잭션
- 트랜잭션 추상화, 스프링 부트의 자동 주입
    - DB 기술에 따라 트랜잭션을 사용하는 코드가 모두 달랐다.
    - 기술 변경 시 코드 변경을 최소화 하기 위해서 스프링은 트랜잭션 추상화를 통해 이를 해결했다 -> `PlatformTransactionManager`
      ```java
      public interface PlatformTransactionManager extends TransactionManager {
          getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
        
          void commit(TransactionStatus status) throws TransactionException;
        
          void rollback(TransactionStatus status) throws TransactionException;
      }
      ```
    - 또한 스프링은 트랜잭션 매니저의 구현체도 제공하기 때문에 필요한 구현체를 스프링 빈으로 등록하고 주입 받아 사용만 하면 된다.
    - 스프링부트는 어떤 데이터 접근 기술을 사용하는지를 자동으로 인식해서 적절한 트랜잭션 매니저를 선택해서 빈으로 등록해준다.
- 선언적 트랜잭션과 AOP
    - 트랜잭션을 처리하기 위한 프록시를 도입하기 전에는 서비스 로직에서 트랜잭션을 직접 시작했다.

### 스프링 트랜잭션 적용 확인
- @Transaction 이 특정 클래스나 메서드에 하나라도 있으면 트랜잭션 AOP는 프록시를 만들어서 스프링 컨테이너에 등록한다.
- 그리고 프록시는 내부에 실제 객체를 참조하게 된다. 
- ` TransactionSynchronizationManager.isActualTransactionActive()` 
  - 현재 쓰레드에 트랜잭션이 적용되어 있는지 확인할 수 있는 기능 

### 트랜잭션 적용 위치에 따른 우선순위 
- 항상 더 구체적이고 자세한 것이 높은 우선순위를 가진다.
  - 메서드 -> 클래스 -> 인터페이스 
- TxLevelTest.class


