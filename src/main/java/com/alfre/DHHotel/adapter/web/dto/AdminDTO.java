package com.alfre.DHHotel.adapter.web.dto;

import com.alfre.DHHotel.domain.model.Role;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AdminDTO {
    public String email;
    public Role role;
    public String name;
}
