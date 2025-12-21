package com.services;

import com.accessChecker.AccessChecker;
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
import java.util.Set;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserRep userRepository;
    private final AccessChecker accessChecker;

    @Autowired
    public UserService(UserMapper userMapper,
                       UserRep userRepository,
                       AccessChecker accessChecker) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
    }

    @CachePut(value = "users", key = "#result.id")
    public UserDto createUser(UserDto dto, Set<String> roles, boolean isServiceCall) {

        accessChecker.checkAdminAccess(roles, isServiceCall); // проверка с флагом

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

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with email: " + email));
    }

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id, Long requesterId, Set<String> roles) {

        accessChecker.checkUserAccess(id, requesterId, roles);

        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String name, String surname,
                                     Pageable pageable, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        Specification<User> spec = UserSpecification.firstNameContains(name)
                .and(UserSpecification.surnameContains(surname));

        return userRepository.findAll(spec, pageable)
                .map(userMapper::toDto);
    }

    @CachePut(value = "users", key = "#id")
    public UserDto updateUser(Long id, UserDto dto,
                              Long requesterId, Set<String> roles) {
        accessChecker.checkUserAccess(id, requesterId, roles);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalStateException("Cannot update deactivated user");
        }

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setBirthDate(dto.getBirthDate());

        return userMapper.toDto(userRepository.save(user));
    }

    @CacheEvict(value = "users", key = "#id")
    public void activateUser(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getActive()) {
            throw new IllegalStateException("User already active");
        }

        user.setActive(true);
    }

    @CacheEvict(value = "users", key = "#id")
    public void deactivateUser(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalStateException("User already inactive");
        }

        user.setActive(false);
    }

    @CacheEvict(value = {"users", "userCards"}, key = "#id")
    public void deleteUser(Long id, Set<String> roles) {
        accessChecker.checkAdminAccess(roles);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        userRepository.delete(user);
    }

    @CacheEvict(value = {"users", "userCards"}, allEntries = true)
    public void clearAllCache() {
        System.out.println("Clearing all user caches");
    }

}
