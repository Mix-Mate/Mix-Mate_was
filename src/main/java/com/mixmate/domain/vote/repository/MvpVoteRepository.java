package com.mixmate.domain.vote.repository;

import com.mixmate.domain.group.entity.Group;
import com.mixmate.domain.participant.entity.Participant;
import com.mixmate.domain.vote.entity.MvpVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MvpVoteRepository extends JpaRepository<MvpVote, Long> {

    boolean existsByVoter(Participant voter);

    List<MvpVote> findByTarget_Group(Group group);
}
