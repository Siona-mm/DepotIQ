package com.depotiq.config;

import com.depotiq.models.AppUser;
import com.depotiq.repositories.AppUserRepository;
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
        ensureAccount("admin", adminPassword, "ADMIN");
        ensureAccount("manager", managerPassword, "MANAGER");
        ensureAccount("viewer", viewerPassword, "VIEWER");
    }

    private void ensureAccount(String username, String password, String role) {
        if (!repository.existsByUsernameIgnoreCase(username)) {
            repository.save(account(username, password, role));
        }
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
