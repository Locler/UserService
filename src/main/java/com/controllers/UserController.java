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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    // GET BY EMAIL (ADMIN only)
    @GetMapping("/by-email")
    public ResponseEntity<UserDto> getUserByEmail(
            @RequestParam @Email String email,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(userService.getUserByEmail(email, roles));
    }

    // CREATE USER (ADMIN only)
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserDto userDto,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        UserDto created = userService.createUser(userDto, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET USER BY ID (ADMIN or USER)
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(userService.getUserById(id, requesterId, roles));
    }

    // GET ALL USERS (ADMIN only)
    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) @Size(min = 2, max = 50) String name,
            @RequestParam(required = false) @Size(min = 2, max = 50) String surname,
            @PageableDefault Pageable pageable,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(userService.getAllUsers(name, surname, pageable, roles));
    }

    // UPDATE USER (ADMIN or USER)
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UserDto userDto,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        return ResponseEntity.ok(userService.updateUser(id, userDto, requesterId, roles));
    }

    // DEACTIVATE USER (ADMIN only)
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        userService.deactivateUser(id, roles);
        return ResponseEntity.ok().build();
    }

    // ACTIVATE USER (ADMIN only)
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        userService.activateUser(id, roles);
        return ResponseEntity.ok().build();
    }

    // DELETE USER (ADMIN only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Min(1) Long id,
            @RequestHeader("X-User-Roles") String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        userService.deleteUser(id, roles);
        return ResponseEntity.noContent().build();
    }
}