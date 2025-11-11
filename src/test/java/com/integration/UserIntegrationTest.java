package com.integration;

import com.dto.UserDto;
import com.repositories.UserRep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;


import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;


@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRep userRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUser() throws Exception {
        UserDto user = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        String json = objectMapper.writeValueAsString(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("john@mail.com")));
    }

    @Test
    void shouldGetAllUsers() throws Exception {
        UserDto user = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)));
        UserDto userSecond = UserDto.builder()
                .name("Spartak")
                .surname("Doe")
                .email("spartak@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1991, 1, 1))
                .build();
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSecond)));
        mockMvc.perform(get("/users")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void shouldGetUserById() throws Exception {
        UserDto user = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        String json = objectMapper.writeValueAsString(user);

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getContentAsString();

        UserDto created = objectMapper.readValue(response, UserDto.class);

        mockMvc.perform(get("/users/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("John")));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UserDto user = UserDto.builder()
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();
        UserDto created = objectMapper.readValue(response, UserDto.class);

        created.setSurname("Updated");
        mockMvc.perform(put("/users/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname", is("Updated")));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        UserDto user = UserDto.builder()
                .id(null)
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();
        UserDto created = objectMapper.readValue(response, UserDto.class);

        mockMvc.perform(delete("/users/" + created.getId()))
                .andExpect(status().isNoContent());
    }

}
