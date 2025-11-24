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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

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
    void shouldGetCardById() throws Exception {
        PaymentCardDto card = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();
        String response = mockMvc.perform(post("/cards/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andReturn().getResponse().getContentAsString();
        PaymentCardDto created = objectMapper.readValue(response, PaymentCardDto.class);

        mockMvc.perform(get("/cards/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is("1111222233334444")));
    }

    @Test
    void shouldGetAllCardsByUserId() throws Exception {
        PaymentCardDto card = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();
        mockMvc.perform(post("/cards/user/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(card)));
        PaymentCardDto cardSecond = PaymentCardDto.builder()
                .number("1111222123123123")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();
        mockMvc.perform(post("/cards/user/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cardSecond)));
        mockMvc.perform(get("/cards/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldUpdateCard() throws Exception {
        PaymentCardDto card = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();
        String response = mockMvc.perform(post("/cards/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andReturn().getResponse().getContentAsString();
        PaymentCardDto created = objectMapper.readValue(response, PaymentCardDto.class);

        created.setNumber("7777888899990000");
        mockMvc.perform(put("/cards/" + created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andDo(print()) // печатаю ответ для отладки
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is("7777888899990000")));
    }

    @Test
    void shouldDeactivateCard() throws Exception {
        PaymentCardDto card = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();
        String response = mockMvc.perform(post("/cards/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andReturn().getResponse().getContentAsString();
        PaymentCardDto created = objectMapper.readValue(response, PaymentCardDto.class);

        mockMvc.perform(put("/cards/" + created.getId() + "/deactivate")
                        .param("active", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteCard() throws Exception {
        PaymentCardDto card = PaymentCardDto.builder()
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(userId)
                .build();
        String response = mockMvc.perform(post("/cards/user/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andReturn().getResponse().getContentAsString();
        PaymentCardDto created = objectMapper.readValue(response, PaymentCardDto.class);

        mockMvc.perform(delete("/cards/" + created.getId()))
                .andExpect(status().isNoContent());
    }
}
