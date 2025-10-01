package com.amrita.amritabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.amrita.amritabackend.model.Group;
import com.amrita.amritabackend.model.GroupMember;
import com.amrita.amritabackend.model.User;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // ✅ Find all members of a group
    List<GroupMember> findByGroup(Group group);

    // ✅ Find all groups for a user
    List<GroupMember> findByUser(User user);

    // ✅ Check if user already in group
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
}
