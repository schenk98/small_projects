package com.readingtracker.repository;

import com.readingtracker.domain.ClassMembership;
import com.readingtracker.domain.ClassMembershipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassMembershipRepository extends JpaRepository<ClassMembership, ClassMembershipId> {
}

