package com.amrita.amritabackend.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amrita.amritabackend.dto.GroupResponseDto;
import com.amrita.amritabackend.model.Group;
import com.amrita.amritabackend.model.GroupMember;
import com.amrita.amritabackend.model.GroupType;
import com.amrita.amritabackend.model.User;
import com.amrita.amritabackend.repository.GroupMemberRepository;
import com.amrita.amritabackend.repository.GroupRepository;
import com.amrita.amritabackend.repository.UserRepository;

@Service
public class GroupService {

        @Autowired
        private GroupRepository groupRepository;

        @Autowired
        private GroupMemberRepository groupMemberRepository;

        @Autowired
        private UserRepository userRepository;

        /**
         * Expand group IDs into unique user IDs (deduplicated).
         */
        @Transactional(readOnly = true)
        public Set<Long> expandGroupsToUserIds(List<Long> groupIds) {
                return groupIds.stream()
                                .flatMap(groupId -> groupMemberRepository.findByGroup(
                                                groupRepository.findById(groupId)
                                                                .orElseThrow(() -> new RuntimeException(
                                                                                "Group not found: " + groupId)))
                                                .stream())
                                .map(GroupMember::getUser)
                                .map(User::getId)
                                .collect(Collectors.toSet()); // deduplicates
        }

        /**
         * Get groups created by a specific user.
         */
        @Transactional(readOnly = true)
        public List<Group> getMyGroups(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                return groupRepository.findByCreatedBy(user);
        }

        /**
         * Get default groups a user belongs to.
         */
        @Transactional(readOnly = true)
        public List<Group> getDefaultGroups(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<GroupMember> memberships = groupMemberRepository.findByUser(user);
                return memberships.stream()
                                .map(GroupMember::getGroup)
                                .filter(g -> g.getType() == GroupType.DEFAULT)
                                .toList();
        }

        /**
         * Convert Group + Members → GroupResponseDto
         */
        @Transactional(readOnly = true)
        public GroupResponseDto mapToDto(Group group, List<GroupMember> members) {
                return GroupResponseDto.builder()
                                .id(group.getId())
                                .name(group.getName())
                                .description(group.getDescription())
                                .type(group.getType().name())
                                .createdAt(group.getCreatedAt())
                                .createdBy(Map.of(
                                                "id", group.getCreatedBy().getId(),
                                                "fullName", group.getCreatedBy().getFullName(),
                                                "email", group.getCreatedBy().getEmail()))
                                .members(members.stream()
                                                .map(gm -> Map.<String, Object>of(
                                                                "id", gm.getUser().getId(),
                                                                "fullName", gm.getUser().getFullName(),
                                                                "email", gm.getUser().getEmail(),
                                                                "role", gm.getRole()))
                                                .toList())
                                .build();
        }

        // ✅ Overload: fetch members internally
        @Transactional(readOnly = true)
        public GroupResponseDto mapToDto(Group group) {
                List<GroupMember> members = groupMemberRepository.findByGroup(group);
                return mapToDto(group, members);
        }
}
