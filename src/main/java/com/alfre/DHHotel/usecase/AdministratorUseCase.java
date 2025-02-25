package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdministratorUseCase {
    private final AdministratorRepository administratorRepository;
    private final UserRepository userRepository;

    public AdministratorUseCase(AdministratorRepository administratorRepository, UserRepository userRepository) {
        this.administratorRepository = administratorRepository;
        this.userRepository = userRepository;
    }

    public List<Administrator> getAllAdministrators() {
        return administratorRepository.getAllAdministrators();
    }

    public Optional<Administrator> getAdministratorByUserId(long userId) {
        return administratorRepository.getAdministratorByUserId(userId);
    }

    public Optional<Administrator> getAdministratorById(long id) {
        return administratorRepository.getAdministratorById(id);
    }

    public int updateAdministrator(Administrator administrator, long userId) {
        return administratorRepository.updateAdministrator(administrator, userId);
    }

    public int deleteAdministrator(long id) {
        Administrator admin = administratorRepository.getAdministratorById(id)
                .orElseThrow(() -> new RuntimeException("No existe el administrador que quieres eliminar"));

        return userRepository.deleteUser(admin.user_id);
    }
}