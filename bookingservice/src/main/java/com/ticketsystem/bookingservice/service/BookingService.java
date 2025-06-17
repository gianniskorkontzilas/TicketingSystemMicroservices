package com.ticketsystem.bookingservice.service;

import com.ticketsystem.bookingservice.client.InventoryServiceClient;
import com.ticketsystem.bookingservice.entity.Customer;
import com.ticketsystem.bookingservice.event.BookingEvent;
import com.ticketsystem.bookingservice.repository.CustomerRepository;
import com.ticketsystem.bookingservice.request.BookingRequest;
import com.ticketsystem.bookingservice.response.BookingResponse;
import com.ticketsystem.bookingservice.response.InventoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @Autowired
    public BookingService(final CustomerRepository customerRepository,
                          final InventoryServiceClient inventoryServiceClient,
                          final KafkaTemplate<String, BookingEvent> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.inventoryServiceClient = inventoryServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public BookingResponse createBooking(final BookingRequest request) {
        final Customer customer = customerRepository.findById(request.getUserId()).orElse(null);
        if (customer == null) {
            throw new RuntimeException("User not found");
        }

        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(request.getEventId());
        log.info("Inventory Response: {}", inventoryResponse);

        if (inventoryResponse.getCapacity() < request.getTicketCount()) {
            throw new RuntimeException("Not enough inventory");
        }

        final BookingEvent bookingEvent = createBookingEvent(request, customer, inventoryResponse);
        kafkaTemplate.send("booking", bookingEvent);
        log.info("Booking sent to Kafka: {}", bookingEvent);

        return BookingResponse.builder()
                .userId(bookingEvent.getUserId())
                .eventId(bookingEvent.getEventId())
                .ticketCount(bookingEvent.getTicketCount())
                .totalPrice(bookingEvent.getTotalPrice())
                .build();
    }

    private BookingEvent createBookingEvent(final BookingRequest request,
                                            final Customer customer,
                                            final InventoryResponse inventoryResponse) {
        return BookingEvent.builder()
                .userId(customer.getId())
                .eventId(request.getEventId())
                .ticketCount(request.getTicketCount())
                .totalPrice(inventoryResponse.getTicketPrice().multiply(BigDecimal.valueOf(request.getTicketCount())))
                .build();
    }
}
