package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.web.dto.RoomDTO;
import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class that handles business logic related to rooms.
 * <p>
 * This class provides methods to manage rooms, including retrieving, creating, updating,
 * deleting, and filtering rooms by status and type.
 * </p>
 *
 * @author Alfredo Sobrados González
 */
@Service
public class RoomUseCase {
    private final RoomRepository roomRepository;

    /**
     * Constructs a RoomUseCase with the specified RoomRepository.
     *
     * @param roomRepository The repository handling room persistence operations.
     */
    public RoomUseCase(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Retrieves all rooms from the repository.
     *
     * @return A list of all rooms.
     */
    public List<Room> getAllRooms() {
        return roomRepository.getAllRooms();
    }

    /**
     * Retrieves a room by its ID.
     *
     * @param id The ID of the room.
     * @return An {@link Optional} containing the room if found.
     */
    public Optional<Room> getRoomById(long id) {
        return roomRepository.getRoomById(id);
    }

    /**
     * Creates a new room.
     *
     * @param newRoom The room details.
     * @return The ID of the newly created room.
     * @throws IllegalArgumentException If the price per night is zero or negative.
     */
    public long createRoom(Room newRoom) {
        if (newRoom.price_per_night.stripTrailingZeros().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }
        return roomRepository.createRoom(newRoom);
    }

    /**
     * Updates an existing room.
     *
     * @param updatedRoom The updated room details.
     * @param id The ID of the room to update.
     * @return The number of rows affected.
     * @throws IllegalArgumentException If the room does not exist.
     */
    public int updateRoom(Room updatedRoom, long id) {
        roomRepository.getRoomById(id)
                .orElseThrow(() -> new IllegalArgumentException("La habitación no existe"));
        return roomRepository.updateRoom(updatedRoom, id);
    }

    /**
     * Deletes a room by its ID.
     *
     * @param idDeleteRoom The ID of the room to delete.
     * @return The number of rows affected.
     * @throws IllegalArgumentException If the room does not exist.
     */
    public int deleteRoom(long idDeleteRoom) {
        roomRepository.getRoomById(idDeleteRoom)
                .orElseThrow(() -> new IllegalArgumentException("La habitación no existe"));
        return roomRepository.deleteRoom(idDeleteRoom);
    }

    /**
     * Retrieves a list of rooms filtered by type.
     *
     * @param type The type of room (SINGLE, DOUBLE, SUITE).
     * @return A list of rooms matching the specified type, converted to DTO format.
     */
    public List<RoomDTO> getRoomsByType(RoomType type) {
        return roomRepository.getRoomsByType(type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of available rooms.
     *
     * @return A list of available rooms in DTO format.
     */
    public List<RoomDTO> getAvailableRooms() {
        return roomRepository.getAvailableRooms()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Room entity to a RoomDTO.
     *
     * @param room The Room entity.
     * @return The corresponding RoomDTO.
     */
    private RoomDTO toDTO(Room room) {
        return RoomDTO.builder()
                .room_number(room.room_number)
                .type(room.type)
                .price_per_night(room.price_per_night)
                .status(room.status)
                .build();
    }

    /**
     * Updates the status of a room.
     *
     * @param id The ID of the room.
     * @param status The new status (AVAILABLE, OCCUPIED, MAINTENANCE).
     * @return The number of rows affected.
     */
    public int updateStatus(long id, RoomStatus status) {
        return roomRepository.updateStatus(id, status);
    }

    /**
     * Retrieves a list of rooms currently under maintenance.
     *
     * @return A list of rooms in maintenance.
     */
    public List<Room> getRoomsInMaintenance() {
        return roomRepository.getRoomsInMaintenance();
    }
}