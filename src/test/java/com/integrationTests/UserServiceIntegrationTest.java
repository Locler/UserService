package com.integrationTests;

import com.dto.UserDto;
import com.repositories.UserRep;
import com.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {"spring.profiles.active=test"})
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRep userRepository;

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private UserDto dto;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        dto = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .birthDate(LocalDate.of(1995, 5, 10))
                .build();
    }

    @Test
    void createUserGetByEmail() {
        UserDto created = userService.createUser(dto, Set.of("ROLE_ADMIN"), false);
        assertNotNull(created.getId());

        UserDto found = userService.getUserByEmail("john@example.com", Set.of("ROLE_ADMIN"));
        assertEquals("John", found.getName());
        assertEquals("Doe", found.getSurname());
    }

    @Test
    void updateUser() {
        UserDto created = userService.createUser(dto, Set.of("ROLE_ADMIN"), false);

        created.setName("Jane");
        UserDto updated = userService.updateUser(created.getId(), created, created.getId(), Set.of("ROLE_ADMIN"));

        assertEquals("Jane", updated.getName());
    }



    @Test
    void updateNonExistentUser() {
        dto.setEmail("nonexistent@example.com");
        assertThrows(RuntimeException.class, () ->
                userService.updateUser(999L, dto, 999L, Set.of("ROLE_ADMIN")));
    }

    @Test
    void getNonExistentUser() {
        assertThrows(RuntimeException.class, () ->
                userService.getUserByEmail("noone@example.com", Set.of("ROLE_ADMIN")));
    }
}