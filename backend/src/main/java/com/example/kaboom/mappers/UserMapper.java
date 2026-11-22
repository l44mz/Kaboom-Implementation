
package com.example.kaboom.mappers;

import com.example.kaboom.dtos.UserDto;
import com.example.kaboom.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCoins(),
            user.getRole(),
            null
        );
    }
}
