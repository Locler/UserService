package com.services;

import com.accessChecker.AccessChecker;
import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import com.entities.User;
import com.exceptions.BadRequestException;
import com.mappers.PaymentCardMapper;
import com.repositories.PaymentCardRep;
import com.repositories.UserRep;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PaymentCardService {

    private final PaymentCardMapper paymentCardMapper;

    private final PaymentCardRep paymentCardRep;

    private final UserRep userRepository;

    private final CacheManager cacheManager;

    private final JwtService jwtService;

    private final AccessChecker accessChecker;

    @Autowired
    public PaymentCardService(PaymentCardMapper paymentCardMapper, PaymentCardRep paymentCardRep,
                              UserRep userRepository, CacheManager cacheManager,
                              JwtService jwtService, AccessChecker accessChecker) {
        this.paymentCardMapper = paymentCardMapper;
        this.paymentCardRep = paymentCardRep;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.jwtService = jwtService;
        this.accessChecker = accessChecker;
    }

    @CachePut(value = "cards", key = "#result.id") // добавляю новый элемент
    @CacheEvict(value = "userCards", key = "#userId") // сношу список всех карт пользователя для актуальности данных
    @Transactional()
    public PaymentCardDto createCard(Long userId, PaymentCardDto dto,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkUserAccess(userId, claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getActive()) throw new BadRequestException("Cannot create card for inactive user");

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


    @Transactional(readOnly = true)
    public Page<PaymentCardDto> getAllCards(Pageable pageable,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        return paymentCardRep.findAll(pageable)
                .map(paymentCardMapper::toPaymentDto);
    }


    @Cacheable(value = "cards", key = "#id")
    @Transactional(readOnly = true)
    public PaymentCardDto getCardById(Long id,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkUserAccess(id,claims);

        return paymentCardRep.findById(id)
                .map(paymentCardMapper::toPaymentDto)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    @Cacheable(value = "userCards", key = "#userId")
    @Transactional(readOnly = true)
    public List<PaymentCardDto> getCardsByUserId(Long userId,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkUserAccess(userId, claims);

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        return paymentCardMapper.toDtoPaymentList(paymentCardRep.findByUserId(userId));
    }

    @Caching(
            put = { @CachePut(value = "cards", key = "#id") },
            evict = { @CacheEvict(value = "userCards", key = "#result.userId") }
    )
    @Transactional
    public PaymentCardDto updateCard(Long id, PaymentCardDto dto,String token) {
        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        Claims claims = jwtService.parse(token);
        accessChecker.checkUserAccess(card.getUser().getId(), claims);

        if (!card.getActive()) {
            throw new IllegalStateException("Cannot update inactive card");
        }

        if (dto.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date must be in the future");
        }

        card.setNumber(dto.getNumber());
        card.setHolder(dto.getHolder());
        card.setExpirationDate(dto.getExpirationDate());
        card.setActive(dto.getActive());

        PaymentCard updated = paymentCardRep.save(card);

        // evict кэш списка карт пользователя
        Objects.requireNonNull(cacheManager.getCache("userCards")).evict(updated.getUser().getId());

        return paymentCardMapper.toPaymentDto(updated);
    }

    @CacheEvict(value = "cards", key = "#id")
    @Transactional
    public void activateCard(Long id,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (card.getActive() == true) {
            throw new IllegalStateException("Card already in this state");
        }
        card.setActive(true);
        paymentCardRep.save(card);
    }

    @CacheEvict(value = "cards", key = "#id")
    @Transactional
    public void deactivateCard(Long id,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + id));

        if (!card.getActive()) {
            throw new IllegalStateException("Card already inactive");
        }

        card.setActive(false);
        paymentCardRep.save(card);
    }

    @Caching(evict = {
            @CacheEvict(value = "cards", key = "#id"),
    })
    @Transactional
    public void deleteCard(Long id,String token) {

        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        PaymentCard card = paymentCardRep.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + id));
        if (!card.getActive()) {
            throw new IllegalStateException("Card already inactive");
        }
        card.setActive(false);
        paymentCardRep.delete(card);

        // Очистка userCards вручную
        Objects.requireNonNull(cacheManager.getCache("userCards")).evict(card.getUser().getId());
    }

    @CacheEvict(value = {"cards","userCards"},allEntries = true) // allEntries = true => удаляю все записи игнор ключ
    public void clearAllCache() {
        System.out.println("Clearing all card caches");
    }

}
