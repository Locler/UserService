package com.controllers;

import com.dto.UserDto;
import com.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    private String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        return header.substring(7);
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserDto> getUserByEmail(
            @RequestParam @Email String email,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        UserDto user = userService.getUserByEmail(email, token);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto,
                                              @RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        UserDto created = userService.createUser(userDto, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        UserDto user = userService.getUserById(id, token);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) @Size(min = 2, max = 50) String name,
            @RequestParam(required = false) @Size(min = 2, max = 50) String surname,
            @PageableDefault Pageable pageable,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        Page<UserDto> users = userService.getAllUsers(name, surname, pageable, token);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @Valid @RequestBody UserDto userDto,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        UserDto updated = userService.updateUser(id, userDto, token);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        userService.deactivateUser(id, token);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        userService.activateUser(id, token);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Min(value = 1, message = "ID must be positive") Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        userService.deleteUser(id, token);
        return ResponseEntity.noContent().build();
    }
}