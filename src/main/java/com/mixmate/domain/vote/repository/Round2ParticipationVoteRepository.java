package com.mixmate.domain.vote.repository;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.vote.entity.Round2ParticipationVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Round2ParticipationVoteRepository extends JpaRepository<Round2ParticipationVote, Long> {

    boolean existsByVoter(Participant voter);

    List<Round2ParticipationVote> findByVoter_Group(Group group);
}
