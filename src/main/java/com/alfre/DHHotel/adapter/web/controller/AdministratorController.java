package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.usecase.AdministratorUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

@RestController
@RequestMapping("/api/superadmin")
public class AdministratorController {
    private final AdministratorUseCase administratorUseCase;

    public AdministratorController(AdministratorUseCase administratorUseCase) {
        this.administratorUseCase = administratorUseCase;
    }

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