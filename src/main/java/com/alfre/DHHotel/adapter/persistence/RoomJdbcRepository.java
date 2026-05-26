package com.alfre.DHHotel.adapter.persistence;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

/**
 * Backwards-compatible alias for {@link SqlRoomRepository}.
 */
@Repository
public class RoomJdbcRepository extends SqlRoomRepository {
    public RoomJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        super(namedParameterJdbcTemplate, dataSource);
    }
}
