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

    @PostMapping("/user/{userId}")
    public ResponseEntity<PaymentCardDto> createCard(@PathVariable @Min(value = 1, message = "ID must be positive") Long userId,@Valid @RequestBody PaymentCardDto dto) {
        PaymentCardDto created = paymentCardService.createCard(userId,dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        return ResponseEntity.ok(paymentCardService.getCardById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(@PageableDefault() Pageable pageable) {
        return ResponseEntity.ok(paymentCardService.getAllCards(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentCardDto>> getCardsByUserId(@PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(@PathVariable @Min(1) Long id, @Valid @RequestBody PaymentCardDto dto) {
        return ResponseEntity.ok(paymentCardService.updateCard(id, dto));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(@PathVariable @Min(1) Long id) {
        paymentCardService.deactivateCard(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable @Min(1) Long id) {
        paymentCardService.activateCard(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable @Min(1) Long id) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
