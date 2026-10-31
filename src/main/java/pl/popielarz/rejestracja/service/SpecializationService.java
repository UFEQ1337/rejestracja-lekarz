package pl.popielarz.rejestracja.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.popielarz.rejestracja.dto.SpecializationDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.mapper.SpecializationMapper;
import pl.popielarz.rejestracja.repository.SpecializationRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final SpecializationMapper specializationMapper;

    public SpecializationService(SpecializationRepository specializationRepository,
                                 SpecializationMapper specializationMapper) {
        this.specializationRepository = specializationRepository;
        this.specializationMapper = specializationMapper;
    }

    public List<SpecializationDto.Response> findAll() {
        return specializationRepository.findAllByOrderByNameAsc().stream()
                .map(specializationMapper::toResponse)
                .toList();
    }

    @Transactional
    public SpecializationDto.Response create(SpecializationDto.Request request) {
        if (specializationRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Specjalizacja '" + request.name() + "' już istnieje");
        }
        return specializationMapper.toResponse(
                specializationRepository.save(specializationMapper.toEntity(request)));
    }
}
