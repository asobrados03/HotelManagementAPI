package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    List<Room> getAllRooms();
    Optional<Room> getRoomById(long id);
    long createRoom(Room newRoom);
    int updateRoom(Room room, long id);
    int deleteRoom(long id);
    List<Room> getRoomsByType(RoomType type);
    List<Room> getAvailableRooms();
    int updateStatus(long id, RoomStatus status);
    List<Room> getRoomsInMaintenance();
    void deleteAll();
}
