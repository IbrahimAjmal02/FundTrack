package com.ibrahim.fundtrack.client;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsBySubaccountNumber(String subaccountNumber);
}
