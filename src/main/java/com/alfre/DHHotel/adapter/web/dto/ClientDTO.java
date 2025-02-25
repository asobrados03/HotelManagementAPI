package com.alfre.DHHotel.adapter.web.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ClientDTO {
    public String email;
    public String firstName;
    public String lastName;
    public String phone;
}
