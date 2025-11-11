package com.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder(toBuilder = true)
public class PaymentCardDto {

    private Long id;

    @NotNull
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13 to 19 digits")
    private String number;

    @NotNull
    @Size(max = 100, message = "Card holder cannot be longer than 100 characters")
    private String holder;

    @NotNull
    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @NotNull
    private Boolean active;

    @NotNull(message = "User ID is required")
    private Long userId;
}
