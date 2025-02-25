package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.web.dto.RoomDTO;
import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoomUseCaseTest {
    @Mock
    private RoomRepository roomRepository;

    private RoomUseCase roomUseCase;

    @BeforeEach
    public void setup() {
        roomUseCase = new RoomUseCase(roomRepository);
    }

    @Test
    public void getAllRooms_success() {
        // Arrange: Preparamos una lista de habitaciones
        List<Room> roomList = new ArrayList<>();
        Room room = new Room();
        // Configuramos las propiedades de room si es necesario
        roomList.add(room);
        when(roomRepository.getAllRooms()).thenReturn(roomList);

        // Act: Ejecutamos el método
        List<Room> result = roomUseCase.getAllRooms();

        // Assert: Verificamos que el resultado sea el esperado y se llamó al método del repository
        assertEquals(roomList, result);
        verify(roomRepository).getAllRooms();
    }

    @Test
    public void getRoomById_success() {
        // Arrange: Creamos una reserva y simulamos su retorno para el identificador dado
        Room room = new Room();
        when(roomRepository.getRoomById(10L)).thenReturn(Optional.of(room));

        // Act
        Optional<Room> result = roomUseCase.getRoomById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(room, result.get());
        verify(roomRepository).getRoomById(10L);
    }

    @Test
    public void createRoom_success_validPrice_returnsRoomId() {
        Room newRoom = new Room();
        newRoom.price_per_night = new BigDecimal("100.00");
        long expectedId = 10L;
        when(roomRepository.createRoom(newRoom)).thenReturn(expectedId);

        long result = roomUseCase.createRoom(newRoom);
        assertEquals(expectedId, result);
        verify(roomRepository).createRoom(newRoom);
    }

    @Test
    public void createRoom_failure_invalidPrice_throwsException() {
        Room newRoom = new Room();
        // Establecemos un precio igual a 0 (o negativo) tras quitar ceros
        newRoom.price_per_night = BigDecimal.ZERO;
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                roomUseCase.createRoom(newRoom)
        );
        assertEquals("El precio debe ser mayor que 0", ex.getMessage());
    }

    @Test
    public void updateRoom_success_roomExists_returnsUpdatedRows() {
        long id = 5L;
        Room updatedRoom = new Room();
        updatedRoom.room_number = 101; // Ejemplo de campo a actualizar

        Room existingRoom = new Room();
        when(roomRepository.getRoomById(id)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.updateRoom(updatedRoom, id)).thenReturn(1);

        int result = roomUseCase.updateRoom(updatedRoom, id);
        assertEquals(1, result);
        verify(roomRepository).getRoomById(id);
        verify(roomRepository).updateRoom(updatedRoom, id);
    }

    @Test
    public void updateRoom_failure_roomDoesNotExist_throwsException() {
        long id = 5L;
        Room updatedRoom = new Room();
        when(roomRepository.getRoomById(id)).thenReturn(Optional.empty());
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                roomUseCase.updateRoom(updatedRoom, id)
        );
        assertEquals("La habitación no existe", ex.getMessage());
    }

    @Test
    public void deleteRoom_success_roomExists_returnsDeletedRows() {
        long idDeleteRoom = 3L;
        Room room = new Room();
        when(roomRepository.getRoomById(idDeleteRoom)).thenReturn(Optional.of(room));
        when(roomRepository.deleteRoom(idDeleteRoom)).thenReturn(1);

        int result = roomUseCase.deleteRoom(idDeleteRoom);
        assertEquals(1, result);
        verify(roomRepository).getRoomById(idDeleteRoom);
        verify(roomRepository).deleteRoom(idDeleteRoom);
    }

    @Test
    public void deleteRoom_failure_roomDoesNotExist_throwsException() {
        long idDeleteRoom = 3L;
        when(roomRepository.getRoomById(idDeleteRoom)).thenReturn(Optional.empty());
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                roomUseCase.deleteRoom(idDeleteRoom)
        );
        assertEquals("La habitación no existe", ex.getMessage());
    }

    @Test
    public void getRoomsByType_success_returnsListOfRoomDTO() {
        RoomType type = RoomType.SINGLE;
        Room room1 = new Room();
        room1.room_number = 101;
        room1.type = type;
        room1.price_per_night = new BigDecimal("100.00");
        room1.status = RoomStatus.AVAILABLE;

        Room room2 = new Room();
        room2.room_number = 102;
        room2.type = type;
        room2.price_per_night = new BigDecimal("150.00");
        room2.status = RoomStatus.OCCUPIED;

        List<Room> rooms = Arrays.asList(room1, room2);
        when(roomRepository.getRoomsByType(type)).thenReturn(rooms);

        List<RoomDTO> dtos = roomUseCase.getRoomsByType(type);
        assertEquals(2, dtos.size());

        // Verificamos el mapeo del primer objeto
        RoomDTO dto1 = dtos.getFirst();
        assertEquals(room1.room_number, dto1.room_number);
        assertEquals(room1.type, dto1.type);
        assertEquals(room1.price_per_night, dto1.price_per_night);
        assertEquals(room1.status, dto1.status);

        // Verificamos el mapeo del segundo objeto
        RoomDTO dto2 = dtos.get(1);
        assertEquals(room2.room_number, dto2.room_number);
        assertEquals(room2.type, dto2.type);
        assertEquals(room2.price_per_night, dto2.price_per_night);
        assertEquals(room2.status, dto2.status);
    }

    @Test
    public void getAvailableRooms_success() {
        Room room1 = new Room();
        room1.room_number = 101;
        room1.type = RoomType.SINGLE;
        room1.price_per_night = new BigDecimal("100.00");
        room1.status = RoomStatus.AVAILABLE;

        Room room2 = new Room();
        room2.room_number = 102;
        room2.type = RoomType.DOUBLE;
        room2.price_per_night = new BigDecimal("150.00");
        room2.status = RoomStatus.AVAILABLE;

        List<Room> rooms = Arrays.asList(room1, room2);
        when(roomRepository.getAvailableRooms()).thenReturn(rooms);

        List<RoomDTO> dtos = roomUseCase.getAvailableRooms();
        assertEquals(2, dtos.size());

        // Verificamos el mapeo para el primer objeto
        RoomDTO dto1 = dtos.getFirst();
        assertEquals(room1.room_number, dto1.room_number);
        assertEquals(room1.type, dto1.type);
        assertEquals(room1.price_per_night, dto1.price_per_night);
        assertEquals(room1.status, dto1.status);

        // Verificamos el mapeo para el segundo objeto
        RoomDTO dto2 = dtos.get(1);
        assertEquals(room2.room_number, dto2.room_number);
        assertEquals(room2.type, dto2.type);
        assertEquals(room2.price_per_night, dto2.price_per_night);
        assertEquals(room2.status, dto2.status);
    }

    @Test
    public void updateStatus_success_callsRepositoryAndReturnsResult() {
        long id = 4L;
        RoomStatus newStatus = RoomStatus.MAINTENANCE;
        when(roomRepository.updateStatus(id, newStatus)).thenReturn(1);
        int result = roomUseCase.updateStatus(id, newStatus);
        assertEquals(1, result);
        verify(roomRepository).updateStatus(id, newStatus);
    }

    @Test
    public void getRoomsInMaintenance_success_returnsListOfRooms() {
        Room room1 = new Room();
        Room room2 = new Room();
        List<Room> rooms = Arrays.asList(room1, room2);
        when(roomRepository.getRoomsInMaintenance()).thenReturn(rooms);

        List<Room> result = roomUseCase.getRoomsInMaintenance();
        assertEquals(2, result.size());
        assertEquals(rooms, result);
        verify(roomRepository).getRoomsInMaintenance();
    }
}