package com.services;

import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.exceptions.BadRequestException;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @CachePut(value = "cards", key = "#result.id") // добавляю новый элемент
    @CacheEvict(value = "userCards", key = "#userId") // сношу список всех карт пользователя для актуальности данных
    @Transactional()
    public PaymentCardDto createCard(Long userId, PaymentCardDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getActive()) {
            throw new BadRequestException("Can not create a payment card with unactive user");
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

    @Cacheable("cards")
    @Transactional(readOnly = true)
    public Page<PaymentCardDto> getAllCards(Pageable pageable) {
        return paymentCardRep.findAll(pageable)
                .map(paymentCardMapper::toPaymentDto);
    }

    @Cacheable(value = "cards", key = "#id")
    @Transactional(readOnly = true)
    public PaymentCardDto getCardById(Long id) {
        return paymentCardRep.findById(id)
                .map(paymentCardMapper::toPaymentDto)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    @Cacheable(value = "userCards", key = "#userId")
    @Transactional(readOnly = true)
    public List<PaymentCardDto> getCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return paymentCardMapper.toDtoPaymentList(paymentCardRep.findByUserId(userId));
    }

    @CachePut(value = "cards", key = "#id")
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

    @CachePut(value = "cards", key = "#id")
    @Transactional
    public void activateCard(Long id) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (card.getActive() == true) {
            throw new IllegalStateException("Card already in this state");
        }
        paymentCardRep.updateCardStatus(id, true);
    }

    @CachePut(value = "cards", key = "#id")
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

    @CacheEvict(value = { "cards", "userCards" }, key = "#id")
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

    @CacheEvict(value = {"cards","userCards"},allEntries = true) // allEntries = true => удаляю все записи игнор ключ
    public void clearAllCache() {
        System.out.println("Clearing all card caches");
    }

}
