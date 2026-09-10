package com.shopstack.mapper;

import com.shopstack.dto.UserResponse;
import com.shopstack.entity.User;
import org.springframework.stereotype.Component;

/**
 * UserMapper — Maps User entities to UserResponse DTOs.
 */
@Component
public class UserMapper {

    /**
     * Converts a {@link User} entity to a safe {@link UserResponse} DTO.
     *
     * @param user the persistent user entity
     * @return {@link UserResponse} containing safe, client-facing fields
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
