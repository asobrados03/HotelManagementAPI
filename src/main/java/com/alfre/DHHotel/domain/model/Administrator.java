package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Administrator {
    public long id;
    public long user_id; // FK a la tabla users
    public String name;
}