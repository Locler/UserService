package com.unitTests;

import com.accessChecker.AccessChecker;
import com.dto.UserDto;
import com.entities.User;
import com.mappers.UserMapper;
import com.repositories.UserRep;
import com.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRep userRepository;

    @Mock
    private AccessChecker accessChecker;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UserDto dto;
    private User user;

    @BeforeEach
    void setup() {
        dto = UserDto.builder()
                .email("test@mail.com")
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(2000,1,1))
                .build();

        user = new User();
        user.setId(1L);
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setBirthDate(dto.getBirthDate());
        user.setActive(true);
    }

    @Test
    void createUser() {

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(dto.getEmail());
        savedUser.setActive(true);

        UserDto resultDto = UserDto.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .surname(dto.getSurname())
                .birthDate(dto.getBirthDate())
                .active(true)
                .build();

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(savedUser);

        when(userMapper.toEntity(dto)).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(resultDto);

        doNothing().when(accessChecker).checkAdminAccess(any(), anyBoolean());

        UserDto result = userService.createUser(dto, Set.of("ROLE_ADMIN"), false);

        assertEquals(dto.getEmail(), result.getEmail());
        assertTrue(result.getActive());
    }

    @Test
    void createUserDuplicateEmail() {
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class,
                () -> userService.createUser(dto, Set.of("ROLE_ADMIN"), false));
    }

    @Test
    void getUserByIdNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.getUserById(1L, 1L, Set.of("ROLE_USER")));
    }

    @Test
    void getUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        doNothing().when(accessChecker).checkUserAccess(any(), any(), any());

        UserDto result = userService.getUserById(1L, 1L, Set.of("ROLE_USER"));

        assertEquals(dto.getEmail(), result.getEmail());
    }

    @Test
    void updateUser() {

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(user)).thenReturn(dto);

        doNothing().when(accessChecker).checkUserAccess(any(), any(), any());

        UserDto result = userService.updateUser(1L, dto, 1L, Set.of("ROLE_USER"));

        assertEquals(dto.getName(), result.getName());
    }
}
