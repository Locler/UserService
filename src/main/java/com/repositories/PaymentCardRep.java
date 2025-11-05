package com.repositories;

import com.entities.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRep extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findByUserId(Long userId);

    boolean existsByNumber(String number);

    // Native query
    @Query(value = "SELECT * FROM payment_cards pc WHERE pc.user_id = :userId", nativeQuery = true)
    List<PaymentCard> findCardsByUserNative(@Param("userId") Long userId);

    // JPQL update для статуса карты
    @Modifying
    @Query("UPDATE PaymentCard p SET p.active = :status WHERE p.id = :id")
    void updateCardStatus(@Param("id") Long id, @Param("status") boolean status);


}
