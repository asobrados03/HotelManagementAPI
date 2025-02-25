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

@ExtendWith(MockitoExtension.class)
public class RoomControllerTest {

    @Mock
    private RoomUseCase roomUseCase;

    @InjectMocks
    private RoomController roomController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();
    }

    @Test
    public void whenGetAllRooms_success_thenReturnsRoomList() throws Exception {
        // Preparar
        List<Room> rooms = Arrays.asList(new Room(), new Room());
        when(roomUseCase.getAllRooms()).thenReturn(rooms);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase, times(1)).getAllRooms();
    }

    @Test
    public void whenGetAllRooms_empty_thenReturnsNotFound() throws Exception {
        // Simulamos una lista vacía para provocar el error
        when(roomUseCase.getAllRooms()).thenReturn(emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones registrados en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getAllRooms();
    }

    @Test
    public void whenGetRoomById_success_thenReturnsRoom() throws Exception {
        // Preparar
        long roomId = 1L;
        Room room = new Room();
        when(roomUseCase.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/room/{id}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists())
                .andDo(print());

        verify(roomUseCase).getRoomById(roomId);
    }

    @Test
    public void whenGetRoomById_notFound_thenReturnsNotFound() throws Exception {
        // Preparar
        long roomId = 999L;
        // Simulamos que no se encuentra la habitación
        when(roomUseCase.getRoomById(roomId)).thenReturn(Optional.empty());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/room/{id}", roomId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La habitación solicitada no existe."))
                .andDo(print());

        verify(roomUseCase, times(1)).getRoomById(roomId);
    }

    @Test
    public void whenCreateRoom_success_thenReturnsCreatedRoom() throws Exception {
        // Preparar
        long roomId = 5L;
        when(roomUseCase.createRoom(any(Room.class))).thenReturn(roomId);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andDo(print());

        verify(roomUseCase).createRoom(any(Room.class));
    }

    @Test
    public void whenCreateRoom_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        when(roomUseCase.createRoom(any(Room.class)))
                .thenThrow(new RuntimeException("Error creando habitación"));

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error creando habitación"))
                .andDo(print());

        verify(roomUseCase).createRoom(any(Room.class));
    }

    @Test
    public void whenUpdateRoom_success_thenReturnsOk() throws Exception {
        // Preparar
        long roomId = 1L;
        when(roomUseCase.updateRoom(any(Room.class), eq(roomId))).thenReturn(1);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/admin/room/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());

        verify(roomUseCase).updateRoom(any(Room.class), eq(roomId));
    }

    @Test
    public void whenUpdateRoom_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        long roomId = 1L;
        when(roomUseCase.updateRoom(any(Room.class), eq(roomId))).thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/admin/room/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());

        verify(roomUseCase).updateRoom(any(Room.class), eq(roomId));
    }

    @Test
    public void whenUpdateRoom_exception_thenReturnsInternalServerError() throws Exception {
        // Preparar
        long roomId = 1L;
        when(roomUseCase.updateRoom(any(Room.class), eq(roomId)))
                .thenThrow(new RuntimeException("Error de servicio."));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/admin/room/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());

        verify(roomUseCase).updateRoom(any(Room.class), eq(roomId));
    }

    @Test
    public void whenDeleteRoom_success_thenReturnsOk() throws Exception {
        // Preparar
        long roomId = 1L;
        when(roomUseCase.deleteRoom(roomId)).thenReturn(1);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/room/{idDeleteRoom}", roomId))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("La habitación con id: " + roomId + " se ha eliminado correctamente"))
                .andDo(print());

        verify(roomUseCase).deleteRoom(roomId);
    }

    @Test
    public void whenDeleteRoom_failure_thenReturnsBadRequest() throws Exception {
        // Preparar
        long roomId = 999L;
        when(roomUseCase.deleteRoom(roomId)).thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/room/{idDeleteRoom}", roomId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido eliminar."))
                .andDo(print());

        verify(roomUseCase).deleteRoom(roomId);
    }

    @Test
    public void whenDeleteRoom_exception_thenReturnsInternalServerError() throws Exception {
        // Preparar
        long roomId = 1L;
        when(roomUseCase.deleteRoom(roomId)).thenThrow(new RuntimeException());

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/room/{idDeleteRoom}", roomId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());

        verify(roomUseCase).deleteRoom(roomId);
    }

    @Test
    public void whenGetRoomsByType_success_thenReturnsRoomDTOList() throws Exception {
        // Preparar
        RoomType type = RoomType.SINGLE;
        RoomDTO roomDTO1 = RoomDTO.builder().build();
        RoomDTO roomDTO2 = RoomDTO.builder().build();

        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO1, roomDTO2);
        when(roomUseCase.getRoomsByType(type)).thenReturn(roomDTOs);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/public/rooms/type/{type}", type)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase).getRoomsByType(type);
    }

    @Test
    public void whenGetRoomsByType_empty_thenReturnsNotFound() throws Exception {
        // Preparar
        RoomType type = RoomType.SINGLE;
        // Simulamos que no hay habitaciones del tipo solicitado
        when(roomUseCase.getRoomsByType(type)).thenReturn(emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/public/rooms/type/{type}", type)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones del tipo solicitado en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getRoomsByType(type);
    }

    @Test
    public void whenGetAvailableRooms_success_thenReturnsRoomDTOList() throws Exception {
        // Preparar
        RoomDTO roomDTO1 = RoomDTO.builder().build();
        RoomDTO roomDTO2 = RoomDTO.builder().build();

        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO1, roomDTO2);
        when(roomUseCase.getAvailableRooms()).thenReturn(roomDTOs);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/public/rooms/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase).getAvailableRooms();
    }

    @Test
    public void whenGetAvailableRooms_empty_thenReturnsNotFound() throws Exception {
        // Simulamos que no hay habitaciones disponibles
        when(roomUseCase.getAvailableRooms()).thenReturn(emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/public/rooms/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones disponibles en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getAvailableRooms();
    }

    @Test
    public void whenUpdateStatus_success_thenReturnsOk() throws Exception {
        // Preparar
        long roomId = 1L;
        RoomStatus status = RoomStatus.AVAILABLE; // se asume que existe en el enum
        when(roomUseCase.updateStatus(roomId, status)).thenReturn(1);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/admin/rooms/{id}/status/{status}", roomId, status))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());

        verify(roomUseCase).updateStatus(roomId, status);
    }

    @Test
    public void whenUpdateStatus_failure_thenReturnsInternalServerError() throws Exception {
        // Preparar
        long roomId = 1L;
        RoomStatus status = RoomStatus.AVAILABLE;
        when(roomUseCase.updateStatus(roomId, status)).thenReturn(0);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/admin/rooms/{id}/status/{status}", roomId, status))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."))
                .andDo(print());

        verify(roomUseCase).updateStatus(roomId, status);
    }

    @Test
    public void whenUpdateStatus_exception_thenReturnsInternalServerError() throws Exception {
        // Preparar
        long roomId = 1L;
        RoomStatus status = RoomStatus.AVAILABLE;
        when(roomUseCase.updateStatus(roomId, status))
                .thenThrow(new RuntimeException("Error de servicio."));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/admin/rooms/{id}/status/{status}", roomId, status))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."))
                .andDo(print());

        verify(roomUseCase).updateStatus(roomId, status);
    }


    @Test
    public void whenGetRoomsInMaintenance_success_thenReturnsRoomList() throws Exception {
        // Preparar
        List<Room> rooms = Arrays.asList(new Room(), new Room());
        when(roomUseCase.getRoomsInMaintenance()).thenReturn(rooms);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/rooms/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());

        verify(roomUseCase).getRoomsInMaintenance();
    }

    @Test
    public void whenGetRoomsInMaintenance_empty_thenReturnsNotFound() throws Exception {
        // Simulamos que no hay habitaciones en mantenimiento
        when(roomUseCase.getRoomsInMaintenance()).thenReturn(emptyList());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/rooms/maintenance"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones en mantenimiento en el sistema."))
                .andDo(print());

        verify(roomUseCase, times(1)).getRoomsInMaintenance();
    }
}