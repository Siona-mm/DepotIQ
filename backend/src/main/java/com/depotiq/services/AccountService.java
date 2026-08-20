package com.depotiq.services;

import com.depotiq.dtos.auth.AuthenticatedUserResponse;
import com.depotiq.dtos.auth.UpdateCredentialsRequest;
import com.depotiq.models.AppUser;
import com.depotiq.repositories.AppUserRepository;
import com.depotiq.repositories.UserProfileRepository;
import com.depotiq.repositories.UserSettingsRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountService {
    private final AppUserRepository accountRepository;
    private final UserProfileRepository profileRepository;
    private final UserSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            AppUserRepository accountRepository,
            UserProfileRepository profileRepository,
            UserSettingsRepository settingsRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticatedUserResponse updateCredentials(
            String currentUsername,
            UpdateCredentialsRequest request
    ) {
        AppUser account = accountRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        String nextUsername = request.username().trim().toLowerCase(Locale.ROOT);
        boolean usernameChanged = !account.getUsername().equalsIgnoreCase(nextUsername);
        if (usernameChanged && accountRepository.existsByUsernameIgnoreCase(nextUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }

        String oldUsername = account.getUsername();
        if (usernameChanged) {
            account.setUsername(nextUsername);
            profileRepository.findByUsername(oldUsername).ifPresent(profile -> {
                profile.setUsername(nextUsername);
                profileRepository.save(profile);
            });
            settingsRepository.findByUsername(oldUsername).ifPresent(settings -> {
                settings.setUsername(nextUsername);
                settingsRepository.save(settings);
            });
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        accountRepository.save(account);
        return new AuthenticatedUserResponse(nextUsername, List.of("ROLE_" + account.getRole()));
    }
}
