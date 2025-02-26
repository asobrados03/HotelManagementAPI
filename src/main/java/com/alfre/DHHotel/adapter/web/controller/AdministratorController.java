package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.usecase.AdministratorUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * This class handles HTTP requests for managing administrator resources.
 * It delegates business logic to the AdministratorUseCase.
 *
 * @author Alfredo Sobrados González
 */
@RestController
@RequestMapping("/api/superadmin")
public class AdministratorController {
    private final AdministratorUseCase administratorUseCase;

    /**
     * Constructs an AdministratorController with the provided AdministratorUseCase.
     *
     * @param administratorUseCase the use case that contains the business logic for administrators
     */
    public AdministratorController(AdministratorUseCase administratorUseCase) {
        this.administratorUseCase = administratorUseCase;
    }

    /**
     * Retrieves all administrators from the system.
     * <p>
     * If no administrators are registered, a RuntimeException is thrown and a 404 Not Found response is returned.
     * </p>
     *
     * @return a ResponseEntity containing a list of Administrator objects if found,
     *         or a 404 Not Found status with an error message otherwise
     */
    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdministrators() {
        try {
            List<Administrator> response = administratorUseCase.getAllAdministrators();
            if(response.equals(emptyList())) {
                throw new RuntimeException("No hay administradores registrados en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Retrieves an administrator by the associated user ID.
     *
     * @param userId the user ID of the administrator to retrieve
     * @return a ResponseEntity containing the Administrator object if found,
     *         or a 404 Not Found status with an error message if not found
     */
    @GetMapping("/admin/userId/{userId}")
    public ResponseEntity<?> getAdministratorByUserId(@PathVariable long userId) {
        try {
            Administrator response = administratorUseCase.getAdministratorByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("El administrador solicitado no existe."));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Retrieves an administrator by their unique identifier.
     *
     * @param id the unique identifier of the administrator to retrieve
     * @return a ResponseEntity containing the Administrator object if found,
     *         or a 404 Not Found status with an error message if not found
     */
    @GetMapping("/admin/id/{id}")
    public ResponseEntity<?> getAdministratorById(@PathVariable long id) {
        try {
            Administrator response = administratorUseCase.getAdministratorById(id)
                    .orElseThrow(() -> new RuntimeException("El administrador solicitado no existe."));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Updates the administrator information for a given user.
     *
     * @param administrator the Administrator object containing updated data
     * @param userId the user ID associated with the administrator to update
     * @return a ResponseEntity with a success message if the update is successful,
     *         or a 400 Bad Request status with an error message if the update fails
     */
    @PutMapping("/admin/{userId}")
    public ResponseEntity<String> updateAdministrator(@RequestBody Administrator administrator,
                                                      @PathVariable long userId) {
        int rowsAffected = administratorUseCase.updateAdministrator(administrator, userId);

        if(rowsAffected == 1){
            return ResponseEntity.ok("La actualización se ha hecho correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha podido actualizar.");
        }
    }

    /**
     * Deletes an administrator by their unique identifier.
     *
     * @param id the unique identifier of the administrator to delete
     * @return a ResponseEntity with a success message if deletion is successful,
     *         or a 404 Not Found/400 Bad Request status with an error message if deletion fails
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteAdministrator(@PathVariable long id) {
        try {
            int rowsAffected = administratorUseCase.deleteAdministrator(id);

            if (rowsAffected == 1) {
                return ResponseEntity.ok("El administrador con id: " + id + " se ha eliminado correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha podido eliminar.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}