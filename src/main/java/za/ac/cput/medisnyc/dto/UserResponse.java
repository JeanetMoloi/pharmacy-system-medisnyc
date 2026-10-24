package za.ac.cput.medisnyc.dto;

import za.ac.cput.medisnyc.domain.User;

import java.util.List;

public record UserResponse(Long id, String username, String email, String firstName, String lastName,
                           List<String> roles, boolean enabled, String linkedProfileId) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(role -> role.getName()).sorted().toList(),
                user.isEnabled(),
                user.getLinkedProfileId());
    }
}
