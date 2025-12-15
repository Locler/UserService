package com.services;

import com.accessChecker.AccessChecker;
import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.exceptions.BadRequestException;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PaymentCardService {

    private final PaymentCardMapper paymentCardMapper;
    private final PaymentCardRep paymentCardRep;
    private final UserRep userRepository;
    private final CacheManager cacheManager;
    private final AccessChecker accessChecker;

    @Autowired
    public PaymentCardService(PaymentCardMapper paymentCardMapper, PaymentCardRep paymentCardRep,
                              UserRep userRepository, CacheManager cacheManager,
                              AccessChecker accessChecker) {
        this.paymentCardMapper = paymentCardMapper;
        this.paymentCardRep = paymentCardRep;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.accessChecker = accessChecker;
    }

    @CachePut(value = "cards", key = "#result.id")
    @CacheEvict(value = "userCards", key = "#userId")
    @Transactional
    public PaymentCardDto createCard(Long userId, PaymentCardDto dto, Long requesterId, Set<String> roles) {
        accessChecker.checkUserAccess(userId, requesterId, roles);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getActive()) throw new BadRequestException("Cannot create card for inactive user");

        if (user.getPaymentCards() != null && user.getPaymentCards().size() >= 5)
            throw new IllegalStateException("User cannot have more than 5 cards");

        if (dto.getExpirationDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expiration date must be in the future");

        if (paymentCardRep.existsByNumber(dto.getNumber()))
            throw new IllegalArgumentException("Card number already exists");

        PaymentCard card = paymentCardMapper.toPaymentCardEntity(dto);
        card.setUser(user);
        card.setActive(true);

        return paymentCardMapper.toPaymentDto(paymentCardRep.save(card));
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardDto> getAllCards(Pageable pageable, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);
        return paymentCardRep.findAll(pageable).map(paymentCardMapper::toPaymentDto);
    }

    @Cacheable(value = "cards", key = "#id")
    @Transactional(readOnly = true)
    public PaymentCardDto getCardById(Long id, Long requesterId, Set<String> roles) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        accessChecker.checkUserAccess(card.getUser().getId(), requesterId, roles);
        return paymentCardMapper.toPaymentDto(card);
    }

    @Cacheable(value = "userCards", key = "#userId")
    @Transactional(readOnly = true)
    public List<PaymentCardDto> getCardsByUserId(Long userId, Long requesterId, Set<String> roles) {
        accessChecker.checkUserAccess(userId, requesterId, roles);
        if (!userRepository.existsById(userId))
            throw new IllegalArgumentException("User not found with id: " + userId);
        return paymentCardMapper.toDtoPaymentList(paymentCardRep.findByUserId(userId));
    }

    @Caching(
            put = {@CachePut(value = "cards", key = "#id")},
            evict = {@CacheEvict(value = "userCards", key = "#result.userId")}
    )
    @Transactional
    public PaymentCardDto updateCard(Long id, PaymentCardDto dto, Long requesterId, Set<String> roles) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        accessChecker.checkUserAccess(card.getUser().getId(), requesterId, roles);

        if (!card.getActive())
            throw new IllegalStateException("Cannot update inactive card");
        if (dto.getExpirationDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expiration date must be in the future");

        card.setNumber(dto.getNumber());
        card.setHolder(dto.getHolder());
        card.setExpirationDate(dto.getExpirationDate());
        card.setActive(dto.getActive());

        PaymentCard updated = paymentCardRep.save(card);
        Objects.requireNonNull(cacheManager.getCache("userCards")).evict(updated.getUser().getId());
        return paymentCardMapper.toPaymentDto(updated);
    }

    @CacheEvict(value = "cards", key = "#id")
    @Transactional
    public void activateCard(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (card.getActive()) throw new IllegalStateException("Card already active");
        card.setActive(true);
        paymentCardRep.save(card);
    }

    @CacheEvict(value = "cards", key = "#id")
    @Transactional
    public void deactivateCard(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        if (!card.getActive()) throw new IllegalStateException("Card already inactive");
        card.setActive(false);
        paymentCardRep.save(card);
    }

    @Caching(evict = {@CacheEvict(value = "cards", key = "#id")})
    @Transactional
    public void deleteCard(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        card.setActive(false);
        paymentCardRep.delete(card);
        Objects.requireNonNull(cacheManager.getCache("userCards")).evict(card.getUser().getId());
    }

    public void clearAllCache() {
        Objects.requireNonNull(cacheManager.getCache("cards")).clear();
        Objects.requireNonNull(cacheManager.getCache("userCards")).clear();
    }

}
