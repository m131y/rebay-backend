package com.rebay.rebay_backend.user.security;

import com.rebay.rebay_backend.S3.service.S3Service;
import com.rebay.rebay_backend.user.entity.User;
import com.rebay.rebay_backend.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final S3Service s3Service;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture");

        log.info("{}", oAuth2User);

        // github
        if (email == null && oAuth2User.getAttribute("login") != null) {
            email = oAuth2User.getAttribute("login") + "@github.local";
            avatarUrl = oAuth2User.getAttribute("avatar_url");
        }

        byte[] imageBytes = s3Service.downloadImageFromUrl(avatarUrl);

        final String finalEmail = email;
        final String finalName = name != null ? name : "User";
        final String finalAvatarUrl = s3Service.uploadImageBytes(imageBytes, "ssh/profile");

        User user = userRepository.findByEmail(finalEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(generateUsername(finalEmail)); //
                    newUser.setEmail(finalEmail);
                    newUser.setFullName(finalName);
                    newUser.setProfileImageUrl(finalAvatarUrl);
                    newUser.setPassword("");
                    newUser.setEnabled(true);
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(newUser);
                });

        if (finalAvatarUrl != null && !finalAvatarUrl.equals(user.getProfileImageUrl())) {
            user.setProfileImageUrl(finalAvatarUrl);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    public String generateUsername(String email) {
        String baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String username = baseUsername;
        int count = 1;

        while(userRepository.existsByUsername(username)) {
            username = baseUsername + count;
            count ++;
        }
        return username;
    }
}
