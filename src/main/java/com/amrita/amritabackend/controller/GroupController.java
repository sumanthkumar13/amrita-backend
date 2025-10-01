package com.amrita.amritabackend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amrita.amritabackend.dto.ApiResponse;
import com.amrita.amritabackend.dto.GroupResponseDto;
import com.amrita.amritabackend.model.Group;
import com.amrita.amritabackend.model.GroupMember;
import com.amrita.amritabackend.model.GroupType;
import com.amrita.amritabackend.model.User;
import com.amrita.amritabackend.repository.GroupMemberRepository;
import com.amrita.amritabackend.repository.GroupRepository;
import com.amrita.amritabackend.repository.UserRepository;
import com.amrita.amritabackend.service.GroupService;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

        @Autowired
        private GroupRepository groupRepository;

        @Autowired
        private GroupMemberRepository groupMemberRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private GroupService groupService;

        // ✅ Create a new group
        @PostMapping
        public ResponseEntity<ApiResponse> createGroup(@RequestBody java.util.Map<String, Object> payload) {
                try {
                        String name = payload.get("name").toString();
                        String description = payload.containsKey("description")
                                        ? payload.get("description").toString()
                                        : null;
                        Long creatorId = Long.valueOf(payload.get("creatorId").toString());

                        User creator = userRepository.findById(creatorId)
                                        .orElseThrow(() -> new RuntimeException("Creator not found"));

                        Group group = Group.builder()
                                        .name(name)
                                        .description(description)
                                        .createdBy(creator)
                                        .build();

                        Group savedGroup = groupRepository.save(group);

                        // Add creator as ADMIN
                        GroupMember gm = GroupMember.builder()
                                        .group(savedGroup)
                                        .user(creator)
                                        .role("ADMIN")
                                        .build();
                        groupMemberRepository.save(gm);

                        GroupResponseDto dto = groupService.mapToDto(savedGroup);
                        return ResponseEntity.ok(new ApiResponse("success", "Group created", dto));

                } catch (Exception ex) {
                        return ResponseEntity.internalServerError()
                                        .body(new ApiResponse("error", "Failed to create group: " + ex.getMessage(),
                                                        null));
                }
        }

        // ✅ Compatibility endpoint: Get all groups visible to a user
        // (user-created + default groups they belong to)
        @GetMapping("/user/{userId}")
        public ResponseEntity<ApiResponse> getGroupsOfUser(@PathVariable Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // User-created groups (exclude default)
                List<Group> createdGroups = groupRepository.findByCreatedBy(user).stream()
                                .filter(g -> g.getType() != GroupType.DEFAULT)
                                .toList();

                // Default groups user belongs to
                List<Group> defaultGroups = groupService.getDefaultGroups(userId);

                // Merge
                List<GroupResponseDto> response = new java.util.ArrayList<>();
                response.addAll(createdGroups.stream().map(groupService::mapToDto).toList());
                response.addAll(defaultGroups.stream().map(groupService::mapToDto).toList());

                return ResponseEntity.ok(new ApiResponse("success", "Groups fetched", response));
        }

        // ✅ Get ONLY user-created groups (not default)
        @GetMapping("/user/{userId}/created")
        public ResponseEntity<ApiResponse> getUserCreatedGroups(@PathVariable Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Group> createdGroups = groupRepository.findByCreatedBy(user).stream()
                                .filter(g -> g.getType() != GroupType.DEFAULT)
                                .toList();

                List<GroupResponseDto> response = createdGroups.stream()
                                .map(groupService::mapToDto)
                                .toList();

                return ResponseEntity.ok(new ApiResponse("success", "User-created groups fetched", response));
        }

        // ✅ Get default groups a user belongs to
        @GetMapping("/default/user/{userId}")
        public ResponseEntity<ApiResponse> getDefaultGroups(@PathVariable Long userId) {
                try {
                        List<Group> defaultGroups = groupService.getDefaultGroups(userId);
                        List<GroupResponseDto> response = defaultGroups.stream()
                                        .map(groupService::mapToDto)
                                        .toList();

                        return ResponseEntity.ok(new ApiResponse("success", "Default groups fetched", response));
                } catch (Exception ex) {
                        return ResponseEntity.internalServerError()
                                        .body(new ApiResponse("error",
                                                        "Failed to fetch default groups: " + ex.getMessage(), null));
                }
        }

        // ✅ Get default groups by branch (ex: "CSE", "ECE")
        @GetMapping("/default/branch/{branch}")
        public ResponseEntity<ApiResponse> getDefaultGroupsByBranch(@PathVariable String branch) {
                List<Group> defaultGroups = groupRepository.findAll().stream()
                                .filter(g -> g.getType() == GroupType.DEFAULT)
                                .filter(g -> g.getName().toLowerCase().contains(branch.toLowerCase()))
                                .toList();

                List<GroupResponseDto> response = defaultGroups.stream()
                                .map(groupService::mapToDto)
                                .toList();

                return ResponseEntity.ok(new ApiResponse("success", "Default groups fetched by branch", response));
        }

        // ✅ Add members to a group
        @PostMapping("/{groupId}/add-members")
        public ResponseEntity<ApiResponse> addMembers(@PathVariable Long groupId, @RequestBody List<Long> userIds) {
                Group group = groupRepository.findById(groupId)
                                .orElseThrow(() -> new RuntimeException("Group not found"));

                for (Long uid : userIds) {
                        User user = userRepository.findById(uid)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        if (groupMemberRepository.findByGroupAndUser(group, user).isEmpty()) {
                                GroupMember gm = GroupMember.builder()
                                                .group(group)
                                                .user(user)
                                                .role("MEMBER")
                                                .build();
                                groupMemberRepository.save(gm);
                        }
                }

                GroupResponseDto dto = groupService.mapToDto(group);
                return ResponseEntity.ok(new ApiResponse("success", "Members added", dto));
        }

        // ✅ Remove member from group
        @DeleteMapping("/{groupId}/remove-member/{userId}")
        public ResponseEntity<ApiResponse> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
                Group group = groupRepository.findById(groupId)
                                .orElseThrow(() -> new RuntimeException("Group not found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                groupMemberRepository.findByGroupAndUser(group, user).ifPresent(groupMemberRepository::delete);

                GroupResponseDto dto = groupService.mapToDto(group);
                return ResponseEntity.ok(new ApiResponse("success", "Member removed", dto));
        }

        // ✅ Get group details (with members)
        @GetMapping("/{groupId}")
        public ResponseEntity<ApiResponse> getGroupDetails(@PathVariable Long groupId) {
                Group group = groupRepository.findById(groupId)
                                .orElseThrow(() -> new RuntimeException("Group not found"));

                GroupResponseDto dto = groupService.mapToDto(group);
                return ResponseEntity.ok(new ApiResponse("success", "Group details fetched", dto));
        }
}
