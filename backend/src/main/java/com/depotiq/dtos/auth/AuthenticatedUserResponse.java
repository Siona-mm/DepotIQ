package com.depotiq.dtos.auth;

import java.util.List;

public record AuthenticatedUserResponse(String username, List<String> roles) {
}
