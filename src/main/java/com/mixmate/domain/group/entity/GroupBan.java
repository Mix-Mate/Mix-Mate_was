package com.mixmate.domain.group.entity;

import com.mixmate.domain.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_ban", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GroupBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long banId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private String displayName;

    @Column(length = 30)
    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성자와 시그니처가 같지만, 다른 엔티티처럼 생성 경로를 팩토리 하나로 고정해 둔다.(다른 코드 스타일 과의 일관성)
    public static GroupBan create(User user, Group group, String displayName, String reason) {
        return new GroupBan(user, group, displayName, reason);
    }

    private GroupBan(User user, Group group, String displayName, String reason) {
        this.user = user;
        this.group = group;
        this.displayName = displayName;
        this.reason = reason;
    }
}
