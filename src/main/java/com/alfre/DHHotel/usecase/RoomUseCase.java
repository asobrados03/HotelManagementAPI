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

@Service
public class RoomUseCase {
    private final RoomRepository roomRepository;

    public RoomUseCase(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.getAllRooms();
    }

    public Optional<Room> getRoomById(long id){
        return roomRepository.getRoomById(id);
    }

    public long createRoom(Room newRoom) {
        if (newRoom.price_per_night.stripTrailingZeros().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }

        return roomRepository.createRoom(newRoom);
    }

    public int updateRoom(Room updatedRoom, long id){
        roomRepository.getRoomById(id).orElseThrow(() -> new IllegalArgumentException("La habitación no existe"));
        return roomRepository.updateRoom(updatedRoom, id);
    }

    public int deleteRoom(long idDeleteRoom) {
        roomRepository.getRoomById(idDeleteRoom).orElseThrow(() -> new IllegalArgumentException("La habitación no existe"));
        return roomRepository.deleteRoom(idDeleteRoom);
    }

    public List<RoomDTO> getRoomsByType(RoomType type) {
        return roomRepository.getRoomsByType(type)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<RoomDTO> getAvailableRooms() {
        return roomRepository.getAvailableRooms()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private RoomDTO toDTO(Room room) {
        return RoomDTO.builder()
                .room_number(room.room_number)
                .type(room.type)
                .price_per_night(room.price_per_night)
                .status(room.status)
                .build();
    }

    public int updateStatus(long id, RoomStatus status) {
        return roomRepository.updateStatus(id, status);
    }

    public List<Room> getRoomsInMaintenance() {
        return roomRepository.getRoomsInMaintenance();
    }
}