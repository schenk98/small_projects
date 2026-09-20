package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "class_memberships")
@Getter
@Setter
@NoArgsConstructor
public class ClassMembership {

    @EmbeddedId
    private ClassMembershipId id;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by_user_id", nullable = false)
    private Long assignedByUserId;
}

