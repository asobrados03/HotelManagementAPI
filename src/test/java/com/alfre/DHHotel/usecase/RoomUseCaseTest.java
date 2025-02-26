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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the rooms management operations business logic.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class RoomUseCaseTest {
    @Mock
    private RoomRepository roomRepository;

    private RoomUseCase roomUseCase;

    /**
     * Sets up the RoomUseCase instance with the injected roomRepository.
     */
    @BeforeEach
    public void setup() {
        roomUseCase = new RoomUseCase(roomRepository);
    }

    /**
     * Tests that getAllRooms() returns a list of rooms successfully.
     * It verifies that the returned list matches the repository result.
     */
    @Test
    public void getAllRooms_success() {
        // Arrange: Prepare a list of rooms
        List<Room> roomList = new ArrayList<>();
        Room room = new Room();
        // Configure room properties as needed
        roomList.add(room);
        when(roomRepository.getAllRooms()).thenReturn(roomList);

        // Act: Execute the method
        List<Room> result = roomUseCase.getAllRooms();

        // Assert: Verify the expected result and that the repository method was called
        assertEquals(roomList, result);
        verify(roomRepository).getAllRooms();
    }

    /**
     * Tests that getRoomById() returns the expected room when found.
     */
    @Test
    public void getRoomById_success() {
        // Arrange: Create a room and simulate its return for the given ID
        Room room = new Room();
        when(roomRepository.getRoomById(10L)).thenReturn(Optional.of(room));

        // Act
        Optional<Room> result = roomUseCase.getRoomById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(room, result.get());
        verify(roomRepository).getRoomById(10L);
    }

    /**
     * Tests that createRoom() returns the generated room ID when the price is valid.
     */
    @Test
    public void createRoom_success_validPrice_returnsRoomId() {
        // Arrange
        Room newRoom = new Room();
        newRoom.price_per_night = new BigDecimal("100.00");
        long expectedId = 10L;
        when(roomRepository.createRoom(newRoom)).thenReturn(expectedId);

        // Act
        long result = roomUseCase.createRoom(newRoom);

        // Assert
        assertEquals(expectedId, result);
        verify(roomRepository).createRoom(newRoom);
    }

    /**
     * Tests that createRoom() throws an exception when the room has an invalid price (zero or negative).
     */
    @Test
    public void createRoom_failure_invalidPrice_throwsException() {
        // Arrange
        Room newRoom = new Room();
        // Set price to 0 (or negative) after stripping zeros
        newRoom.price_per_night = BigDecimal.ZERO;

        // Act & Assert
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                roomUseCase.createRoom(newRoom)
        );
        assertEquals("El precio debe ser mayor que 0", ex.getMessage());
    }

    /**
     * Tests that updateRoom() returns the number of updated rows when the room exists.
     */
    @Test
    public void updateRoom_success_roomExists_returnsUpdatedRows() {
        // Arrange
        long id = 5L;
        Room updatedRoom = new Room();
        updatedRoom.room_number = 101; // Example field to update

        Room existingRoom = new Room();
        when(roomRepository.getRoomById(id)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.updateRoom(updatedRoom, id)).thenReturn(1);

        // Act
        int result = roomUseCase.updateRoom(updatedRoom, id);

        // Assert
        assertEquals(1, result);
        verify(roomRepository).getRoomById(id);
        verify(roomRepository).updateRoom(updatedRoom, id);
    }

    /**
     * Tests that updateRoom() throws an exception when the room does not exist.
     */
    @Test
    public void updateRoom_failure_roomDoesNotExist_throwsException() {
        // Arrange
        long id = 5L;
        Room updatedRoom = new Room();
        when(roomRepository.getRoomById(id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                roomUseCase.updateRoom(updatedRoom, id)
        );
        assertEquals("La habitación no existe", ex.getMessage());
    }

    /**
     * Tests that deleteRoom() returns the number of deleted rows when the room exists.
     */
    @Test
    public void deleteRoom_success_roomExists_returnsDeletedRows() {
        // Arrange
        long idDeleteRoom = 3L;
        Room room = new Room();
        when(roomRepository.getRoomById(idDeleteRoom)).thenReturn(Optional.of(room));
        when(roomRepository.deleteRoom(idDeleteRoom)).thenReturn(1);

        // Act
        int result = roomUseCase.deleteRoom(idDeleteRoom);

        // Assert
        assertEquals(1, result);
        verify(roomRepository).getRoomById(idDeleteRoom);
        verify(roomRepository).deleteRoom(idDeleteRoom);
    }

    /**
     * Tests that deleteRoom() throws an exception when the room does not exist.
     */
    @Test
    public void deleteRoom_failure_roomDoesNotExist_throwsException() {
        // Arrange
        long idDeleteRoom = 3L;
        when(roomRepository.getRoomById(idDeleteRoom)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                roomUseCase.deleteRoom(idDeleteRoom)
        );
        assertEquals("La habitación no existe", ex.getMessage());
    }

    /**
     * Tests that getRoomsByType() returns a list of RoomDTO objects correctly mapped from the rooms of a given type.
     */
    @Test
    public void getRoomsByType_success_returnsListOfRoomDTO() {
        // Arrange
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

        // Act
        List<RoomDTO> dtos = roomUseCase.getRoomsByType(type);

        // Assert
        assertEquals(2, dtos.size());

        // Verify mapping for the first object
        RoomDTO dto1 = dtos.getFirst();
        assertEquals(room1.room_number, dto1.room_number);
        assertEquals(room1.type, dto1.type);
        assertEquals(room1.price_per_night, dto1.price_per_night);
        assertEquals(room1.status, dto1.status);

        // Verify mapping for the second object
        RoomDTO dto2 = dtos.get(1);
        assertEquals(room2.room_number, dto2.room_number);
        assertEquals(room2.type, dto2.type);
        assertEquals(room2.price_per_night, dto2.price_per_night);
        assertEquals(room2.status, dto2.status);
    }

    /**
     * Tests that getAvailableRooms() returns a list of RoomDTO objects for available rooms.
     */
    @Test
    public void getAvailableRooms_success() {
        // Arrange
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

        // Act
        List<RoomDTO> dtos = roomUseCase.getAvailableRooms();

        // Assert
        assertEquals(2, dtos.size());

        // Verify mapping for the first object
        RoomDTO dto1 = dtos.getFirst();
        assertEquals(room1.room_number, dto1.room_number);
        assertEquals(room1.type, dto1.type);
        assertEquals(room1.price_per_night, dto1.price_per_night);
        assertEquals(room1.status, dto1.status);

        // Verify mapping for the second object
        RoomDTO dto2 = dtos.get(1);
        assertEquals(room2.room_number, dto2.room_number);
        assertEquals(room2.type, dto2.type);
        assertEquals(room2.price_per_night, dto2.price_per_night);
        assertEquals(room2.status, dto2.status);
    }

    /**
     * Tests that updateStatus() calls the repository to update the room's status and returns the result.
     */
    @Test
    public void updateStatus_success_callsRepositoryAndReturnsResult() {
        // Arrange
        long id = 4L;
        RoomStatus newStatus = RoomStatus.MAINTENANCE;
        when(roomRepository.updateStatus(id, newStatus)).thenReturn(1);

        // Act
        int result = roomUseCase.updateStatus(id, newStatus);

        // Assert
        assertEquals(1, result);
        verify(roomRepository).updateStatus(id, newStatus);
    }

    /**
     * Tests that getRoomsInMaintenance() returns the list of rooms that are in maintenance.
     */
    @Test
    public void getRoomsInMaintenance_success_returnsListOfRooms() {
        // Arrange
        Room room1 = new Room();
        Room room2 = new Room();
        List<Room> rooms = Arrays.asList(room1, room2);
        when(roomRepository.getRoomsInMaintenance()).thenReturn(rooms);

        // Act
        List<Room> result = roomUseCase.getRoomsInMaintenance();

        // Assert
        assertEquals(2, result.size());
        assertEquals(rooms, result);
        verify(roomRepository).getRoomsInMaintenance();
    }
}