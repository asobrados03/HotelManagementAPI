package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.usecase.ClientUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

@RestController
@RequestMapping("/api")
public class ClientController {
    private final ClientUseCase clientUseCase;

    public ClientController(ClientUseCase clientUseCase) {
        this.clientUseCase = clientUseCase;
    }

    @GetMapping("/admin/clients")
    public ResponseEntity<?> getAllClients() {
        try {
            List<Client> response = clientUseCase.getAllClients();

            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay clientes registrados en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin/client/{id}")
    public ResponseEntity<?> getClientById(@PathVariable long id) {
        try {
            Client response = clientUseCase.getClientById(id)
                    .orElseThrow(() -> new RuntimeException("El cliente solicitado no existe"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/admin/client/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable long id) {
        try {
            int response = clientUseCase.deleteClient(id);

            if (response == 1) {
                return ResponseEntity.ok("Cliente con id: " + id + "eliminado correctamente.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El cliente no ha sido eliminado");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}