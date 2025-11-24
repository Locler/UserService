package com.mappers;

import com.dto.PaymentCardDto;
import com.entities.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentCardMapper{

    //берем user.id у dto и вставляем в userId у ent
    @Mapping(source = "user.id", target = "userId")
    PaymentCardDto toPaymentDto(PaymentCard entity);

    @Mapping(source = "userId", target = "user.id")
    PaymentCard toPaymentCardEntity(PaymentCardDto dto);

    List<PaymentCardDto> toDtoPaymentList(List<PaymentCard> entities);


}
