package com.depotiq.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.depotiq.dtos.auth.AuthenticatedUserResponse;
import com.depotiq.dtos.auth.UpdateCredentialsRequest;
import com.depotiq.models.AppUser;
import com.depotiq.models.UserProfile;
import com.depotiq.models.UserSettings;
import com.depotiq.repositories.AppUserRepository;
import com.depotiq.repositories.UserProfileRepository;
import com.depotiq.repositories.UserSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AppUserRepository accountRepository;
    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private UserSettingsRepository settingsRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AccountService service;

    @Test
    void updatesUsernamePasswordAndOwnedRecordsTogether() {
        AppUser account = account("admin", "old-hash", "ADMIN");
        UserProfile profile = new UserProfile();
        profile.setUsername("admin");
        profile.setDisplayName("Admin");
        UserSettings settings = new UserSettings();
        settings.setUsername("admin");

        when(accountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("admin123", "old-hash")).thenReturn(true);
        when(accountRepository.existsByUsernameIgnoreCase("operations.admin")).thenReturn(false);
        when(profileRepository.findByUsername("admin")).thenReturn(Optional.of(profile));
        when(settingsRepository.findByUsername("admin")).thenReturn(Optional.of(settings));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        AuthenticatedUserResponse response = service.updateCredentials(
                "admin",
                new UpdateCredentialsRequest("Operations.Admin", "admin123", "new-password")
        );

        assertEquals("operations.admin", response.username());
        assertEquals("operations.admin", account.getUsername());
        assertEquals("new-hash", account.getPasswordHash());
        assertEquals("operations.admin", profile.getUsername());
        assertEquals("operations.admin", settings.getUsername());
        verify(accountRepository).save(account);
        verify(profileRepository).save(profile);
        verify(settingsRepository).save(settings);
    }

    @Test
    void rejectsAnIncorrectCurrentPassword() {
        AppUser account = account("admin", "old-hash", "ADMIN");
        when(accountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateCredentials(
                        "admin",
                        new UpdateCredentialsRequest("admin", "wrong-password", "")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private AppUser account(String username, String passwordHash, String role) {
        AppUser account = new AppUser();
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setRole(role);
        account.setEnabled(true);
        return account;
    }
}
