package com.amrita.amritabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.amrita.amritabackend.model.Group;
import com.amrita.amritabackend.model.GroupType;
import com.amrita.amritabackend.model.User;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // ✅ Find groups created by a user
    List<Group> findByCreatedBy(User user);

    // ✅ Search groups by name
    List<Group> findByNameContainingIgnoreCase(String name);

    List<Group> findByType(GroupType type);

    // convenience
    List<Group> findByCreatedBy_Id(Long userId);

    Optional<Group> findByName(String name);

}
