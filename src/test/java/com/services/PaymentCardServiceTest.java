package com.services;

import com.accessChecker.AccessChecker;
import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRep cardRepository;

    @Mock
    private UserRep userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userCardsCache;

    @Mock
    private JwtService jwtService;

    @Mock
    private AccessChecker accessChecker;

    @InjectMocks
    private PaymentCardService cardService;

    private User user;
    private PaymentCard card;
    private PaymentCardDto dto;
    private Claims claims;
    private final String token = "mock.token";

    @BeforeEach
    void setup() {
        claims = mock(Claims.class);

        // JWT
        when(jwtService.parse(anyString())).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1"); // userId из токена

        // AccessChecker
        doNothing().when(accessChecker).checkUserAccess(anyLong(), eq(claims));
        doNothing().when(accessChecker).checkAdminAccess(eq(claims));

        // Cache
        when(cacheManager.getCache("userCards")).thenReturn(userCardsCache);

        user = User.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .email("john@mail.com")
                .active(true)
                .birthDate(LocalDate.of(1990, 1, 1))
                .paymentCards(new ArrayList<>())
                .build();

        card = PaymentCard.builder()
                .id(10L)
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .user(user)
                .build();

        dto = PaymentCardDto.builder()
                .id(10L)
                .number("1111222233334444")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .userId(1L)
                .build();
    }

    @Test
    void createCard() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardMapper.toPaymentCardEntity(dto)).thenReturn(card);
        when(cardRepository.save(any())).thenReturn(card);
        when(cardMapper.toPaymentDto(card)).thenReturn(dto);

        PaymentCardDto result = cardService.createCard(1L, dto, token);

        assertEquals("1111222233334444", result.getNumber());
        verify(cardRepository).save(any());
    }

    @Test
    void getAllCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> cards = new PageImpl<>(List.of(card));

        when(cardRepository.findAll(eq(pageable))).thenReturn(cards);
        when(cardMapper.toPaymentDto(card)).thenReturn(dto);

        Page<PaymentCardDto> result = cardService.getAllCards(pageable, token);

        assertEquals(1, result.getTotalElements());
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void getCardById() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardMapper.toPaymentDto(card)).thenReturn(dto);

        PaymentCardDto result = cardService.getCardById(10L, token);

        assertEquals(10L, result.getId());
    }

    @Test
    void getCardsByUserId() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(cardRepository.findByUserId(1L)).thenReturn(List.of(card));
        when(cardMapper.toDtoPaymentList(List.of(card))).thenReturn(List.of(dto));

        List<PaymentCardDto> result = cardService.getCardsByUserId(1L, token);

        assertEquals(1, result.size());
        verify(cardRepository).findByUserId(1L);
    }

    @Test
    void updateCard() {
        PaymentCard updatedEntity = card.toBuilder()
                .holder("John Updated")
                .build();

        PaymentCardDto updatedDto = dto.toBuilder()
                .holder("John Updated")
                .build();

        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(updatedEntity);
        when(cardMapper.toPaymentDto(updatedEntity)).thenReturn(updatedDto);

        PaymentCardDto result = cardService.updateCard(10L, updatedDto, token);

        assertEquals("John Updated", result.getHolder());
        verify(cardRepository).save(any());
        verify(userCardsCache).evict(card.getUser().getId());
    }

    @Test
    void deactivateCard() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        cardService.deactivateCard(10L, token);

        assertFalse(card.getActive());
    }

    @Test
    void deleteCard() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        doNothing().when(cardRepository).delete(card);

        cardService.deleteCard(10L, token);

        verify(cardRepository).delete(card);
        verify(userCardsCache).evict(card.getUser().getId());
    }
}
