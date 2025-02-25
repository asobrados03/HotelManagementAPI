package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    public long id;
    public long user_id; // FK a la tabla users
    public String first_name;
    public String last_name;
    public String phone;
}