package com.mappers;

import com.dto.UserDto;
import com.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PaymentCardMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDto toDto(User entity);

    User toEntity(UserDto dto);

    List<UserDto> toDtoUserList(List<User> entities);

}

