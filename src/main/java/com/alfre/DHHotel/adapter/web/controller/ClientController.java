package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.usecase.ClientUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * This class handles HTTP requests for managing client resources.
 * It delegates business logic to the ClientUseCase.
 *
 * @author Alfredo Sobrados González
 */
@RestController
@RequestMapping("/api")
public class ClientController {
    private final ClientUseCase clientUseCase;

    /**
     * Constructs a ClientController with the provided ClientUseCase.
     *
     * @param clientUseCase the use case that contains the business logic for client operations
     */
    public ClientController(ClientUseCase clientUseCase) {
        this.clientUseCase = clientUseCase;
    }

    /**
     * Retrieves all registered clients.
     * <p>
     * If no clients are registered, a RuntimeException is thrown and a 404 Not Found response is returned.
     * </p>
     *
     * @return a ResponseEntity containing the list of clients or an error message if none are found
     */
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

    /**
     * Retrieves a client by its unique identifier.
     *
     * @param id the unique identifier of the client
     * @return a ResponseEntity containing the client if found, or a 404 Not Found response with an error message if not
     */
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

    /**
     * Deletes a client by its unique identifier.
     *
     * @param id the unique identifier of the client to delete
     * @return a ResponseEntity with a success message if deletion is successful,
     *         or a 404 Not Found response with an error message if deletion fails
     */
    @DeleteMapping("/admin/client/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable long id) {
        try {
            int response = clientUseCase.deleteClient(id);

            if (response == 1) {
                return ResponseEntity.ok("Cliente con id: " + id + " eliminado correctamente.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El cliente no ha sido eliminado");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}