package com.mixmate.domain.home.service;

import com.mixmate.domain.auth.entity.User;
import com.mixmate.domain.auth.repository.UserRepository;
import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.group.enums.GroupStatus;
import com.mixmate.domain.group.repository.GroupBanRepository;
import com.mixmate.domain.group.repository.GroupRepository;
import com.mixmate.domain.home.dto.request.HomeGroupJoinReqDto;
import com.mixmate.domain.home.dto.request.HomeInviteCodeVerifyReqDto;
import com.mixmate.domain.home.dto.response.HomeGroupListResDto;
import com.mixmate.domain.home.dto.response.HomeGroupSummaryResDto;
import com.mixmate.domain.home.dto.response.HomeInviteCodeVerifyResDto;
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

    private static final long INVITE_CODE_VALID_DAYS = 3;

    private final GroupRepository groupRepository;
    private final GroupBanRepository groupBanRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    /**
     * 참여코드가 실제로 존재하고 아직 만료(3일)되지 않은 그룹인지 검증합니다.
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Group group = findValidGroupByInviteCode(dto.getInviteCode());

        if (group.getStatus() != GroupStatus.RECRUITING) {
            throw new CustomException(ErrorCode.GROUP_LOCKED);
        }
        if (participantRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED);
        }
        if (groupBanRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.BANNED_FROM_GROUP);
        }

        Participant participant = Participant.join(user, group, dto.getProfile().toEntity());
        participantRepository.save(participant);
        return HomeInviteCodeVerifyResDto.fromEntity(group);
    }

    /**
     * 로그인한 사용자 본인이 참여중인(관리자·일반 참여자 모두 포함) 그룹 목록을 조회한다.
     * state=active면 FINISHED를 제외한 진행중인 그룹만, state=finished면 FINISHED인 그룹만 내려준다.
     * 참여 그룹이 없으면 빈 배열을 담아 200으로 응답한다.
     */
    @Transactional(readOnly = true)
    public HomeGroupListResDto getMyGroups(Long userId, String scope, String state) {
        if (!"me".equalsIgnoreCase(scope)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Participant> participants;
        if ("active".equalsIgnoreCase(state)) {
            participants = participantRepository.findByUserAndGroup_StatusNot(user, GroupStatus.FINISHED);
        } else if ("finished".equalsIgnoreCase(state)) {
            participants = participantRepository.findByUserAndGroup_Status(user, GroupStatus.FINISHED);
        } else {
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        List<HomeGroupSummaryResDto> groups = participants.stream()
                .map(p -> HomeGroupSummaryResDto.fromEntity(
                        p.getGroup(), p.getRole(), participantRepository.countByGroup(p.getGroup())))
                .toList();

        return HomeGroupListResDto.builder().groups(groups).build();
    }

    /**
     * 참여코드는 그룹 생성일로부터 INVITE_CODE_VALID_DAYS일 동안만 유효하다.
     * 만료된 코드는 존재하지 않는 코드와 동일하게 취급해 사용자에게 같은 에러를 보여준다.
     */
    private Group findValidGroupByInviteCode(String inviteCode) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));

        LocalDateTime expiresAt = group.getCreatedAt().plusDays(INVITE_CODE_VALID_DAYS);
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_INVITE_CODE);
        }
        return group;
    }
}
