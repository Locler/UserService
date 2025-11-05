package com.repositories;

import com.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRep extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    // JPQL модификация - изменение статуса (activate/deactivate)
    @Modifying
    @Query("UPDATE User u SET u.active = :status WHERE u.id = :id")
    void updateUserStatus(@Param("id") Long id, @Param("status") boolean status);

}
