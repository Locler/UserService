package com.unitTests;

import com.accessChecker.AccessChecker;
import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import com.services.PaymentCardService;
import com.exceptions.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRep paymentCardRep;

    @Mock
    private UserRep userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private AccessChecker accessChecker;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userCardsCache;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private User user;
    private PaymentCard card;
    private PaymentCardDto cardDto;

    @BeforeEach
    void setup() {

        user = new User();
        user.setId(1L);
        user.setActive(true);

        card = new PaymentCard();
        card.setId(1L);
        card.setUser(user);
        card.setActive(true);
        card.setNumber("1234567890123");
        card.setHolder("John Doe");
        card.setExpirationDate(LocalDate.now().plusYears(1));

        cardDto = PaymentCardDto.builder()
                .id(1L)
                .userId(user.getId())
                .number("1234567890123")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(1))
                .active(true)
                .build();
    }

    @Test
    void createCardSuccess() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(paymentCardRep.existsByNumber(cardDto.getNumber())).thenReturn(false);
        when(paymentCardMapper.toPaymentCardEntity(cardDto)).thenReturn(card);
        when(paymentCardRep.save(any())).thenReturn(card);
        when(paymentCardMapper.toPaymentDto(card)).thenReturn(cardDto);

        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());

        PaymentCardDto result = paymentCardService.createCard(
                user.getId(), cardDto, user.getId(), Set.of("ROLE_USER"));

        assertEquals(cardDto.getNumber(), result.getNumber());
        assertTrue(result.getActive());
    }

    @Test
    void createCardInactiveUser() {
        user.setActive(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());

        assertThrows(BadRequestException.class,
                () -> paymentCardService.createCard(user.getId(), cardDto, user.getId(), Set.of("ROLE_USER")));
    }

    @Test
    void getCardByIdSuccess() {
        when(paymentCardRep.findById(card.getId())).thenReturn(Optional.of(card));
        when(paymentCardMapper.toPaymentDto(card)).thenReturn(cardDto);
        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());

        PaymentCardDto result = paymentCardService.getCardById(card.getId(), user.getId(), Set.of("ROLE_USER"));

        assertEquals(cardDto.getId(), result.getId());
    }

    @Test
    void updateCardSuccess() {

        when(cacheManager.getCache("userCards")).thenReturn(userCardsCache);
        doNothing().when(userCardsCache).evict(any());

        PaymentCardDto updatedDto = PaymentCardDto.builder()
                .id(card.getId())
                .userId(user.getId())
                .number("9876543210987")
                .holder("John Smith")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();

        when(paymentCardRep.findById(card.getId())).thenReturn(Optional.of(card));
        when(paymentCardRep.save(any())).thenReturn(card);
        when(paymentCardMapper.toPaymentDto(card)).thenReturn(updatedDto);

        doNothing().when(accessChecker).checkUserAccess(anyLong(), anyLong(), any());

        // Evict кэш мокан через userCardsCache
        PaymentCardDto result = paymentCardService.updateCard(
                card.getId(), updatedDto, user.getId(), Set.of("ROLE_USER")
        );

        assertEquals(updatedDto.getNumber(), result.getNumber());
        assertEquals(updatedDto.getHolder(), result.getHolder());
    }

    @Test
    void deactivateCardSuccess() {
        when(paymentCardRep.findById(card.getId())).thenReturn(Optional.of(card));
        doNothing().when(accessChecker).checkAdminAccess(any());

        paymentCardService.deactivateCard(card.getId(), Set.of("ROLE_ADMIN"));

        assertFalse(card.getActive());
    }

    @Test
    void activateCardSuccess() {
        card.setActive(false);
        when(paymentCardRep.findById(card.getId())).thenReturn(Optional.of(card));
        doNothing().when(accessChecker).checkAdminAccess(any());
        when(paymentCardRep.save(any())).thenReturn(card);

        paymentCardService.activateCard(card.getId(), Set.of("ROLE_ADMIN"));

        assertTrue(card.getActive());
    }
}