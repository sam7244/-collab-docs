package com.collabdocs.auth.repository;

import com.collabdocs.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByOauth2ProviderAndOauth2ProviderId(String oauth2Provider, String oauth2ProviderId);
}
