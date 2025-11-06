package com.services;

import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentCardService {

    private final PaymentCardMapper paymentCardMapper;

    private final PaymentCardRep paymentCardRep;

    private final UserRep userRepository;

    @Autowired
    public PaymentCardService(PaymentCardMapper paymentCardMapper, PaymentCardRep paymentCardRep, UserRep userRepository) {
        this.paymentCardMapper = paymentCardMapper;
        this.paymentCardRep = paymentCardRep;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentCardDto createCard(Long userId, PaymentCardDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getActive()) {
            throw new IllegalStateException("Cannot create card for inactive user");
        }

        // Проверка лимита карт
        if (user.getPaymentCards() != null && user.getPaymentCards().size() >= 5) {
            throw new IllegalStateException("User cannot have more than 5 cards");
        }

        if (dto.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future");
        }

        if (paymentCardRep.existsByNumber(dto.getNumber())) {
            throw new IllegalArgumentException("Card number already exists");
        }

        PaymentCard card = paymentCardMapper.toPaymentCardEntity(dto);
        card.setUser(user);
        card.setActive(true);
        return paymentCardMapper.toPaymentDto(paymentCardRep.save(card));
    }

    public PaymentCardDto getCardById(Long id) {
        return paymentCardRep.findById(id)
                .map(paymentCardMapper::toPaymentDto)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    public List<PaymentCardDto> getCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return paymentCardMapper.toDtoPaymentList(paymentCardRep.findByUserId(userId));
    }

    @Transactional
    public PaymentCardDto updateCard(Long id, PaymentCardDto dto) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (!card.getActive()) {
            throw new IllegalStateException("Cannot update inactive card");
        }

        if (dto.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future");
        }

        card.setHolder(dto.getHolder());
        card.setExpirationDate(dto.getExpirationDate());
        card.setActive(dto.getActive());

        return paymentCardMapper.toPaymentDto(paymentCardRep.save(card));
    }

    @Transactional
    public void activateCard(Long id, boolean active) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (card.getActive() == active) {
            throw new IllegalStateException("Card already in this state");
        }
        paymentCardRep.updateCardStatus(id, active);
    }

    @Transactional
    public void deactivateCard(Long id) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + id));

        if (!card.getActive()) {
            throw new IllegalStateException("Card already inactive");
        }

        card.setActive(false);
        paymentCardRep.save(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + id));
        if (!card.getActive()) {
            throw new IllegalStateException("Card already inactive");
        }
        card.setActive(false);
        paymentCardRep.delete(card);
    }


}
