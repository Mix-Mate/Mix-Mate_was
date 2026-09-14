package com.mixmate.domain.home.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupBanRepository;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.home.dto.request.HomeGroupJoinByLinkReqDto;
import com.mixmate.domain.home.dto.request.HomeGroupJoinReqDto;
import com.mixmate.domain.home.dto.request.HomeInviteCodeVerifyReqDto;
import com.mixmate.domain.home.dto.response.HomeGroupListResDto;
import com.mixmate.domain.home.dto.response.HomeGroupSummaryResDto;
import com.mixmate.domain.home.dto.response.HomeInviteCodeVerifyResDto;
import com.mixmate.domain.participant.dto.request.ParticipantProfileRequest;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.participant.repository.ParticipantRepository;
import com.mixmate.exception.CustomException;
import com.mixmate.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메인 홈 화면에서 쓰이는 그룹 참여코드 검증, 입장, 내 그룹 목록 조회를 처리하는 서비스입니다.
 * 그룹 생성/수정/삭제는 이 클래스가 아니라 group 패키지의 GroupService가 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class HomeGroupService {

    private final GroupRepository groupRepository;
    private final GroupBanRepository groupBanRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    /**
     * 참여코드가 실제로 존재하고, 만료(3일)되지 않았으며, 아직 모집중인 그룹인지 검증합니다.
     * 프로필 입력 화면으로 넘어가기 전, 코드만 먼저 빠르게 확인할 때 사용합니다.
     *
     * @param dto 검증할 참여코드
     * @return 검증된 그룹의 최소 정보
     */
    @Transactional(readOnly = true)
    public HomeInviteCodeVerifyResDto verifyInviteCode(HomeInviteCodeVerifyReqDto dto) {
        Group group = findValidGroupByInviteCode(dto.getInviteCode());
        return HomeInviteCodeVerifyResDto.fromEntity(group);
    }

    /**
     * 참여코드와 프로필을 함께 받아 그룹에 일반 참가자(PARTICIPANT)로 입장시킵니다.
     * 모집이 마감됐거나(RECRUITING이 아님) 이미 참여중이면 거부합니다.
     *
     * @param dto 참여코드와 본인 프로필
     * @param userId 입장하는 사용자 식별자
     * @return 입장한 그룹의 최소 정보
     */
    @Transactional
    public HomeInviteCodeVerifyResDto joinGroup(HomeGroupJoinReqDto dto, Long userId) {
        return join(findValidGroupByInviteCode(dto.getInviteCode()), dto.getProfile(), userId);
    }

    /**
     * 초대 링크 토큰이 가리키는 그룹이 아직 유효하고 모집중인지 검증합니다.
     * 링크를 누른 사람에게 그룹 이름을 먼저 보여주는 화면이 이 응답을 씁니다.
     *
     * @param inviteToken 초대 링크에 담긴 22자 토큰
     * @return 검증된 그룹의 최소 정보
     */
    @Transactional(readOnly = true)
    public HomeInviteCodeVerifyResDto verifyInviteToken(String inviteToken) {
        return HomeInviteCodeVerifyResDto.fromEntity(findValidGroupByInviteToken(inviteToken));
    }

    /**
     * 초대 링크로 들어온 사용자를 일반 참가자(PARTICIPANT)로 입장시킵니다.
     * 그룹은 경로의 토큰이 지목하므로 참여코드는 받지 않습니다.
     *
     * @param inviteToken 초대 링크에 담긴 22자 토큰
     * @param dto 본인 프로필
     * @param userId 입장하는 사용자 식별자
     * @return 입장한 그룹의 최소 정보
     */
    @Transactional
    public HomeInviteCodeVerifyResDto joinGroupByToken(String inviteToken, HomeGroupJoinByLinkReqDto dto, Long userId) {
        return join(findValidGroupByInviteToken(inviteToken), dto.getProfile(), userId);
    }

    /**
     * 참여코드로 들어왔든 초대 링크로 들어왔든, 그룹이 정해진 뒤의 입장 처리는 같다.
     */
    private HomeInviteCodeVerifyResDto join(Group group, ParticipantProfileRequest profile, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (participantRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED);
        }
        if (groupBanRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.BANNED_FROM_GROUP);
        }

        participantRepository.save(Participant.join(user, group, profile.toEntity()));
        return HomeInviteCodeVerifyResDto.fromEntity(group);
    }

    /**
     * 로그인한 사용자 본인이 참여중인(관리자·일반 참여자 모두 포함) 그룹 목록을 조회한다.
     * state=active면 FINISHED를 제외한 진행중인 그룹만, state=finished면 FINISHED인 그룹만,
     * state=banned면 차단당한 그룹 목록을 내려준다. 참여/차단 그룹이 없으면 빈 배열을 담아 200으로 응답한다.
     *
     * 차단당하면 Participant 행 자체가 삭제되므로(관리자가 대신 등록한 것처럼 취급되지 않는 한),
     * banned는 participant가 아니라 GroupBan을 별도로 조회한다.
     */
    @Transactional(readOnly = true)
    public HomeGroupListResDto getMyGroups(Long userId, String scope, String state) {
        if (!"me".equalsIgnoreCase(scope)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<HomeGroupSummaryResDto> groups;
        if ("banned".equalsIgnoreCase(state)) {
            groups = groupBanRepository.findAllByUserWithGroup(user).stream()
                    .map(HomeGroupSummaryResDto::fromBan)
                    .toList();
            return HomeGroupListResDto.builder().groups(groups).build();
        }

        List<Participant> participants;
        if ("active".equalsIgnoreCase(state)) {
            participants = participantRepository.findByUserAndGroup_StatusNot(user, GroupStatus.FINISHED);
        } else if ("finished".equalsIgnoreCase(state)) {
            participants = participantRepository.findByUserAndGroup_Status(user, GroupStatus.FINISHED);
        } else {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        groups = participants.stream()
                .map(p -> HomeGroupSummaryResDto.fromEntity(
                        p.getGroup(), p.getRole(), participantRepository.countByGroup(p.getGroup())))
                .toList();

        return HomeGroupListResDto.builder().groups(groups).build();
    }

    /**
     * 참여코드는 Group.INVITE_VALID_DAYS일 동안, 그리고 모집중(RECRUITING)일 때만 유효하다.
     * 참여코드는 6자리라 무작위 대입이 가능하므로, 만료된 코드를 없는 코드와 같은 에러로 뭉갠다.
     */
    private Group findValidGroupByInviteCode(String inviteCode) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));

        if (isExpired(group)) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE);
        }
        return validateRecruiting(group);
    }

    /**
     * 초대 링크 토큰의 유효 기간은 참여코드와 같다.
     * 다만 토큰은 128비트라 열거가 불가능하므로, 없는 링크와 만료된 링크를 구분해서 알려준다.
     */
    private Group findValidGroupByInviteToken(String inviteToken) {
        Group group = groupRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_LINK));

        if (isExpired(group)) {
            throw new CustomException(ErrorCode.EXPIRED_INVITE_LINK);
        }
        return validateRecruiting(group);
    }

    private boolean isExpired(Group group) {
        return group.getInviteExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * 모집이 마감된 그룹은 참여코드로 들어오든 링크로 들어오든 똑같이 막는다.
     */
    private Group validateRecruiting(Group group) {
        if (group.getStatus() != GroupStatus.RECRUITING) {
            throw new CustomException(ErrorCode.INVALID_GROUP_STATUS, "참가자 모집이 마감된 그룹입니다.");
        }
        return group;
    }
}
