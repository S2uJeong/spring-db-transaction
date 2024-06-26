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

## 트랜잭션 AOP가 작동 하지 않아 트랜잭션이 시작하지 않는 경우 
### 01. 프록시와 메서드 내부 호출
- `@Transaction`을 사용하면 스프링의 트랜잭션 AOP가 적용되며 프록시 방식을 사용한다.
- 대상 객체의 내부에서 메서드 호출이 발생하면 프록시를 거치지 않고 대상 객체를 직접 호출하는 문제가 발생한다. 그럼 트랜잭션이 적용되지 않는다.
  ```java
  @Slf4j
  @SpringBootTest
  public class InternalCallV1Test {
  
  @Autowired CallService callService;

    @Test
    void printProxy() {
        log.info("callService class = {}", callService.getClass());
    }
    @Test
    void internalCall() {
         callService.internal();
    }
    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class IntervalCallV1TestConfig {
        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {

        public void external() {
            log.info("call external");
            printTxInfo();
            //===== 아래 코드 부터만 트랜잭션이 필요해서 분리한 상황 (의도는 그렇다. 실제론 트랜잭션 작동 안함)
            internal();
        }
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {} ", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx read = {}", readOnly);
        }
    }
   }
  ```
- 문제원인
  - ![img.png](img/프록시와_내부호출.png) 
  - 자바 언어에서 메서드 앞에 별도의 참조가 없으면 this라는 뜻으로 자기 자신의 인스턴스를 가리킨다.
- 해결 
  - 이를 보완하기 위해 트랜잭션을 사용해야 하는 메서드를 별도의 클래스로 분리하는 방법이 있을 수 있다.
  ```java
  @Slf4j
  @SpringBootTest
  public class InternalCallV2Test {
    @Autowired CallService callService;

    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class IntervalCallV2TestConfig {
        @Bean
        CallService callService() {
            return new CallService(innerService());
        }
        @Bean
        InternalService innerService() {
            return new InternalService();
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    static class CallService {
        private final InternalService internalService;
        public void external() {
            log.info("call external");
            printTxInfo();
            internalService.internal();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {} ", txActive);
        }
    }

    @Slf4j
    static class InternalService {
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();

        }
        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {} ", txActive);
        }
    }
  }
  ```
  - ![img.png](img/프록시와_내부호출_해결후.png)
### 02. public 메서드에만 트랜잭션이 적용됨
- 스프링의 트랜잭션 AOP 기능은 `public` 메서드에서만 트랜잭션을 적용하도록 기본설정 되어 있다.
- 트랜잭션은 주로 비즈니스 로직의 시작점에 걸기 때문에 대부분 외부에 열어준 곳을 시작점으로 사용한다. 
- 이런 이유로 이런 규칙이 만들어 졌다. 

### 03. 초기화 시점
- 스프링 초기화 시점에는 트랜잭션 AOP가 적용되지 않을 수 있다. 
- 초기화 코드가 먼저 호출되고, 그 다음에 트랜잭션 AOP가 적용되기 때문이다. 
- 해결 방법으로 `@PostConstruct` 보다는 `@EventListener(ApplicationReadyEvent.class)`을 사용하는 것이다. (트랜잭션이 필요하다면)
```java
    static class Hello{
        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init @PostConstruct tc active = {}", isActive);
        }
        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            log.info("");
        }
    }
```
## 트랜잭션 옵션
- 추후 필요할 때 복습

## 예외와 트랜잭션션 커밋, 롤백
- 예외가 발생하고, 트랜잭션 범위 밖으로 예외를 던지게 되면
- 스프링 트랜백션 AOP는 예외의 종류에 따라 트랜잭션을 커밋하거나 롤백한다.
  - 롤백 : 언체크 예외 - `RuntimeException`, `Error`와 그  하위 예외 
  - 커밋 : 체크 예외 - `Exception`
- 스프링은 왜 체크 예외는 커밋하고 언체크 예외를 롤백할까? 
  - 기본적으로 체크 예외는 비즈니스 의미가 있을 때 사용하고, 런타임 예외는 복구 불가능한 예외로 가정하기 때문이다. 
  - 비즈니스 상황에 따라 체크 예외를 롤백하고 싶으면 `rollbackFor` 옵션을 사용한다. 
- 커밋/롤백 여부 로그 확인 코드
  - application.properties
    ```properties
    logging.level.org.springframework.transaction.interceptor=TRACE
    logging.level.org.springframework.jdbc.datasource.DataSourceTransactionManager=D
    EBUG
    #JPA log
    logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG
    logging.level.org.hibernate.resource.transaction=DEBUG
    ```
- ```java
  @Slf4j
    static class RollbackService{
        // 런타임 예외 발생 : 롤백
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }
        // 체크 예외 발생 : 커밋
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedExceprion");
            throw new MyException();
        }
        // 체크 예외 rollbackFor 옵션으로 지정 : 롤백
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }

    }
  ```
    
## 트랜잭션 전파 (propagation)
- 트랜잭션이 이미 진행중인데, 여기에 추가로 트랜잭션을 또 수행하려고 한다면 어떻게 해야할까.
- 이때 어떻게 동작할지 결정하는 것을 트랜잭션 전파라고 한다. 
- 스프링은 다양한 트랜잭션 전파 옵션을 제공한다.


### 트랜잭션 종류 정리 
- 물리/논리 트랜잭션
  - 물리 : DB에 적용되는 트랜잭셔
  - 논리 : 트랜잭션 매니저를 통해 트랜잭션을 사용하는 단위
  - 논리 트랜잭션 개념은 트랜잭션이 진행되는 중에 내부에 추가로 트랜잭션을 사용하는 경우에 나타난다. 
- 외부/내부 트랜잭션
  - 외부 : 먼저 호출된 트랜잭션
  - 내부 : 외부 트랜잭션 이후에 호출된 트랜잭션 
  
### 기본 옵션 - `REQUIRED`
- 외부 트랜잭션과 내부 트랜잭션을 묶어서 하나의 트랜잭션으로 만듬.
- **내부 트랜잭션이 외부 트랜잭션에 참여하는 것이다.**
- 클라이언트에 가까울 수록 외부이며, 먼저 실행된 트랜잭션이 외부라고 할 수 있다.

- 원칙
  - 모든 논리 트랜잭션이 커밋되어야 물리 트랜잭션이 커밋된다.
  - 하나의 논리 트랜잭션이라도 롤백되면 물리 트랜잭션은 롤백된다. 

- Participating in existing transaction
  - 트랜잭션은 묶이지만, 한 트랙잭션에 커밋 및 롤백을 여러번 할 수 있는데. 어떻게 가능한걸까?
  - 내부 트랜잭션이 외부 트랜잭션에 참여한다는 뜻 =  외부 트랜잭션만 물리 트랜잭션을 시작하고, 커밋한다
  - ![img.png](img/트랜잭션_전파.png)
  - ![img.png](img/트랜잭션_전파2.png)
  - 그렇다면 커밋/롤백 권한을 가진 외부 트랜잭션의 로직은 성공하고 내부 트랜잭션은 로직이 실패하여 `Rollback`을 실행한다면 어떻게 될까.
    - 이때, 내부 트랜잭션에서 롤백을 실제로 실행하면, 외부 트랜잭션까지 이어지지 않아 오류가 발생할 것이다.
    - 따라서 내부 트랜잭션은 물리 트랜잭션을 롤백하지 않는 대신에 트랜잭션 동기화 매니저에 `rollbackOnly=true`라는 표시를 한다.
    - 이후 외부 트랜잭션에서 커밋시점에 동기화 매니저에 표시를 보고 커밋이 아닌 롤백한다. 
    - 스프링은 이 경우 `UnecpectedRollbackException` 런타임 예외를 던진다. 

### 옵션 - `Requires_new`
- 외부 트랜잭션과 내부 트랜잭션을 완전히 분리해서 사용하는 방법 = 각각 별도의 물리 트랜잭션을 사용
- 이 옵션에서의 동장방식은, 이전과 달리 내부 트랜잭션을 시작할 깨 기존 트랜잭션에 참여하는게 아니라 새로운 물리 트랜잭션을 만들어서 시작한다 .
- 이 방법은 커넥션이 동시에 2개가 사용된다는 점을 주의해야 한다. 
```java
  public class LogRepository {
    private final EntityManager em;
  
    @Transactional(propagation = Propagation.REQUIRES_NEW) // REQUIRES_NEW 설정
    public void save(Log logMessage) { ...}
  }
```
```java
    /**
     * MemberService @Transactional:ON
     * MemberRepository @Transactional:ON
     * LogRepository @Transactional(REQUIRES_NEW) Exception
     */
    @Test
    void recoverException_success() {
            //given
            String username = "로그예외_recoverException_success";
            //when
            memberService.joinV2(username); // 예외 잡는 로직 포함된 함수

            //then : member 저장, log 롤백 
            assertTrue(memberRepository.find(username).isPresent());
            assertTrue(logRepository.find(username).isEmpty());
            }
```
- ![img.png](img/requires_new.png)

### 서비스단에서 트랜잭션을 시작하라.
- /propagation/*
- Repo를 호출하는 서비스단에서 트랜잭션을 처리하게 되면 논리/물리/외부/내부 트랜잭션 과 관련된 트랜잭션 전파와 같은 것을 고민할 필요 없이
- 한 트랜잭션으로 묶을 수 있다.
- 서비스단의 시작부터 끝까지, 관련 로직은 해당 트랜잭션이 생성한 커넥션을 사용하게 된다. 
- 이렇게 구성하면, 물리 트랜잭션으로 함께 묶여 있는 논리 트랜잭션 중 하나라도 error나 rollback이 발생하면 rollback이 되어 데이터 연산이 이뤄지지 않는다.
  - 비즈니스 요구 중, 회원 가입을 시도한 로그를 남기는데 실패하더라도 회원 가입은 유지되어야 한다는 조건이 있다면 어떻게 할까 
  - 참고로 서비스/repo단에 다 트랜잭션 처리를 한 뒤, 서비스단에서 예외를 잡는다고 해도, 이미 내부에서 rollbackOnly가 표시되면 외부에서 commit될 시 `UnexpectedRollbackException`가 발생됨
- 로그와 멤버 repo처럼 커밋과 롤백 조건을 달리해야하는 경우 `Requires_new` 옵션을 활용하라.