package com.rebay.rebay_backend.user.service;

import com.rebay.rebay_backend.social.repository.FollowRepository;
import com.rebay.rebay_backend.user.dto.*;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.exception.AuthenticationException;
import com.rebay.rebay_backend.user.exception.InvalidPasswordException;
import com.rebay.rebay_backend.user.exception.ResourceNotFoundException;
import com.rebay.rebay_backend.user.exception.UserAlreadyExistsException;
import com.rebay.rebay_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    public UserDto updateProfile(UserUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // username, email, 중복확인
        if (userRepository.existsByUsername(request.getUsername())) {
            if (!currentUser.getUsername().equals(request.getUsername())) {
                throw new UserAlreadyExistsException("동일한 username이 존재합니다.");
                //throw new UserAlreadyExistsException("Username already exists");
            }
        }
        currentUser.setUsername(request.getUsername());

        if (userRepository.existsByEmail(request.getEmail())) {
            if (!currentUser.getEmail().equals(request.getEmail())) {
                throw new UserAlreadyExistsException("동일한 이메일이 존재합니다.");
                //throw new UserAlreadyExistsException("Email already exists");
            }
        }
        currentUser.setEmail(request.getEmail());

        currentUser.setFullName(request.getFullName());
        currentUser.setBio(request.getBio());
        currentUser.setProfileImageUrl(request.getProfileImageUrl());
        currentUser.setUpdatedAt(LocalDateTime.now());
        currentUser.setEnabled(request.isEnabled());

        return UserDto.fromEntity(userRepository.save(currentUser));
    }

    public UserResponse getUserProfile(Long userId) {
        User currentUser = authenticationService.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. : " + userId));
//                .orElseThrow(() -> new ResourceNotFoundException("User not found for userId: " + userId));

        return mapToUserResponse(targetUser);
    }

    @Transactional
    public boolean updatePassword(PasswordUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // request 의 new password 가 현재비밀번호와 동일할 때 예외
        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new InvalidPasswordException("바꿀 비밀번호가 현재 비밀번호와 동일합니다.");
//            throw new InvalidPasswordException("The new password must be different from the current password.");

        }

        // request 의 old password 가 현재 패스워드와 동일하지 않으면 예외
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new InvalidPasswordException("현재 비밀번호가 동일하지 않습니다.");
//            throw new InvalidPasswordException("Current password does not match.");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

        User savedUser = userRepository.save(currentUser);
        return true;
    }

    public String findPassword(FindPasswordRequest request) {
        String loginId = request.getEmail() != null ? request.getEmail() : request.getUsername();

        User user = userRepository.findByEmail(loginId)
                .or(() -> userRepository.findByUsername(loginId))
                .orElseThrow(() -> new AuthenticationException("로그인 ID가 유효하지 않습니다."));
        //new AuthenticationException("Authentication failed"));

        return user.getFullName();
    }

    public boolean resetPassword(LoginRequest request) {
        String loginId = request.getEmail() != null ? request.getEmail() : request.getUsername();

        User currentUser = userRepository.findByEmail(loginId)
                .or(() -> userRepository.findByUsername(loginId))
                .orElseThrow(() -> new AuthenticationException("로그인 ID가 유효하지 않습니다."));
        //new AuthenticationException("Authentication failed"));

        currentUser.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(currentUser);
        return true;
    }

    public UserResponse mapToUserResponse(User targetUser) {
        User currentUser = authenticationService.getCurrentUser();

        Long followersCount = followRepository.countFollowers(targetUser);
        Long followingCount = followRepository.countFollowing(targetUser);
        boolean isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, targetUser);

        return UserResponse.builder()
                .id(targetUser.getId())
                .username(targetUser.getUsername())
                .email(targetUser.getEmail())
                .fullName(targetUser.getFullName())
                .profileImageUrl(targetUser.getProfileImageUrl())
                .bio(targetUser.getBio())
                .followersCount(followersCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .build();
    }
}
