package com.TicketSystem.bookingservice.service;

import com.TicketSystem.bookingservice.client.InventoryServiceClient;
import com.TicketSystem.bookingservice.entity.Customer;
import com.TicketSystem.bookingservice.repository.CustomerRepository;
import com.TicketSystem.bookingservice.request.BookingRequest;
import com.TicketSystem.bookingservice.response.BookingResponse;
import com.TicketSystem.bookingservice.response.InventoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryServiceClient;

    @Autowired
    public BookingService(final CustomerRepository customerRepository,
                          final InventoryServiceClient inventoryServiceClient
                          ) {
        this.customerRepository = customerRepository;
        this.inventoryServiceClient = inventoryServiceClient;

    }

    public BookingResponse createBooking(final BookingRequest request) {
        final Customer customer = customerRepository.findById(request.getUserId()).orElse(null);
        if (customer == null) {
            throw new RuntimeException("User not found");
        }
        final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(request.getEventId());
        System.out.println("Inventory Service Response" + inventoryResponse);


        return BookingResponse.builder().build();
    }
}