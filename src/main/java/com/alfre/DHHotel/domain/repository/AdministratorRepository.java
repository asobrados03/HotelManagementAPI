package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Administrator;

import java.util.List;
import java.util.Optional;

public interface AdministratorRepository {
    List<Administrator> getAllAdministrators();
    Optional<Administrator> getAdministratorByUserId(String userId);
    Optional<Administrator> getAdministratorById(long id);
    void createAdministrator(Administrator newAdministrator);
    int updateAdministrator(Administrator administrator, long userId);
    Optional<Administrator> getAdministratorByUserId(long userId);
    void deleteAll();
}
