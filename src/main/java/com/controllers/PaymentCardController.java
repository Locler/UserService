package com.controllers;

import com.dto.PaymentCardDto;
import com.services.PaymentCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Validated
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    private String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        return header.substring(7);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<PaymentCardDto> createCard(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long userId,
            @Valid @RequestBody PaymentCardDto dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        PaymentCardDto created = paymentCardService.createCard(userId, dto, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return ResponseEntity.ok(paymentCardService.getCardById(id, token));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(@PageableDefault Pageable pageable,@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        return ResponseEntity.ok(paymentCardService.getAllCards(pageable,token));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PaymentCardDto>> getCardsByUserId(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long userId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId, token));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @Valid @RequestBody PaymentCardDto dto,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        return ResponseEntity.ok(paymentCardService.updateCard(id, dto, token));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        paymentCardService.deactivateCard(id, token);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateCard(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        paymentCardService.activateCard(id, token);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        paymentCardService.deleteCard(id, token);
        return ResponseEntity.noContent().build();
    }
}