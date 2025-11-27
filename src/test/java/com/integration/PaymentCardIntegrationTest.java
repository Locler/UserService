package com.integration;

import com.dto.PaymentCardDto;
import com.dto.UserDto;
import com.repositories.PaymentCardRep;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentCardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentCardRep cardRepository;

    @Autowired
    private UserRep userRepository;

    private Long userId;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("jwt.secret", () -> "01234567890123456789012345678901ABCDEF");
    }

    @BeforeEach
    void setup() throws Exception {
        cardRepository.deleteAll();
        userRepository.deleteAll();

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

        userId = objectMapper.readValue(response, UserDto.class).getId();
    }

    @Test
    void shouldCreateCard() throws Exception {
        PaymentCardDto card = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();

        mockMvc.perform(post("/cards/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number", is("1111222233334444")));
    }

    @Test
    void shouldGetAllCardsByUserId() throws Exception {
        PaymentCardDto card1 = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();

        PaymentCardDto card2 = PaymentCardDto.builder()
                .number("5555666677778888")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();

        mockMvc.perform(post("/cards/user/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(card1)));

        mockMvc.perform(post("/cards/user/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(card2)));

        mockMvc.perform(get("/cards/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
