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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Validated
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) return Set.of();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    // CREATE CARD (USER or ADMIN)
    @PostMapping("/user/{userId}")
    public ResponseEntity<PaymentCardDto> createCard(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody PaymentCardDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        PaymentCardDto created = paymentCardService.createCard(userId, dto, requesterId, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET CARD BY ID (USER or ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(paymentCardService.getCardById(id, requesterId, roles));
    }

    // GET ALL CARDS (ADMIN only)
    @GetMapping
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(
            @PageableDefault Pageable pageable,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(paymentCardService.getAllCards(pageable, roles));
    }

    // GET CARDS BY USER ID (USER can only see own cards, ADMIN can see all)
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<PaymentCardDto>> getCardsByUserId(
            @PathVariable @Min(1) Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId, requesterId, roles));
    }

    // UPDATE CARD (USER or ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody PaymentCardDto dto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(paymentCardService.updateCard(id, dto, requesterId, roles));
    }

    // DEACTIVATE CARD (ADMIN only)
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        paymentCardService.deactivateCard(id, roles);
        return ResponseEntity.ok().build();
    }

    // ACTIVATE CARD (ADMIN only)
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateCard(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        paymentCardService.activateCard(id, roles);
        return ResponseEntity.ok().build();
    }

    // DELETE CARD (ADMIN only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        paymentCardService.deleteCard(id, roles);
        return ResponseEntity.noContent().build();
    }
}