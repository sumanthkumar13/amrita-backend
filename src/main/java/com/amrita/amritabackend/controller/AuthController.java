package com.amrita.amritabackend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amrita.amritabackend.dto.ApiResponse;
import com.amrita.amritabackend.dto.LoginRequest;
import com.amrita.amritabackend.dto.OtpRequest;
import com.amrita.amritabackend.dto.OtpVerifyRequest;
import com.amrita.amritabackend.dto.ResetPasswordRequest;
import com.amrita.amritabackend.dto.SignupRequest;
import com.amrita.amritabackend.dto.UserResponse;
import com.amrita.amritabackend.model.Event;
import com.amrita.amritabackend.model.Group;
import com.amrita.amritabackend.model.GroupMember;
import com.amrita.amritabackend.model.GroupType;
import com.amrita.amritabackend.model.User;
import com.amrita.amritabackend.repository.EventRepository;
import com.amrita.amritabackend.repository.GroupMemberRepository;
import com.amrita.amritabackend.repository.GroupRepository;
import com.amrita.amritabackend.repository.UserRepository;
import com.amrita.amritabackend.service.OtpService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private GroupRepository groupRepository;

        @Autowired
        private GroupMemberRepository groupMemberRepository;

        @Autowired
        private OtpService otpService;

        @Autowired
        private Cloudinary cloudinary;

        @Autowired
        private PasswordEncoder passwordEncoder;

        // ✅ Signup Endpoint
        @PostMapping("/signup")
        public ResponseEntity<ApiResponse> signup(@RequestBody SignupRequest signupRequest) {
                String email = signupRequest.getEmail();
                // Ensure OTP verification
                if (email == null || !otpService.isVerified(email)) {
                        return ResponseEntity.badRequest()
                                        .body(new ApiResponse("error", "Email not verified via OTP", null));
                }

                if (userRepository.findByEmail(email).isPresent()) {
                        return ResponseEntity
                                        .badRequest()
                                        .body(new ApiResponse("error", "User already exists", null));
                }

                // 1. Create User (✅ added batchYear)
                User newUser = User.builder()
                                .email(signupRequest.getEmail())
                                .password(passwordEncoder.encode(signupRequest.getPassword())) // ✅ now hashed
                                .fullName(signupRequest.getFullName())
                                .branch(signupRequest.getBranch())
                                .rollNumber(signupRequest.getRollNumber())
                                .phoneNumber(signupRequest.getPhoneNumber())
                                .batchYear(signupRequest.getBatchYear())
                                .build();

                User savedUser = userRepository.save(newUser);

                // 2. Create/Find Default Group
                String groupName = signupRequest.getBranch() + "-" + signupRequest.getBatchYear();
                Group defaultGroup = groupRepository.findByName(groupName)
                                .orElseGet(() -> {
                                        Group g = Group.builder()
                                                        .name(groupName)
                                                        .description("Default group for " + signupRequest.getBranch()
                                                                        + " " + signupRequest.getBatchYear())
                                                        .createdBy(savedUser)
                                                        .type(GroupType.DEFAULT)
                                                        .build();
                                        return groupRepository.save(g);
                                });

                // 3. Add user as member
                GroupMember membership = GroupMember.builder()
                                .group(defaultGroup)
                                .user(savedUser)
                                .role("MEMBER")
                                .build();
                groupMemberRepository.save(membership);

                // 4. Build response data
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("user", UserResponse.fromEntity(savedUser));
                responseData.put("defaultGroups", List.of(Map.of(
                                "id", defaultGroup.getId(),
                                "name", defaultGroup.getName(),
                                "type", defaultGroup.getType().name())));

                // consume OTP verification so it cannot be reused
                otpService.consumeVerification(email);

                return ResponseEntity.ok(new ApiResponse("success", "Signup successful", responseData));
        }

        @PostMapping("/send-otp")
        public ResponseEntity<ApiResponse> sendOtp(@RequestBody OtpRequest request) {
                String email = request.getEmail();
                if (email == null || !email.toLowerCase().endsWith(".amrita.edu")) {
                        return ResponseEntity.badRequest()
                                        .body(new ApiResponse("error", "Only Amrita email IDs are allowed", null));
                }

                otpService.generateAndSendOtp(email);
                return ResponseEntity.ok(new ApiResponse("success", "OTP sent to provided email (check spam)", null));
        }

        @PostMapping("/verify-otp")
        public ResponseEntity<ApiResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
                String email = request.getEmail();
                String otp = request.getOtp();
                if (email == null || otp == null) {
                        return ResponseEntity.badRequest()
                                        .body(new ApiResponse("error", "Email and OTP required", null));
                }

                boolean ok = otpService.verifyOtp(email, otp);
                if (ok) {
                        return ResponseEntity.ok(new ApiResponse("success", "OTP verified", null));
                } else {
                        return ResponseEntity.badRequest()
                                        .body(new ApiResponse("error", "Invalid or expired OTP", null));
                }
        }

        @PostMapping("/send-reset-otp")
        public ResponseEntity<ApiResponse> sendResetOtp(@RequestBody OtpRequest req) {
                otpService.generateAndSendOtp(req.getEmail());
                return ResponseEntity.ok(new ApiResponse("success", "Reset OTP sent to email", null));
        }

        @PostMapping("/reset-password")
        public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest req) {
                if (!otpService.verifyOtp(req.getEmail(), req.getOtp())) {
                        return ResponseEntity.badRequest()
                                        .body(new ApiResponse("error", "Invalid or expired OTP", null));
                }

                return userRepository.findByEmail(req.getEmail())
                                .map(user -> {
                                        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
                                        userRepository.save(user);
                                        return ResponseEntity.ok(
                                                        new ApiResponse("success", "Password reset successful", null));
                                })
                                .orElse(ResponseEntity.status(404)
                                                .body(new ApiResponse("error", "User not found", null)));
        }

        // ✅ Login Endpoint using LoginRequest DTO
        @PostMapping("/login")
        public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest) {
                return userRepository.findByEmail(loginRequest.getEmail())
                                .map(dbUser -> {
                                        if (passwordEncoder.matches(loginRequest.getPassword(), dbUser.getPassword())) {
                                                // ✅ new users with hashed password
                                                Map<String, Object> responseData = new HashMap<>();
                                                responseData.put("user", UserResponse.fromEntity(dbUser));
                                                return ResponseEntity.ok(new ApiResponse("success", "Login successful",
                                                                responseData));
                                        }
                                        // 👇 Fallback: old users with plaintext password
                                        else if (dbUser.getPassword().equals(loginRequest.getPassword())) {
                                                // Migrate old password -> hash it now
                                                dbUser.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
                                                userRepository.save(dbUser);

                                                Map<String, Object> responseData = new HashMap<>();
                                                responseData.put("user", UserResponse.fromEntity(dbUser));
                                                return ResponseEntity.ok(new ApiResponse("success",
                                                                "Login successful (migrated)", responseData));
                                        } else {
                                                return ResponseEntity.status(401).body(
                                                                new ApiResponse("error", "Invalid password", null));
                                        }

                                })
                                .orElse(ResponseEntity.status(404).body(
                                                new ApiResponse("error", "User not found", null)));
        }

        // ✅ Get only user info (basic profile)
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse> getBasicUser(@PathVariable Long id) {
                return userRepository.findById(id)
                                .map(user -> ResponseEntity.ok(
                                                new ApiResponse("success", "User profile fetched",
                                                                UserResponse.fromEntity(user))))
                                .orElse(ResponseEntity.status(404).body(
                                                new ApiResponse("error", "User not found", null)));
        }

        // ✅ Get full profile + events created by user
        @GetMapping("/{id}/profile")
        public ResponseEntity<ApiResponse> getUserProfileWithEvents(@PathVariable Long id) {
                return userRepository.findById(id)
                                .map(user -> {
                                        List<Event> userEvents = eventRepository.findByOrganiserId(id);

                                        Map<String, Object> profileData = new HashMap<>();
                                        profileData.put("user", UserResponse.fromEntity(user));
                                        profileData.put("events", userEvents);

                                        return ResponseEntity
                                                        .ok(new ApiResponse("success", "Profile fetched", profileData));
                                })
                                .orElse(ResponseEntity.status(404).body(
                                                new ApiResponse("error", "User not found", null)));
        }

        @PostMapping("/{id}/upload-profile")
        public ResponseEntity<ApiResponse> uploadProfileImage(
                        @PathVariable Long id,
                        @RequestParam("profileImage") MultipartFile file) {
                try {
                        User user = userRepository.findById(id)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // ✅ Upload to Cloudinary (safe cast with suppression)
                        @SuppressWarnings("unchecked")
                        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                                        file.getBytes(),
                                        ObjectUtils.asMap("folder", "profile_pics"));

                        String imageUrl = (String) uploadResult.get("secure_url");

                        // ✅ Save to DB
                        user.setProfileImage(imageUrl);
                        userRepository.save(user);

                        return ResponseEntity.ok(
                                        new ApiResponse(
                                                        "success",
                                                        "Profile picture updated",
                                                        Map.of("profileImage", imageUrl)));

                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.internalServerError()
                                        .body(new ApiResponse(
                                                        "error",
                                                        "Failed to upload profile picture: " + e.getMessage(),
                                                        null));
                }
        }

        // ✅ Search users by name or email
        @GetMapping("/search")
        public ResponseEntity<ApiResponse> searchUsers(@RequestParam String query) {
                List<User> results = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query,
                                query);
                List<UserResponse> responseList = results.stream()
                                .map(UserResponse::fromEntity)
                                .toList();

                return ResponseEntity.ok(new ApiResponse("success", "Users fetched", responseList));
        }
}
