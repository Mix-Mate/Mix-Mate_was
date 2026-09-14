package com.mixmate.domain.participant.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.dto.response.ParticipantProfileResponse;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import com.mixmate.domain.participant.enums.Visibility;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * "관리자가 조회하면 비공개 프로필이어도 403 대신 200을 내려준다"는 수정 하나만 좁게 검증한다.
 * ParticipantService의 나머지 기능(명단조회, 차단 등)은 이 프로젝트에서 내가 만든 기능이 아니라
 * 이 테스트의 범위 밖이다.
 */
@ExtendWith(MockitoExtension.class)
class ParticipantServiceProfileVisibilityTest {

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private GroupMembership groupMembership;

    @InjectMocks
    private ParticipantService participantService;

    private Group group;
    private long nextId = 1L;

    @BeforeEach
    void setUp() {
        group = Group.create("테스트 모임", null, "ABC12345", "tokenVisibilityAAAAAAA");
    }

    private Participant participant(com.mixmate.domain.participant.enums.Role role, Visibility visibility) {
        ParticipantProfile profile = ParticipantProfile.builder()
                .displayName("참가자").studentId("2024001").position(Position.MEMBER)
                .major("컴퓨터공학과").isNew(false).grade(Grade.SECOND).gender(Gender.MALE)
                .mbti(Mbti.ISTJ).visibility(visibility).build();
        User user = User.builder().userId(nextId).email("u" + nextId + "@example.com").build();
        Participant participant = role == com.mixmate.domain.participant.enums.Role.HOST
                ? Participant.createHost(user, group, profile)
                : Participant.join(user, group, profile);
        ReflectionTestUtils.setField(participant, "participantId", nextId++);
        return participant;
    }

    @Test
    @DisplayName("본인은 자신의 비공개 프로필을 조회할 수 있다")
    void selfCanViewOwnPrivateProfile() {
        Participant me = participant(com.mixmate.domain.participant.enums.Role.PARTICIPANT, Visibility.PRIVATE);

        when(groupMembership.getMember(1L, 1L)).thenReturn(me);
        when(participantRepository.findByParticipantIdAndGroup(me.getParticipantId(), group))
                .thenReturn(Optional.of(me));

        ParticipantProfileResponse result = participantService.getParticipantProfile(1L, me.getParticipantId(), 1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("관리자는 다른 사람의 비공개 프로필도 조회할 수 있다")
    void hostCanViewOthersPrivateProfile() {
        Participant host = participant(com.mixmate.domain.participant.enums.Role.HOST, Visibility.PUBLIC);
        Participant target = participant(com.mixmate.domain.participant.enums.Role.PARTICIPANT, Visibility.PRIVATE);

        when(groupMembership.getMember(1L, 1L)).thenReturn(host);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));

        ParticipantProfileResponse result = participantService.getParticipantProfile(1L, target.getParticipantId(), 1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("관리자가 아닌 다른 참가자는 남의 비공개 프로필을 볼 수 없다")
    void nonHostCannotViewOthersPrivateProfile() {
        Participant viewer = participant(com.mixmate.domain.participant.enums.Role.PARTICIPANT, Visibility.PUBLIC);
        Participant target = participant(com.mixmate.domain.participant.enums.Role.PARTICIPANT, Visibility.PRIVATE);

        when(groupMembership.getMember(1L, 1L)).thenReturn(viewer);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));

        assertThatThrownBy(() -> participantService.getParticipantProfile(1L, target.getParticipantId(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("공개 프로필은 관리자가 아니어도 누구나 조회할 수 있다")
    void anyoneCanViewPublicProfile() {
        Participant viewer = participant(com.mixmate.domain.participant.enums.Role.PARTICIPANT, Visibility.PUBLIC);
        Participant target = participant(com.mixmate.domain.participant.enums.Role.PARTICIPANT, Visibility.PUBLIC);

        when(groupMembership.getMember(1L, 1L)).thenReturn(viewer);
        when(participantRepository.findByParticipantIdAndGroup(target.getParticipantId(), group))
                .thenReturn(Optional.of(target));

        ParticipantProfileResponse result = participantService.getParticipantProfile(1L, target.getParticipantId(), 1L);

        assertThat(result).isNotNull();
    }
}
