package com.mixmate.domain.group.repository;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Group 엔티티 매핑이 실제 MySQL에서 의도대로 동작하는지 확인합니다.
 *
 * 내장 DB로 치환하지 않고 로컬 MySQL을 그대로 씁니다.
 * 테스트가 끝나면 롤백되므로(DataJpaTest는 기본이 트랜잭션) 로컬 DB에 데이터가 남지 않습니다.
 *
 * 실행 조건: 로컬 MySQL이 떠 있어야 합니다.
 *   꺼져 있으면 개별 테스트가 아니라 스프링 컨텍스트 로딩부터 실패해서 이 클래스 전체가 죽습니다.
 *   컴파일은 DB와 무관하므로 영향받지 않습니다.
 *
 * TODO: CI를 도입하면 CI 서버에는 MySQL이 없어 이 클래스가 깨집니다.
 *       그 시점에 Testcontainers 전환을 팀과 논의할 것.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GroupRepositoryTest {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("그룹을 저장하면 PK와 createdAt이 채워지고 상태는 최초 배정 전이다")
    void save() {
        Group group = Group.create("신촌 모임", "1차 술자리", "ABC12345");

        Group saved = groupRepository.save(group);
        em.flush();

        assertThat(saved.getGroupId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(GroupStatus.BEFORE_FIRST_ASSIGNMENT);
        assertThat(saved.getCreatedAt()).isNotNull();   // JPA Auditing 동작 확인
    }

    @Test
    @DisplayName("초대 코드로 그룹을 찾을 수 있고, 없는 코드면 비어 있다")
    void findByInviteCode() {
        groupRepository.save(Group.create("신촌 모임", null, "ABC12345"));
        em.flush();
        em.clear();

        Optional<Group> found = groupRepository.findByInviteCode("ABC12345");
        Optional<Group> notFound = groupRepository.findByInviteCode("NOPE9999");

        assertThat(found).isPresent();
        assertThat(found.get().getGroupName()).isEqualTo("신촌 모임");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("테이블은 예약어를 피해 event_group 이름으로 생성된다")
    void tableName() {
        Object count = em.createNativeQuery("select count(*) from event_group").getSingleResult();

        assertThat(count).isNotNull();
    }

    @Test
    @DisplayName("status는 ordinal이 아니라 문자열로 저장된다")
    void statusStoredAsString() {
        groupRepository.save(Group.create("신촌 모임", null, "STR12345"));
        em.flush();

        Object status = em.createNativeQuery(
                        "select status from event_group where invite_code = 'STR12345'")
                .getSingleResult();

        assertThat(status).isEqualTo("BEFORE_FIRST_ASSIGNMENT");
    }
}
