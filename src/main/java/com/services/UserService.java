package com.services;

import com.dto.UserDto;
import com.entities.User;
import com.mappers.UserMapper;
import com.repositories.UserRep;
import com.specifications.UserSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {

    private final UserMapper userMapper;

    private final UserRep userRepository;

    @Autowired
    public UserService(UserMapper userMapper, UserRep userRep) {
        this.userMapper = userMapper;
        this.userRepository = userRep;
    }

    @CachePut(value = "users", key = "#result.id")
    @Transactional
    public UserDto createUser(UserDto dto) {
        userRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException("User with this email already exists");
        }
        if (dto.getBirthDate() != null && dto.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Birth date cannot be in the future");
        }
        User user = userMapper.toEntity(dto);
        user.setActive(true);
        return userMapper.toDto(userRepository.save(user));
    }

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    @Cacheable(value = "users")
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String name, String surname, Pageable pageable) {
        Specification<User> spec = UserSpecification.firstNameContains(name)
                .and(UserSpecification.surnameContains(surname));
        return userRepository.findAll(spec, pageable)
                .map(userMapper::toDto);
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getActive() ) {
            throw new IllegalStateException("Cannot update a deactivated user");
        }

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setBirthDate(dto.getBirthDate());

        return userMapper.toDto(userRepository.save(user));
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getActive() == true) {
            throw new IllegalStateException("User is already in this state");
        }
        userRepository.updateUserStatus(id,true);
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!user.getActive()) {
            throw new IllegalStateException("User already inactive");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @CacheEvict(value = { "users", "userCards" }, key = "#id")
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getActive()) {
            throw new IllegalStateException("User already inactive");
        }
        user.setActive(false);
        userRepository.delete(user);
    }

    @CacheEvict(value = { "users", "userCards" }, allEntries = true)
    public void clearAllCache() {
        System.out.println("Clearing all user caches");
    }

}
