package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing room data.
 * Defines CRUD operations for rooms in the system.
 *
 * <p>This interface should be implemented by a class that interacts with the database.</p>
 *
 * @author Alfredo
 */
public interface RoomRepository {

    /**
     * Retrieves a list of all rooms in the system.
     *
     * @return a list of {@link Room} objects.
     */
    List<Room> getAllRooms();

    /**
     * Retrieves a room based on its unique ID.
     *
     * @param id the unique identifier of the room.
     * @return an {@code Optional} containing the room if found, otherwise empty.
     */
    Optional<Room> getRoomById(long id);

    /**
     * Creates a new room in the system.
     *
     * @param newRoom the {@link Room} object to be added.
     * @return the generated unique identifier of the newly created room.
     */
    long createRoom(Room newRoom);

    /**
     * Updates the details of an existing room.
     *
     * @param room the room object containing updated information.
     * @param id the unique identifier of the room to be updated.
     * @return the number of rows affected in the database.
     */
    int updateRoom(Room room, long id);

    /**
     * Deletes a room from the system based on its unique ID.
     *
     * @param id the unique identifier of the room to be deleted.
     * @return the number of rows affected in the database.
     */
    int deleteRoom(long id);

    /**
     * Retrieves a list of rooms of a specific type.
     *
     * @param type the type of room (e.g., SINGLE, DOUBLE, SUITE).
     * @return a list of {@link Room} objects matching the given type.
     */
    List<Room> getRoomsByType(RoomType type);

    /**
     * Retrieves a list of available rooms in the system.
     *
     * @return a list of {@link Room} objects that are available for booking.
     */
    List<Room> getAvailableRooms();

    /**
     * Updates the status of a room (e.g., AVAILABLE, OCCUPIED, MAINTENANCE).
     *
     * @param id the unique identifier of the room.
     * @param status the new status of the room.
     * @return the number of rows affected in the database.
     */
    int updateStatus(long id, RoomStatus status);

    /**
     * Retrieves a list of rooms that are currently under maintenance.
     *
     * @return a list of {@link Room} objects with a status of MAINTENANCE.
     */
    List<Room> getRoomsInMaintenance();

    /**
     * Deletes all room records from the system.
     * <p><b>Warning:</b> This action is irreversible.</p>
     */
    void deleteAll();
}
