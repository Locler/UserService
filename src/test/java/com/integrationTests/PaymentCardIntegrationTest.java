package com.integrationTests;

import com.dto.PaymentCardDto;
import com.entities.User;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import com.services.PaymentCardService;
import com.accessChecker.AccessChecker;
import com.mappers.PaymentCardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {"spring.profiles.active=test"})
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
class PaymentCardIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PaymentCardService cardService;

    @Autowired
    private PaymentCardRep cardRepository;

    @Autowired
    private UserRep userRepository;

    @Autowired
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private AccessChecker accessChecker; // мок для тестов, пропускает проверки доступа

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private PaymentCardDto cardDto;
    private User user;

    @BeforeEach
    void setup() {

        cardRepository.deleteAll();
        userRepository.deleteAll();

        // пользователь
        user = User.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .birthDate(LocalDate.of(1995, 5, 10))
                .active(true)
                .build();
        userRepository.saveAndFlush(user); // flush гарантирует инициализацию

        //карта для тестов
        cardDto = PaymentCardDto.builder()
                .number("1234567890123456")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(user.getId())
                .build();

        cardService.clearAllCache();
    }

    @Test
    void createCardGetByUserId() {
        PaymentCardDto created = cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER"));
        assertNotNull(created.getId());

        List<PaymentCardDto> cards = cardService.getCardsByUserId(user.getId(), user.getId(), Set.of("ROLE_USER"));
        assertEquals(1, cards.size());
        assertEquals("John Doe", cards.getFirst().getHolder());
    }

    @Test
    void updateCard() {
        PaymentCardDto created = cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER"));
        created.setHolder("Jane Doe");
        PaymentCardDto updated = cardService.updateCard(created.getId(), created, user.getId(), Set.of("ROLE_USER"));
        assertEquals("Jane Doe", updated.getHolder());
    }

    @Test
    void activateDeactivateCard() {
        PaymentCardDto created = cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER"));

        // деактивация
        cardService.deactivateCard(created.getId(), Set.of("ROLE_ADMIN"));
        PaymentCardDto deactivated = cardService.getCardById(created.getId(), user.getId(), Set.of("ROLE_USER"));
        assertFalse(deactivated.getActive());

        // активация
        cardService.activateCard(created.getId(), Set.of("ROLE_ADMIN"));
        PaymentCardDto activated = cardService.getCardById(created.getId(), user.getId(), Set.of("ROLE_USER"));
        assertTrue(activated.getActive());
    }

    @Test
    void deleteCard() {
        PaymentCardDto created = cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER"));
        cardService.deleteCard(created.getId(), Set.of("ROLE_ADMIN"));

        assertThrows(IllegalArgumentException.class, () ->
                cardService.getCardById(created.getId(), user.getId(), Set.of("ROLE_USER")));
    }

    @Test
    void cannot_create_card_for_inactive_user() {
        user.setActive(false);
        userRepository.saveAndFlush(user);

        assertThrows(RuntimeException.class, () ->
                cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER")));
    }

    @Test
    void cannot_create_more_than_5_cards_per_user() {
        for (int i = 0; i < 5; i++) {
            cardDto.setNumber("12345678901234" + i); // уникальный номер для каждой карты
            cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER"));
        }

        cardDto.setNumber("1234567890123459");
        Exception ex = assertThrows(IllegalStateException.class, () ->
                cardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER")));
        assertEquals("User cannot have more than 5 cards", ex.getMessage());
    }
}