package com.services;

import com.accessChecker.AccessChecker;
import com.dto.UserDto;
import com.entities.User;
import com.mappers.UserMapper;
import com.repositories.UserRep;
import com.specifications.UserSpecification;
import io.jsonwebtoken.Claims;
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

    private final JwtService jwtService;

    private final AccessChecker accessChecker;


    @Autowired
    public UserService(UserMapper userMapper, UserRep userRepository, JwtService jwtService, AccessChecker accessChecker) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.accessChecker = accessChecker;
    }

    @CachePut(value = "users", key = "#result.id")
    @Transactional
    public UserDto createUser(UserDto dto,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

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
    public UserDto getUserById(Long id,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkUserAccess(id, claims);

        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String name, String surname, Pageable pageable,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        Specification<User> spec = UserSpecification.firstNameContains(name)
                .and(UserSpecification.surnameContains(surname));

        return userRepository.findAll(spec, pageable)
                .map(userMapper::toDto);
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    public UserDto updateUser(Long id, UserDto dto,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkUserAccess(id, claims);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getActive()) throw new IllegalStateException("Cannot update deactivated user");

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setBirthDate(dto.getBirthDate());

        return userMapper.toDto(userRepository.save(user));
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void activateUser(Long id,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getActive()) throw new IllegalStateException("User already active");

        user.setActive(true);
        userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deactivateUser(Long id,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getActive()) throw new IllegalStateException("User already inactive");

        user.setActive(false);
        userRepository.save(user);
    }

    @CacheEvict(value = { "users", "userCards" }, key = "#id")
    @Transactional
    public void deleteUser(Long id,String token) {
        Claims claims = jwtService.parse(token);
        accessChecker.checkAdminAccess(claims);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
    }

    @CacheEvict(value = { "users", "userCards" }, allEntries = true)
    public void clearAllCache() {
        System.out.println("Clearing all user caches");
    }

}
