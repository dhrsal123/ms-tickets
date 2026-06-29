package io.cinema.mstickets.mapper;

import io.cinema.mstickets.domain.dto.request.PaymentDTO;
import io.cinema.mstickets.domain.entity.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "status", target = "status")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "customerEmail", target = "customerEmail")
    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "paymentId", target = "paymentId")
    @Mapping(source = "date", target = "paymentDate")
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    TicketEntity toTicketEntity(PaymentDTO paymentDTO);

}
