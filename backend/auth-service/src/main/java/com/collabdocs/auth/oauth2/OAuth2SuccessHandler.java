package com.collabdocs.auth.oauth2;

import com.collabdocs.auth.entity.User;
import com.collabdocs.auth.repository.UserRepository;
import com.collabdocs.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId(); // "google" or "github"

        String providerId = extractProviderId(oAuth2User, provider);
        String email = extractEmail(oAuth2User, provider);
        String displayName = extractDisplayName(oAuth2User);

        User user = findOrCreateUser(provider, providerId, email, displayName);
        String jwt = jwtService.generateToken(user.getId(), user.getEmail());

        response.sendRedirect(frontendUrl + "/oauth2/callback?token=" + jwt);
    }

    private User findOrCreateUser(String provider, String providerId, String email, String displayName) {
        // Returning OAuth user
        return userRepository.findByOauth2ProviderAndOauth2ProviderId(provider, providerId)
                .orElseGet(() ->
                    // Email already exists (user registered with password before) — link accounts
                    userRepository.findByEmail(email)
                            .map(existing -> {
                                existing.setOauth2Provider(provider);
                                existing.setOauth2ProviderId(providerId);
                                return userRepository.save(existing);
                            })
                            // Brand new user via OAuth
                            .orElseGet(() -> {
                                User newUser = new User(email, null, displayName);
                                newUser.setOauth2Provider(provider);
                                newUser.setOauth2ProviderId(providerId);
                                return userRepository.save(newUser);
                            })
                );
    }

    private String extractProviderId(OAuth2User user, String provider) {
        if ("github".equals(provider)) {
            // GitHub returns id as an Integer
            Object id = user.getAttribute("id");
            return id != null ? String.valueOf(id) : null;
        }
        // Google uses "sub" as the unique identifier
        return user.getAttribute("sub");
    }

    private String extractEmail(OAuth2User user, String provider) {
        String email = user.getAttribute("email");
        if (email == null && "github".equals(provider)) {
            // GitHub allows users to keep email private
            String login = user.getAttribute("login");
            email = (login != null ? login : "user") + "@github.noemail";
        }
        return email;
    }

    private String extractDisplayName(OAuth2User user) {
        String name = user.getAttribute("name");
        if (name == null) {
            name = user.getAttribute("login"); // GitHub fallback
        }
        return name != null ? name : "User";
    }
}
