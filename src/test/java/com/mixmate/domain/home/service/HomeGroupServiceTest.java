package com.mixmate.domain.home.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.entity.GroupBan;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupBanRepository;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.home.dto.request.HomeGroupJoinReqDto;
import com.mixmate.domain.home.dto.request.HomeInviteCodeVerifyReqDto;
import com.mixmate.domain.home.dto.response.HomeGroupListResDto;
import com.mixmate.domain.home.dto.response.HomeInviteCodeVerifyResDto;
import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.entity.ParticipantProfile;
import com.mixmate.domain.participant.enums.Gender;
import com.mixmate.domain.participant.enums.Grade;
import com.mixmate.domain.participant.enums.Mbti;
import com.mixmate.domain.participant.enums.Position;
import com.mixmate.domain.participant.enums.Role;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeGroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupBanRepository groupBanRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HomeGroupService homeGroupService;

    private User user;
    private Group group;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).userName("곽동욱").email("kdw@example.com").build();
        group = Group.create("테스트 모임", "설명", "ABC12345");
        ReflectionTestUtils.setField(group, "createdAt", LocalDateTime.now());
    }

    private ParticipantProfileRequest profileRequest() {
        return new ParticipantProfileRequest("참가자", "2024001", Position.MEMBER, "컴퓨터공학과",
                false, Grade.SECOND, Gender.MALE, Mbti.ISTJ, null, null, null, Visibility.PUBLIC);
    }

    // ---------- verifyInviteCode ----------

    @Test
    @DisplayName("유효한 참여코드면 그룹 정보를 돌려준다")
    void verifyInviteCodeSucceeds() {
        when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(group));

        HomeInviteCodeVerifyResDto result = homeGroupService.verifyInviteCode(
                HomeInviteCodeVerifyReqDto.builder().inviteCode("ABC12345").build());

        assertThat(result.getGroupName()).isEqualTo("테스트 모임");
    }

    @Test
    @DisplayName("존재하지 않는 참여코드면 실패한다")
    void verifyInviteCodeFailsWhenNotFound() {
        when(groupRepository.findByInviteCode("NOPE0000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> homeGroupService.verifyInviteCode(
                HomeInviteCodeVerifyReqDto.builder().inviteCode("NOPE0000").build()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INVITE_CODE);
    }

    @Test
    @DisplayName("발급된 지 3일이 지난 참여코드는 만료된 것으로 취급한다")
    void verifyInviteCodeFailsWhenExpired() {
        ReflectionTestUtils.setField(group, "inviteIssuedAt", LocalDateTime.now().minusDays(4));
        when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> homeGroupService.verifyInviteCode(
                HomeInviteCodeVerifyReqDto.builder().inviteCode("ABC12345").build()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INVITE_CODE);
    }

    // ---------- joinGroup ----------

    private HomeGroupJoinReqDto joinDto() {
        return HomeGroupJoinReqDto.builder().inviteCode("ABC12345").profile(profileRequest()).build();
    }

    @Test
    @DisplayName("모집중인 그룹에 처음 참여하면 참가자로 등록된다")
    void joinGroupSucceeds() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(group));
        when(participantRepository.existsByGroupAndUser(group, user)).thenReturn(false);
        when(groupBanRepository.existsByGroupAndUser(group, user)).thenReturn(false);

        HomeInviteCodeVerifyResDto result = homeGroupService.joinGroup(joinDto(), 1L);

        assertThat(result.getGroupName()).isEqualTo("테스트 모임");
        org.mockito.Mockito.verify(participantRepository).save(any(Participant.class));
    }

    @Test
    @DisplayName("모집이 마감된 그룹에는 참여할 수 없다")
    void joinGroupFailsWhenNotRecruiting() {
        group.closeRecruiting(); // BEFORE_FIRST_ROUND
        // 참여코드 검증에서 먼저 걸리므로 사용자 조회까지 가지 않는다
        when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> homeGroupService.joinGroup(joinDto(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_GROUP_STATUS);
    }

    @Test
    @DisplayName("이미 참여중인 그룹에는 다시 참여할 수 없다")
    void joinGroupFailsWhenAlreadyJoined() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(group));
        when(participantRepository.existsByGroupAndUser(group, user)).thenReturn(true);

        assertThatThrownBy(() -> homeGroupService.joinGroup(joinDto(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_JOINED);
    }

    @Test
    @DisplayName("차단당한 그룹에는 참여할 수 없다")
    void joinGroupFailsWhenBanned() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupRepository.findByInviteCode("ABC12345")).thenReturn(Optional.of(group));
        when(participantRepository.existsByGroupAndUser(group, user)).thenReturn(false);
        when(groupBanRepository.existsByGroupAndUser(group, user)).thenReturn(true);

        assertThatThrownBy(() -> homeGroupService.joinGroup(joinDto(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.BANNED_FROM_GROUP);
    }

    // ---------- getMyGroups ----------

    @Test
    @DisplayName("scope가 me가 아니면 거부한다")
    void getMyGroupsFailsWhenScopeInvalid() {
        assertThatThrownBy(() -> homeGroupService.getMyGroups(1L, "other", "active"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    @DisplayName("state가 알 수 없는 값이면 거부한다")
    void getMyGroupsFailsWhenStateInvalid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> homeGroupService.getMyGroups(1L, "me", "unknown"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    @DisplayName("state=active면 종료되지 않은 참여중인 그룹 목록을 돌려준다")
    void getMyGroupsReturnsActiveGroups() {
        ParticipantProfile profile = ParticipantProfile.builder()
                .displayName("참가자").studentId("2024001").position(Position.MEMBER)
                .major("컴퓨터공학과").isNew(false).grade(Grade.SECOND).gender(Gender.MALE)
                .mbti(Mbti.ISTJ).visibility(Visibility.PUBLIC).build();
        Participant participant = Participant.join(user, group, profile);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(participantRepository.findByUserAndGroup_StatusNot(user, GroupStatus.FINISHED))
                .thenReturn(List.of(participant));
        when(participantRepository.countByGroup(group)).thenReturn(1L);

        HomeGroupListResDto result = homeGroupService.getMyGroups(1L, "me", "active");

        assertThat(result.getGroups()).hasSize(1);
        assertThat(result.getGroups().get(0).getRole()).isEqualTo(Role.PARTICIPANT);
    }

    @Test
    @DisplayName("state=banned면 차단당한 그룹 목록을 돌려준다")
    void getMyGroupsReturnsBannedGroups() {
        GroupBan ban = GroupBan.create(user, group, "참가자", "부적절한 행동");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(groupBanRepository.findAllByUserWithGroup(user)).thenReturn(List.of(ban));

        HomeGroupListResDto result = homeGroupService.getMyGroups(1L, "me", "banned");

        assertThat(result.getGroups()).hasSize(1);
        assertThat(result.getGroups().get(0).getReason()).isEqualTo("부적절한 행동");
    }
}
