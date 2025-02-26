package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.RoomController;
import com.alfre.DHHotel.adapter.web.dto.RoomDTO;
import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import com.alfre.DHHotel.usecase.RoomUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the rooms management operations controller.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class RoomControllerTest {

    @Mock
    private RoomUseCase roomUseCase;

    @InjectMocks
    private RoomController roomController;

    private MockMvc mockMvc;

    /**
     * Sets up the MockMvc instance for testing the RoomController.
     */
    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();
    }

    /**
     * Tests that when all rooms are successfully retrieved,
     * the API returns a JSON list containing the expected number of rooms.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetAllRooms_success_thenReturnsRoomList() throws Exception {
        // Arrange: Prepare a list of two rooms
        List<Room> rooms = Arrays.asList(new Room(), new Room());
        when(roomUseCase.getAllRooms()).thenReturn(rooms);

        // Act & Assert: Perform GET and verify response details
        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase, times(1)).getAllRooms();
    }

    /**
     * Tests that when no rooms are found,
     * the API returns a 404 Not Found response with an appropriate message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetAllRooms_empty_thenReturnsNotFound() throws Exception {
        // Arrange: Simulate an empty room list
        when(roomUseCase.getAllRooms()).thenReturn(emptyList());

        // Act & Assert: Perform GET and verify that the response indicates not found
        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones registrados en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getAllRooms();
    }

    /**
     * Tests that when a room is successfully retrieved by its ID,
     * the API returns the corresponding room details.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetRoomById_success_thenReturnsRoom() throws Exception {
        // Arrange: Set up a room with a specific ID
        long roomId = 1L;
        Room room = new Room();
        when(roomUseCase.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert: Perform GET with the room ID and verify the room is returned
        mockMvc.perform(get("/api/admin/room/{id}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andDo(print());

        verify(roomUseCase).getRoomById(roomId);
    }

    /**
     * Tests that when a room is not found by its ID,
     * the API returns a 404 Not Found response with an appropriate message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetRoomById_notFound_thenReturnsNotFound() throws Exception {
        // Arrange: Set up a scenario where the room is not found
        long roomId = 999L;
        when(roomUseCase.getRoomById(roomId)).thenReturn(Optional.empty());

        // Act & Assert: Perform GET with the room ID and verify a not found response
        mockMvc.perform(get("/api/admin/room/{id}", roomId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La habitación solicitada no existe."))
                .andDo(print());

        verify(roomUseCase, times(1)).getRoomById(roomId);
    }

    /**
     * Tests that when a room is successfully created,
     * the API returns the created room's ID.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenCreateRoom_success_thenReturnsCreatedRoom() throws Exception {
        // Arrange: Simulate creation by returning a room ID
        long roomId = 5L;
        when(roomUseCase.createRoom(any(Room.class))).thenReturn(roomId);

        // Act & Assert: Perform POST and verify successful creation
        mockMvc.perform(post("/api/admin/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(roomUseCase).createRoom(any(Room.class));
    }

    /**
     * Tests that when room creation fails,
     * the API returns a 400 Bad Request response with the error message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenCreateRoom_failure_thenReturnsBadRequest() throws Exception {
        // Arrange: Simulate an exception during room creation
        when(roomUseCase.createRoom(any(Room.class)))
                .thenThrow(new RuntimeException("Error creando habitación"));

        // Act & Assert: Perform POST and verify the error response
        mockMvc.perform(post("/api/admin/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error creando habitación"))
                .andDo(print());

        verify(roomUseCase).createRoom(any(Room.class));
    }

    /**
     * Tests that when a room is successfully updated,
     * the API returns a 200 OK response with a success message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenUpdateRoom_success_thenReturnsOk() throws Exception {
        // Arrange: Set up the room ID and simulate a successful update (returns 1 row updated)
        long roomId = 1L;
        when(roomUseCase.updateRoom(any(Room.class), eq(roomId))).thenReturn(1);

        // Act & Assert: Perform PUT and verify the successful update response
        mockMvc.perform(put("/api/admin/room/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());

        verify(roomUseCase).updateRoom(any(Room.class), eq(roomId));
    }

    /**
     * Tests that when a room update fails (e.g. returns 0 rows updated),
     * the API returns a 400 Bad Request response with an error message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenUpdateRoom_failure_thenReturnsBadRequest() throws Exception {
        // Arrange: Simulate update failure by returning 0 rows updated
        long roomId = 1L;
        when(roomUseCase.updateRoom(any(Room.class), eq(roomId))).thenReturn(0);

        // Act & Assert: Perform PUT and verify the failure response
        mockMvc.perform(put("/api/admin/room/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());

        verify(roomUseCase).updateRoom(any(Room.class), eq(roomId));
    }

    /**
     * Tests that when an exception occurs during room update,
     * the API returns a 500 Internal Server Error response with the error message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenUpdateRoom_exception_thenReturnsInternalServerError() throws Exception {
        // Arrange: Simulate an exception during update
        long roomId = 1L;
        when(roomUseCase.updateRoom(any(Room.class), eq(roomId)))
                .thenThrow(new RuntimeException("Error de servicio."));

        // Act & Assert: Perform PUT and verify the internal server error response
        mockMvc.perform(put("/api/admin/room/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());

        verify(roomUseCase).updateRoom(any(Room.class), eq(roomId));
    }

    /**
     * Tests that when a room is successfully deleted,
     * the API returns a 200 OK response with a confirmation message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenDeleteRoom_success_thenReturnsOk() throws Exception {
        // Arrange: Simulate successful deletion returning 1
        long roomId = 1L;
        when(roomUseCase.deleteRoom(roomId)).thenReturn(1);

        // Act & Assert: Perform DELETE and verify the success message
        mockMvc.perform(delete("/api/admin/room/{idDeleteRoom}", roomId))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("La habitación con id: " + roomId + " se ha eliminado correctamente"))
                .andDo(print());

        verify(roomUseCase).deleteRoom(roomId);
    }

    /**
     * Tests that when room deletion fails,
     * the API returns a 400 Bad Request response with an error message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenDeleteRoom_failure_thenReturnsBadRequest() throws Exception {
        // Arrange: Simulate deletion failure by returning 0
        long roomId = 999L;
        when(roomUseCase.deleteRoom(roomId)).thenReturn(0);

        // Act & Assert: Perform DELETE and verify the error response
        mockMvc.perform(delete("/api/admin/room/{idDeleteRoom}", roomId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido eliminar."))
                .andDo(print());

        verify(roomUseCase).deleteRoom(roomId);
    }

    /**
     * Tests that when an exception occurs during room deletion,
     * the API returns a 500 Internal Server Error response with an error message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenDeleteRoom_exception_thenReturnsInternalServerError() throws Exception {
        // Arrange: Simulate an exception during deletion
        long roomId = 1L;
        when(roomUseCase.deleteRoom(roomId)).thenThrow(new RuntimeException());

        // Act & Assert: Perform DELETE and verify the internal server error response
        mockMvc.perform(delete("/api/admin/room/{idDeleteRoom}", roomId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());

        verify(roomUseCase).deleteRoom(roomId);
    }

    /**
     * Tests that when rooms are retrieved by type successfully,
     * the API returns a list of RoomDTOs of the specified type.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetRoomsByType_success_thenReturnsRoomDTOList() throws Exception {
        // Arrange: Prepare a list of RoomDTOs for a given type
        RoomType type = RoomType.SINGLE;
        RoomDTO roomDTO1 = RoomDTO.builder().build();
        RoomDTO roomDTO2 = RoomDTO.builder().build();

        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO1, roomDTO2);
        when(roomUseCase.getRoomsByType(type)).thenReturn(roomDTOs);

        // Act & Assert: Perform GET and verify that the list is returned
        mockMvc.perform(get("/api/public/rooms/type/{type}", type)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase).getRoomsByType(type);
    }

    /**
     * Tests that when no rooms of a given type are found,
     * the API returns a 404 Not Found response with an appropriate message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetRoomsByType_empty_thenReturnsNotFound() throws Exception {
        // Arrange: Simulate no rooms found for the given type
        RoomType type = RoomType.SINGLE;
        when(roomUseCase.getRoomsByType(type)).thenReturn(emptyList());

        // Act & Assert: Perform GET and verify the not found response
        mockMvc.perform(get("/api/public/rooms/type/{type}", type)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones del tipo solicitado en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getRoomsByType(type);
    }

    /**
     * Tests that when available rooms are retrieved successfully,
     * the API returns a list of available RoomDTOs.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetAvailableRooms_success_thenReturnsRoomDTOList() throws Exception {
        // Arrange: Prepare a list of available RoomDTOs
        RoomDTO roomDTO1 = RoomDTO.builder().build();
        RoomDTO roomDTO2 = RoomDTO.builder().build();

        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO1, roomDTO2);
        when(roomUseCase.getAvailableRooms()).thenReturn(roomDTOs);

        // Act & Assert: Perform GET and verify the returned list
        mockMvc.perform(get("/api/public/rooms/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase).getAvailableRooms();
    }

    /**
     * Tests that when no available rooms are found,
     * the API returns a 404 Not Found response with an appropriate message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetAvailableRooms_empty_thenReturnsNotFound() throws Exception {
        // Arrange: Simulate an empty list of available rooms
        when(roomUseCase.getAvailableRooms()).thenReturn(emptyList());

        // Act & Assert: Perform GET and verify the not found response
        mockMvc.perform(get("/api/public/rooms/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones disponibles en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getAvailableRooms();
    }

    /**
     * Tests that when the room status is successfully updated,
     * the API returns a 200 OK response with a success message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenUpdateStatus_success_thenReturnsOk() throws Exception {
        // Arrange: Set the room ID and desired status, and simulate a successful update (1 row updated)
        long roomId = 1L;
        RoomStatus status = RoomStatus.AVAILABLE;
        when(roomUseCase.updateStatus(roomId, status)).thenReturn(1);

        // Act & Assert: Perform PUT and verify the successful update response
        mockMvc.perform(put("/api/admin/rooms/{id}/status/{status}", roomId, status))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());

        verify(roomUseCase).updateStatus(roomId, status);
    }

    /**
     * Tests that when updating the room status fails (i.e., 0 rows updated),
     * the API returns a 500 Internal Server Error response with an error message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenUpdateStatus_failure_thenReturnsInternalServerError() throws Exception {
        // Arrange: Simulate update failure by returning 0 rows updated
        long roomId = 1L;
        RoomStatus status = RoomStatus.AVAILABLE;
        when(roomUseCase.updateStatus(roomId, status)).thenReturn(0);

        // Act & Assert: Perform PUT and verify the error response
        mockMvc.perform(put("/api/admin/rooms/{id}/status/{status}", roomId, status))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());

        verify(roomUseCase).updateStatus(roomId, status);
    }

    /**
     * Tests that when an exception occurs while updating the room status,
     * the API returns a 500 Internal Server Error response with the exception message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenUpdateStatus_exception_thenReturnsInternalServerError() throws Exception {
        // Arrange: Simulate an exception during the status update
        long roomId = 1L;
        RoomStatus status = RoomStatus.AVAILABLE;
        when(roomUseCase.updateStatus(roomId, status))
                .thenThrow(new RuntimeException("Error de servicio."));

        // Act & Assert: Perform PUT and verify the exception response
        mockMvc.perform(put("/api/admin/rooms/{id}/status/{status}", roomId, status))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());

        verify(roomUseCase).updateStatus(roomId, status);
    }

    /**
     * Tests that when rooms in maintenance are successfully retrieved,
     * the API returns a list of rooms under maintenance.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetRoomsInMaintenance_success_thenReturnsRoomList() throws Exception {
        // Arrange: Prepare a list of rooms in maintenance
        List<Room> rooms = Arrays.asList(new Room(), new Room());
        when(roomUseCase.getRoomsInMaintenance()).thenReturn(rooms);

        // Act & Assert: Perform GET and verify the list is returned
        mockMvc.perform(get("/api/admin/rooms/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase).getRoomsInMaintenance();
    }

    /**
     * Tests that when no rooms in maintenance are found,
     * the API returns a 404 Not Found response with an appropriate message.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test
    public void whenGetRoomsInMaintenance_empty_thenReturnsNotFound() throws Exception {
        // Arrange: Simulate an empty list of rooms in maintenance
        when(roomUseCase.getRoomsInMaintenance()).thenReturn(emptyList());

        // Act & Assert: Perform GET and verify the not found response
        mockMvc.perform(get("/api/admin/rooms/maintenance"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones en mantenimiento en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getRoomsInMaintenance();
    }
}