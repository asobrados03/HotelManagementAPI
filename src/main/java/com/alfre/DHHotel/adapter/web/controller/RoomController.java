package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.adapter.web.dto.RoomDTO;
import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import com.alfre.DHHotel.usecase.RoomUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

@RestController
@RequestMapping("/api")
public class RoomController {
    private final RoomUseCase roomUseCase;

    public RoomController(RoomUseCase roomUseCase) {
        this.roomUseCase = roomUseCase;
    }

    @GetMapping("/admin/rooms")
    public ResponseEntity<?> getAllRooms() {
        try {
            List<Room> response = roomUseCase.getAllRooms();
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay habitaciones registrados en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin/room/{id}")
    public ResponseEntity<?> getRoomById(@PathVariable long id) {
        try {
            Room response = roomUseCase.getRoomById(id)
                    .orElseThrow(() -> new RuntimeException("La habitación solicitada no existe."));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/admin/room")
    public ResponseEntity<?> createRoom(@RequestBody Room newRoom) {
        try {
            return ResponseEntity.ok(roomUseCase.createRoom(newRoom));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/admin/room/{id}")
    public ResponseEntity<String> updateRoom(@RequestBody Room updatedRoom, @PathVariable long id) {
        try {
            int rowsAffected = roomUseCase.updateRoom(updatedRoom, id);

            if (rowsAffected == 1) {
                return ResponseEntity.ok("La actualización se ha hecho correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha podido actualizar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de servicio.");
        }
    }

    @DeleteMapping("admin/room/{idDeleteRoom}")
    public ResponseEntity<?> deleteRoom(@PathVariable long idDeleteRoom) {
        try {
            int rowsAffected = roomUseCase.deleteRoom(idDeleteRoom);

            if (rowsAffected == 1) {
                return ResponseEntity.ok("La habitación con id: " + idDeleteRoom + " se ha eliminado correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha podido eliminar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de servicio.");
        }
    }

    @GetMapping("/public/rooms/type/{type}")
    public ResponseEntity<?> getRoomsByType(@PathVariable RoomType type) {
        try {
            List<RoomDTO> response = roomUseCase.getRoomsByType(type);
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay habitaciones del tipo solicitado en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/public/rooms/available")
    public ResponseEntity<?> getAvailableRooms() {
        try {
            List<RoomDTO> response = roomUseCase.getAvailableRooms();
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay habitaciones disponibles en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/admin/rooms/{id}/status/{status}")
    public ResponseEntity<?> updateStatus(@PathVariable long id, @PathVariable RoomStatus status) {
        try {
            int rowsAffected = roomUseCase.updateStatus(id, status);

            if (rowsAffected == 1) {
                return ResponseEntity.ok("La actualización se ha hecho correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se ha podido actualizar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de servicio.");
        }
    }

    @GetMapping("/admin/rooms/maintenance")
    public ResponseEntity<?> getRoomsInMaintenance() {
        try {
            List<Room> response = roomUseCase.getRoomsInMaintenance();
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay habitaciones en mantenimiento en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}