package com.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder(toBuilder = true)
public class PaymentCardDto implements Serializable {

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

    private Boolean active;

    private Long userId;
}
