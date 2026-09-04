package com.mixmate.domain.participant.repository;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Participant 엔티티 매핑이 실제 MySQL에서 의도대로 동작하는지 확인합니다.
 *
 * 실행 조건은 GroupRepositoryTest와 같습니다. 로컬 MySQL이 떠 있어야 하고,
 * 꺼져 있으면 스프링 컨텍스트 로딩부터 실패해 이 클래스 전체가 죽습니다.
 *
 * User는 팀원(auth 담당) 영역이므로 빌더로 최소 필드만 채워 씁니다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParticipantRepositoryTest {

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private EntityManager em;

    private Group group;
    private User user;

    @BeforeEach
    void setUp() {
        group = Group.create("신촌 모임", "1차 술자리", "PTC12345");
        em.persist(group);
        user = insertUser("김대현", "kdh@example.com");
        em.flush();
    }

    @Test
    @DisplayName("그룹 생성자는 HOST로, 참여 차수는 1차만으로 저장된다")
    void createHost() {
        Participant saved = participantRepository.save(
                Participant.createHost(user, group, profile("김대현")));
        em.flush();

        assertThat(saved.getParticipantId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.HOST);
        assertThat(saved.getRoundParticipation()).isEqualTo(RoundParticipation.FIRST_ONLY);
        assertThat(saved.getProfileUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("초대코드로 입장한 사람은 PARTICIPANT로 저장된다")
    void join() {
        Participant saved = participantRepository.save(
                Participant.join(user, group, profile("김대현")));
        em.flush();

        assertThat(saved.getRole()).isEqualTo(Role.PARTICIPANT);
    }

    @Test
    @DisplayName("관리자가 대리 등록한 참가자는 계정이 없어 user가 null이며, 한 그룹에 여러 명 들어갈 수 있다")
    void addByHost() {
        participantRepository.save(Participant.addByHost(group, profile("문찬주")));
        participantRepository.save(Participant.addByHost(group, profile("최수아")));
        em.flush();
        em.clear();

        List<Participant> found = participantRepository.findByGroup(group);

        // user_id가 NULL인 행은 (group_id, user_id) 유니크 제약에 걸리지 않는다
        assertThat(found).hasSize(2);
        assertThat(found).allSatisfy(p -> assertThat(p.getUser()).isNull());
    }

    @Test
    @DisplayName("같은 유저가 같은 그룹에 두 번 입장하면 유니크 제약에 걸린다")
    void duplicateJoinRejected() {
        participantRepository.save(Participant.join(user, group, profile("김대현")));

        // PK 전략이 IDENTITY라 persist 시점에 INSERT가 바로 나간다.
        // 쓰기 지연이 없으므로 flush가 아니라 save에서 곧장 터진다.
        assertThatThrownBy(() ->
                participantRepository.save(Participant.join(user, group, profile("김대현"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("프로필은 별도 테이블 없이 participant 컬럼으로 펼쳐진다")
    void profileIsEmbedded() {
        participantRepository.save(Participant.join(user, group, profile("김대현")));
        em.flush();

        Object[] row = (Object[]) em.createNativeQuery(
                        "select display_name, major, is_new from participant where user_id = :userId")
                .setParameter("userId", user.getUserId())
                .getSingleResult();

        assertThat(row[0]).isEqualTo("김대현");
        assertThat(row[1]).isEqualTo("컴퓨터공학과");
    }

    @Test
    @DisplayName("enum은 ordinal이 아니라 문자열로 저장된다")
    void enumsStoredAsString() {
        participantRepository.save(Participant.createHost(user, group, profile("김대현")));
        em.flush();

        Object[] row = (Object[]) em.createNativeQuery(
                        "select role, round_participation, gender, visibility from participant where user_id = :userId")
                .setParameter("userId", user.getUserId())
                .getSingleResult();

        assertThat(row).containsExactly("HOST", "FIRST_ONLY", "MALE", "PUBLIC");
    }

    @Test
    @DisplayName("그룹과 유저로 참가 여부를 확인하고 본인을 조회할 수 있다")
    void findByGroupAndUser() {
        participantRepository.save(Participant.join(user, group, profile("김대현")));
        em.flush();
        em.clear();

        User other = insertUser("이서연", "lsy@example.com");

        assertThat(participantRepository.existsByGroupAndUser(group, user)).isTrue();
        assertThat(participantRepository.existsByGroupAndUser(group, other)).isFalse();

        Optional<Participant> found = participantRepository.findByGroupAndUser(group, user);
        assertThat(found).isPresent();
        assertThat(found.get().getProfile().getDisplayName()).isEqualTo("김대현");
    }

    @Test
    @DisplayName("프로필을 교체하면 수정 시각이 갱신된다")
    void updateProfileBumpsTimestamp() {
        Participant participant = participantRepository.save(
                Participant.join(user, group, profile("김대현")));
        em.flush();
        var before = participant.getProfileUpdatedAt();

        participant.updateProfile(profile("김대현(수정)"));
        em.flush();

        assertThat(participant.getProfileUpdatedAt()).isAfterOrEqualTo(before);
        assertThat(participant.getProfile().getDisplayName()).isEqualTo("김대현(수정)");
    }

    @Test
    @DisplayName("참가 정보를 조회하면 그룹까지 함께 로딩돼 추가 쿼리가 나가지 않는다")
    void findWithGroupFetchesGroupEagerly() {
        participantRepository.save(Participant.join(user, group, profile("김대현")));
        em.flush();
        em.clear();     // 1차 캐시를 비워야 프록시인지 실제로 채워진 엔티티인지 구분된다

        Participant found = participantRepository
                .findWithGroupByGroupIdAndUserId(group.getGroupId(), user.getUserId())
                .orElseThrow();

        // 쿼리에서 join fetch가 빠지면 group은 미초기화 프록시가 되어 이 단언만 깨진다.
        // 그때도 쿼리는 성공하고 응답도 그대로라, 이 테스트가 없으면 조용히 되돌아간다.
        boolean groupLoaded = em.getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .isLoaded(found, "group");

        assertThat(groupLoaded).isTrue();
        assertThat(found.getGroup().getGroupName()).isEqualTo("신촌 모임");
    }

    @Test
    @DisplayName("그룹이나 유저가 어긋나면 빈 결과가 나온다")
    void findWithGroupReturnsEmptyWhenNotMember() {
        participantRepository.save(Participant.join(user, group, profile("김대현")));
        User other = insertUser("이서연", "lsy@example.com");
        em.flush();
        em.clear();

        // 그룹은 있지만 참가자가 아닌 경우 — 호출부가 403으로 가른다
        assertThat(participantRepository
                .findWithGroupByGroupIdAndUserId(group.getGroupId(), other.getUserId())).isEmpty();

        // 그룹 자체가 없는 경우 — 호출부가 404로 가른다
        assertThat(participantRepository
                .findWithGroupByGroupIdAndUserId(-1L, user.getUserId())).isEmpty();
    }

    @Test
    @DisplayName("같은 그룹에 계정 없는 참가자가 있어도 요청자 본인 것만 조회된다")
    void findWithGroupIgnoresParticipantsWithoutAccount() {
        participantRepository.save(Participant.addByHost(group, profile("문찬주")));
        participantRepository.save(Participant.join(user, group, profile("김대현")));
        em.flush();
        em.clear();

        Participant found = participantRepository
                .findWithGroupByGroupIdAndUserId(group.getGroupId(), user.getUserId())
                .orElseThrow();

        assertThat(found.getProfile().getDisplayName()).isEqualTo("김대현");
    }

    private ParticipantProfile profile(String displayName) {
        return ParticipantProfile.builder()
                .displayName(displayName)
                .position(Position.MEMBER)
                .major("컴퓨터공학과")
                .isNew(false)
                .grade(Grade.THIRD)
                .gender(Gender.MALE)
                .mbti(Mbti.ISTP)
                .age(24)
                .instaId("@su8oe")
                .bio("잘 부탁드립니다")
                .visibility(Visibility.PUBLIC)
                .build();
    }

    private User insertUser(String userName, String email) {
        User user = User.builder()
                .userName(userName)
                .email(email)
                .password("encoded-password")   // 인증 로직을 타지 않으므로 평문이어도 무방하다
                .build();
        em.persist(user);
        return user;
    }
}
