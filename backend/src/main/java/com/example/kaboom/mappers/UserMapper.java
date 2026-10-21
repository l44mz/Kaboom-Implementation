package com.example.kaboom.mappers;

import com.example.kaboom.dtos.UserDto;
import com.example.kaboom.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
