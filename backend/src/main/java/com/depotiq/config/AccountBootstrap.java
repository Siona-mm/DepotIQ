package com.depotiq.config;

import com.depotiq.models.AppUser;
import com.depotiq.repositories.AppUserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AccountBootstrap implements ApplicationRunner {
    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String managerPassword;
    private final String viewerPassword;

    public AccountBootstrap(
            AppUserRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${depotiq.security.admin-password:admin123}") String adminPassword,
            @Value("${depotiq.security.manager-password:manager123}") String managerPassword,
            @Value("${depotiq.security.viewer-password:viewer123}") String viewerPassword
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.managerPassword = managerPassword;
        this.viewerPassword = viewerPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }

        repository.saveAll(List.of(
                account("admin", adminPassword, "ADMIN"),
                account("manager", managerPassword, "MANAGER"),
                account("viewer", viewerPassword, "VIEWER")
        ));
    }

    private AppUser account(String username, String password, String role) {
        AppUser account = new AppUser();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);
        account.setEnabled(true);
        return account;
    }
}
