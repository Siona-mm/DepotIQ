package com.depotiq.controllers;

import com.depotiq.config.SecurityConfig;
import com.depotiq.dtos.profile.ProfileResponse;
import com.depotiq.dtos.settings.SettingsResponse;
import com.depotiq.services.AccountService;
import com.depotiq.services.InventoryService;
import com.depotiq.services.StoreService;
import com.depotiq.services.HistoricalSalesImportService;
import com.depotiq.services.UserProfileService;
import com.depotiq.services.UserSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                AuthController.class,
                StoreController.class,
                InventoryController.class,
                HistoricalDataImportController.class,
                UserProfileController.class,
                UserSettingsController.class
        },
        properties = "depotiq.cors.allowed-origins=http://localhost:*"
)
@Import({SecurityConfig.class, AuthorizationControllerTest.TestUsers.class})
class AuthorizationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private StoreService storeService;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private HistoricalSalesImportService historicalSalesImportService;

    @MockBean
    private com.depotiq.services.CatalogCsvImportService catalogCsvImportService;

    @MockBean
    private com.depotiq.services.DepotCsvImportService depotCsvImportService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private UserSettingsService userSettingsService;

    @Test
    void returnsCurrentAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(basic("manager", "manager123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("manager"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_MANAGER"));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsViewersToReadInventoryButNotManageIt() throws Exception {
        mockMvc.perform(get("/api/inventory/depot").with(basic("viewer", "viewer123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/depot").with(basic("viewer", "viewer123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void limitsCatalogEndpointsToAdministrators() throws Exception {
        mockMvc.perform(get("/api/stores").with(basic("manager", "manager123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/stores").with(basic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void limitsImportHistoryToAdministrators() throws Exception {
        mockMvc.perform(get("/api/imports").with(basic("manager", "manager123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/imports").with(basic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void allowsAllAuthenticatedRolesToReadTheirProfileAndSettings() throws Exception {
        when(userProfileService.getProfile("viewer"))
                .thenReturn(new ProfileResponse("viewer", "Viewer", null, null, null));
        when(userSettingsService.get("viewer"))
                .thenReturn(new SettingsResponse(7, 3, 250, true, true, true, false));

        mockMvc.perform(get("/api/profile/me").with(basic("viewer", "viewer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viewer"));

        mockMvc.perform(get("/api/settings/me").with(basic("viewer", "viewer123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultHorizon").value(7));
    }

    @Test
    void depotUploadsRequireAdministratorAndUseMultipartFile() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "stock.csv", "text/csv", "data".getBytes());
        for (String path : java.util.List.of("depot-products", "depot-refills")) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/imports/" + path)
                    .file(file).with(basic("manager", "manager123"))).andExpect(status().isForbidden());
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/imports/" + path)
                    .file(file).with(basic("admin", "admin123"))).andExpect(status().isOk());
        }
    }

    private static RequestPostProcessor basic(
            String username,
            String password
    ) {
        return SecurityMockMvcRequestPostProcessors.httpBasic(username, password);
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestUsers {
        @Bean
        UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password(passwordEncoder.encode("admin123")).roles("ADMIN").build(),
                    User.withUsername("manager").password(passwordEncoder.encode("manager123")).roles("MANAGER").build(),
                    User.withUsername("viewer").password(passwordEncoder.encode("viewer123")).roles("VIEWER").build()
            );
        }
    }
}
