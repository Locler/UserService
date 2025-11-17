package com.controllers;

import com.dto.UserDto;
import com.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto created = userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) @Size(min = 2, max = 50) String name,
            @RequestParam(required = false) @Size(min = 2, max = 50) String surname,
            @PageableDefault() Pageable pageable) {

        Page<UserDto> users = userService.getAllUsers(name, surname, pageable);
        return ResponseEntity.ok(users);
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable @Min(value = 1, message = "ID must be positive") Long id, @Valid @RequestBody UserDto userDto) {
        UserDto updated = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updated);
    }


    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }


    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
