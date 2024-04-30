package hello.springtx.propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * joinV1() : 회원과 DB로그를 함께 남기는 비즈니스 로직
 * joinV2() : joinV1() 기능 + DB로그 저장시 예외가 발생하면 예외를 복수한다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final LogRepository logRepository;

    public void joinV1(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info(">> start memberRepository >>");
        memberRepository.save(member);
        log.info(">> end memberRepository >>");

        log.info(">> start logRepository >>");
        logRepository.save(logMessage);
        log.info(">> end logRepository >>");
    }

    public void joinV2(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info(">> start memberRepository >>");
        memberRepository.save(member);
        log.info(">> end memberRepository >>");

        log.info(">> start logRepository >>");

        try {
          logRepository.save(logMessage);
        } catch (RuntimeException e) {
            log.info(" fail log save. logMessage = {}", logMessage.getMessage());
            log.info("정상 흐름 변환");
        }
        log.info(">> end logRepository >>");
        }

}
