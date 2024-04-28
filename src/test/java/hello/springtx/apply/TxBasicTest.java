package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 적용 여부 확인 테스트
 */
@Slf4j
@SpringBootTest
public class TxBasicTest {
    @Autowired
    BasicService basicService;

    @Test
    void proxyCheck() {
        log.info("aop class={}", basicService.getClass());
        Assertions.assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }
    @Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }
    @TestConfiguration
    static class TxApplyBasicConfig {
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
    }
    @Slf4j
    static class BasicService {

        @Transactional // 해당 애노테이션으로 BasicService 객체는 프록시객체로 스프링 빈에 등록된다.
        public void tx() {
            log.info("call tx");
            boolean isTcActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", isTcActive);
        }

        public void nonTx() {
            log.info("call nonTx");
            boolean isTcActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", isTcActive);
        }
    }
}

