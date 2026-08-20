package com.depotiq.controllers;

import com.depotiq.dtos.auth.AuthenticatedUserResponse;
import com.depotiq.dtos.auth.UpdateCredentialsRequest;
import com.depotiq.services.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        return new AuthenticatedUserResponse(authentication.getName(), roles);
    }

    @PutMapping("/credentials")
    public AuthenticatedUserResponse updateCredentials(
            Authentication authentication,
            @Valid @RequestBody UpdateCredentialsRequest request
    ) {
        return accountService.updateCredentials(authentication.getName(), request);
    }
}
