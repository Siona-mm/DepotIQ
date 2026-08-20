package com.depotiq.repositories;

import com.depotiq.models.UserSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUsername(String username);
}
