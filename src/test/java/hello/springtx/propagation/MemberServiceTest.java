package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
public class MemberServiceTest {
    @Autowired MemberRepository memberRepository;
    @Autowired MemberService memberService;
    @Autowired LogRepository logRepository;

    /**
     * MemberService     @Transactional:OFF
     * MemberRepository  @Transactional:ON
     * LogRepository     @Transactional:ON
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";
        //when
        memberService.joinV1(username);
        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * MemberService     @Transactional:OFF
     * MemberRepository  @Transactional:ON
     * LogRepository     @Transactional:ON Exception
     * 트랜잭션 AOP는 해당 런타임 예외를 확인하고 롤백처리 된다.
     * 이 경우 회원은 저장되고 로그는 롤백되므로 데이터 정합성에 문제가 발생할 수 있다. -> 둘을 하나의 트랜잭션으로 묶는것이 바람직
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외_outerTxOff_fail"; // 로직 구성지, 사용자 이름에 로그예외 라는 단어가 포함되면 런타임 예외가 발생한다.
        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);
        //then : 완전히 롤백되지 않고, member 데이터가 남아서 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * MemberService @Transactional:ON
     * MemberRepository @Transactional:OFF
     * LogRepository @Transactional:OFF
     * 하나의 트랜잭션으로 묶기 : 두 가지 repo를 호출하는 회원 서비스에만 트랜잭션을 사용
     */
    @Test
    void singleTx() {
        //given
        String username = "singleTx";
        //when
        memberService.joinV1(username);
        //then : 모든 데이터가 정상 저장된다.
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }
}
