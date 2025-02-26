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

/**
 * This class handles HTTP requests related to room management.
 * It provides endpoints for retrieving, creating, updating,
 * deleting rooms, as well as retrieving rooms by type, available rooms, and
 * updating a room's status.
 */
@RestController
@RequestMapping("/api")
public class RoomController {
    private final RoomUseCase roomUseCase;

    /**
     * Constructs a RoomController with the provided RoomUseCase.
     *
     * @param roomUseCase the use case that contains the business logic for room operations
     */
    public RoomController(RoomUseCase roomUseCase) {
        this.roomUseCase = roomUseCase;
    }

    /**
     * Retrieves all rooms in the system.
     * <p>
     * If no rooms are registered, a RuntimeException is thrown and a 404 Not Found
     * response is returned.
     * </p>
     *
     * @return a ResponseEntity containing the list of rooms if found, or an error message otherwise
     */
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

    /**
     * Retrieves a room by its unique identifier.
     *
     * @param id the unique identifier of the room
     * @return a ResponseEntity containing the room if found, or a 404 Not Found response with an error message if not found
     */
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

    /**
     * Creates a new room.
     *
     * @param newRoom the Room object containing the room details to be created
     * @return a ResponseEntity containing the generated room identifier, or a 400 Bad Request response with an error message if creation fails
     */
    @PostMapping("/admin/room")
    public ResponseEntity<?> createRoom(@RequestBody Room newRoom) {
        try {
            return ResponseEntity.ok(roomUseCase.createRoom(newRoom));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Updates an existing room.
     *
     * @param updatedRoom the Room object containing updated room information
     * @param id the unique identifier of the room to update
     * @return a ResponseEntity with a success message if the update is successful, or an error response if not
     */
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

    /**
     * Deletes a room by its unique identifier.
     *
     * @param idDeleteRoom the unique identifier of the room to be deleted
     * @return a ResponseEntity with a success message if deletion is successful,
     *         or an error response if deletion fails
     */
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

    /**
     * Retrieves rooms by type.
     *
     * @param type the RoomType used to filter the rooms
     * @return a ResponseEntity containing a list of RoomDTO objects if found,
     *         or a 404 Not Found response with an error message if none are found
     */
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

    /**
     * Retrieves all available rooms.
     *
     * @return a ResponseEntity containing a list of RoomDTO objects for available rooms,
     *         or a 404 Not Found response with an error message if none are available
     */
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

    /**
     * Updates the status of a room.
     *
     * @param id the unique identifier of the room
     * @param status the new RoomStatus to set
     * @return a ResponseEntity with a success message if the update is successful,
     *         or an error response if the update fails
     */
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

    /**
     * Retrieves all rooms that are under maintenance.
     *
     * @return a ResponseEntity containing a list of rooms in maintenance,
     *         or a 404 Not Found response with an error message if none are found
     */
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