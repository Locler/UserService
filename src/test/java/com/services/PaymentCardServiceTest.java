package com.services;

import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRep cardRepository;
    @Mock
    private UserRep userRepository;
    @Mock
    private PaymentCardMapper cardMapper;

    @InjectMocks
    private PaymentCardService cardService;

    private User user;
    private PaymentCard card;
    private PaymentCardDto dto;

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

        PaymentCardDto result = cardService.createCard(1L, dto);

        assertEquals("1111222233334444", result.getNumber());
        verify(cardRepository).save(any());
    }

    @Test
    void getAllCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> cards = new PageImpl<>(List.of(card));

        when(cardRepository.findAll(eq(pageable))).thenReturn(cards);
        when(cardMapper.toPaymentDto(card)).thenReturn(dto);

        Page<PaymentCardDto> result = cardService.getAllCards(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("John Doe", result.getContent().getFirst().getHolder());
        verify(cardRepository).findAll(pageable);
    }

    @Test
    void getCardById() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardMapper.toPaymentDto(card)).thenReturn(dto);

        PaymentCardDto result = cardService.getCardById(10L);
        assertEquals(10L, result.getId());
    }

    @Test
    void getCardsByUserId() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(cardRepository.findByUserId(1L)).thenReturn(List.of(card));
        when(cardMapper.toDtoPaymentList(List.of(card))).thenReturn(List.of(dto));

        List<PaymentCardDto> result = cardService.getCardsByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("1111222233334444", result.getFirst().getNumber());
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

        PaymentCardDto result = cardService.updateCard(10L, updatedDto);

        assertEquals("John Updated", result.getHolder());
        verify(cardRepository).save(any(PaymentCard.class));
    }

    @Test
    void activateCard() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        cardService.deactivateCard(10L);
        cardService.activateCard(10L);

        assertTrue(card.getActive());
    }

    @Test
    void deactivateCard() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        cardService.deactivateCard(10L);

        assertFalse(card.getActive());
    }

    @Test
    void deleteCard() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        doNothing().when(cardRepository).delete(card);

        cardService.deleteCard(10L);

        verify(cardRepository).delete(card);
    }

}