package com.multimodule.booking.adapter.in.web;

import com.multimodule.booking.application.port.input.query.GetBookingByIdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final GetBookingByIdQuery getBookingByIdQuery;

}
