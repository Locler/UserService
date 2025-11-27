package com.services;
import com.accessChecker.AccessChecker;
import com.dto.UserDto;
import com.entities.User;
import com.mappers.UserMapper;
import com.repositories.UserRep;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRep userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AccessChecker accessChecker;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto dto;
    private Claims claims;

    private final String token = "mockToken";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        dto = UserDto.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .active(true)
                .build();

        claims = mock(Claims.class);

        when(jwtService.parse(token)).thenReturn(claims);
    }

    @Test
    void createUser_success() {
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(dto)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userService.createUser(dto, token);

        verify(accessChecker).checkAdminAccess(claims);
        verify(userRepository).save(any());
        assertEquals("John", result.getName());
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userService.getUserById(1L, token);

        verify(accessChecker).checkUserAccess(1L, claims);
        assertEquals("John", result.getName());
    }


    @Test
    void updateUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto updated = userService.updateUser(1L, dto, token);

        verify(accessChecker).checkUserAccess(1L, claims);
        assertEquals("John", updated.getName());
    }

    @Test
    void activateUser_success() {
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.activateUser(1L, token);

        verify(accessChecker).checkAdminAccess(claims);
        assertTrue(user.getActive());
    }

    @Test
    void deactivateUser_success() {
        user.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L, token);

        verify(accessChecker).checkAdminAccess(claims);
        assertFalse(user.getActive());
    }

    @Test
    void deleteUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L, token);

        verify(accessChecker).checkAdminAccess(claims);
        verify(userRepository).delete(user);
    }
}