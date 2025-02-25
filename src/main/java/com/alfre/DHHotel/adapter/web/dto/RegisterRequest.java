package com.alfre.DHHotel.adapter.web.dto;

import com.alfre.DHHotel.domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    String username; // email
    String password;
    String name;
    String first_name;
    String last_name;
    String phone;
    Role role;
}
