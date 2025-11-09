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
    @Mapping(target = "user.id", source = "userId")
    PaymentCardDto toPaymentDto(PaymentCard entity);

    @Mapping(target = "userId", source = "user.id")
    PaymentCard toPaymentCardEntity(PaymentCardDto dto);

    List<PaymentCardDto> toDtoPaymentList(List<PaymentCard> entities);


}
