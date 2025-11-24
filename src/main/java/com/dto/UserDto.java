package com.dto;

import com.entities.PaymentCard;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class UserDto implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 50, message = "Name cannot be longer than 50 characters")
    private String name;

    @NotNull
    @Size(max = 50, message = "Surname cannot be longer than 50 characters")
    private String surname;

    @NotNull
    private LocalDate birthDate;

    @NotNull
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email cannot be longer than 100 characters")
    private String email;

    private Boolean active;

    List<PaymentCard> paymentCards;
}
