package com.services;

import com.dto.UserDto;
import com.entities.User;
import com.mappers.UserMapper;
import com.repositories.UserRep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRep userRepository;
    @Mock
    private UserMapper userMapper;

    //создаю реальный объект, но подставляю моки в зависимые классы
    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto dto;
    private User userSecond;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .paymentCards(new ArrayList<>())
                .build();
        dto = UserDto.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john@mail.com")
                .active(true)
                .build();
        userSecond = User.builder()
                .id(2L)
                .name("Jane")
                .surname("Smith")
                .active(true)
                .birthDate(LocalDate.of(1992, 2, 2))
                .email("jane@mail.com")
                .paymentCards(new ArrayList<>())
                .build();

    }

    @Test
    void createUser() {
        when(userMapper.toEntity(dto)).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userService.createUser(dto);

        assertEquals(dto.getName(), result.getName());
        verify(userRepository).save(any());
    }

    @Test
    void getUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userService.getUserById(1L);

        assertEquals("John", result.getName());
    }

    @Test
    void getAllUsers() {
        List<User> users = List.of(user, userSecond);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(dto);


        Page<UserDto> result = userService.getAllUsers("John", "Doe", pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("John", result.getContent().getFirst().getName());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDto(any())).thenReturn(dto);

        UserDto updated = userService.updateUser(1L, dto);
        assertEquals("John", updated.getName());
    }

    @Test
    void activateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        userService.deactivateUser(1L);
        userService.activateUser(1L);

        assertTrue(user.getActive());
    }

    @Test
    void deactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertFalse(user.getActive());
    }

    @Test
    void deleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(99L));
    }

}