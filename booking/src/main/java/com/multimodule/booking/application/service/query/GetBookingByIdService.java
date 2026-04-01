package com.multimodule.booking.application.service.query;

import com.multimodule.booking.application.dto.response.BookingResponse;
import com.multimodule.booking.application.mapper.BookingDataMapper;
import com.multimodule.booking.application.port.input.query.GetBookingByIdQuery;
import com.multimodule.booking.application.port.output.BookingQueryRepository;
import com.multimodule.booking.domain.exception.BookingNotFoundException;
import com.multimodule.booking.domain.valueobject.BookingId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetBookingByIdService implements GetBookingByIdQuery {
    public final BookingQueryRepository bookingQueryRepository;
    public final BookingDataMapper bookingDataMapper;

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID id) {
        return bookingQueryRepository.findById(BookingId.of(id))
                .map(bookingDataMapper::bookingToBookingResponse)
                .orElseThrow(() -> new BookingNotFoundException("User not found with id: " + id));
    }
}
