package com.collabdocs.auth.oauth2;

import com.collabdocs.auth.entity.User;
import com.collabdocs.auth.repository.UserRepository;
import com.collabdocs.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @InjectMocks OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");
    }

    private OAuth2AuthenticationToken googleToken(String email, String sub, String name) {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("sub")).thenReturn(sub);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(oAuth2User);
        when(token.getAuthorizedClientRegistrationId()).thenReturn("google");
        return token;
    }

    @Test
    void onAuthenticationSuccess_newGoogleUser_createsUserAndRedirects() throws Exception {
        OAuth2AuthenticationToken token = googleToken("alice@gmail.com", "google-sub-123", "Alice");
        User savedUser = new User("alice@gmail.com", null, "Alice");
        when(userRepository.findByOauth2ProviderAndOauth2ProviderId("google", "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(), anyString())).thenReturn("new-jwt");

        handler.onAuthenticationSuccess(request, response, token);

        verify(userRepository).save(any(User.class));
        verify(response).sendRedirect("http://localhost:3000/oauth2/callback?token=new-jwt");
    }

    @Test
    void onAuthenticationSuccess_returningGoogleUser_doesNotCreateNewUser() throws Exception {
        OAuth2AuthenticationToken token = googleToken("alice@gmail.com", "google-sub-123", "Alice");
        User existingUser = new User("alice@gmail.com", null, "Alice");
        when(userRepository.findByOauth2ProviderAndOauth2ProviderId("google", "google-sub-123"))
                .thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(any(), anyString())).thenReturn("existing-jwt");

        handler.onAuthenticationSuccess(request, response, token);

        verify(userRepository, never()).save(any());
        verify(response).sendRedirect("http://localhost:3000/oauth2/callback?token=existing-jwt");
    }

    @Test
    void onAuthenticationSuccess_existingEmailUser_linksOAuth2Account() throws Exception {
        OAuth2AuthenticationToken token = googleToken("alice@gmail.com", "google-sub-123", "Alice");
        User passwordUser = new User("alice@gmail.com", "hashed", "Alice");
        when(userRepository.findByOauth2ProviderAndOauth2ProviderId("google", "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@gmail.com")).thenReturn(Optional.of(passwordUser));
        when(userRepository.save(any(User.class))).thenReturn(passwordUser);
        when(jwtService.generateToken(any(), anyString())).thenReturn("linked-jwt");

        handler.onAuthenticationSuccess(request, response, token);

        verify(userRepository).save(passwordUser);
        verify(response).sendRedirect("http://localhost:3000/oauth2/callback?token=linked-jwt");
    }

    @Test
    void onAuthenticationSuccess_githubUser_extractsIdAndFallbackEmail() throws Exception {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("id")).thenReturn(Integer.valueOf(42));
        when(oAuth2User.getAttribute("email")).thenReturn(null);
        when(oAuth2User.getAttribute("login")).thenReturn("octocat");
        when(oAuth2User.getAttribute("name")).thenReturn("Octo Cat");
        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(oAuth2User);
        when(token.getAuthorizedClientRegistrationId()).thenReturn("github");

        User savedUser = new User("octocat@github.noemail", null, "Octo Cat");
        when(userRepository.findByOauth2ProviderAndOauth2ProviderId("github", "42"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("octocat@github.noemail")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(), anyString())).thenReturn("github-jwt");

        handler.onAuthenticationSuccess(request, response, token);

        verify(response).sendRedirect("http://localhost:3000/oauth2/callback?token=github-jwt");
    }
}
