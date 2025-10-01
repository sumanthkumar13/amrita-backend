package com.amrita.amritabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amrita.amritabackend.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // ✅ New: Search by fullName (case insensitive, contains)
    List<User> findByFullNameContainingIgnoreCase(String fullName);

    // (Optional) Search by rollNumber or branch too
    List<User> findByRollNumberContainingIgnoreCase(String rollNumber);

    List<User> findByBranchContainingIgnoreCase(String branch);

    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);

}
