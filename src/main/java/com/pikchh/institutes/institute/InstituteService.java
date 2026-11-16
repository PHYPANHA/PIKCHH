package com.pikchh.institutes.institute;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InstituteService {

    private final InstituteRepository repository;

    public InstituteService(InstituteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InstituteResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(InstituteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstituteResponse findById(UUID id) {
        return InstituteResponse.from(getInstitute(id));
    }

    public InstituteResponse create(InstituteRequest request) {
        String code = normalizeCode(request.code());
        if (repository.existsByCode(code)) {
            throw new InstituteConflictException(code);
        }

        Institute institute = new Institute(
                request.name().trim(),
                code,
                normalizeOptional(request.email()),
                normalizeOptional(request.phone()),
                normalizeOptional(request.website()),
                normalizeOptional(request.address())
        );
        return save(institute, code);
    }

    public InstituteResponse update(UUID id, InstituteRequest request) {
        Institute institute = getInstitute(id);
        String code = normalizeCode(request.code());
        if (repository.existsByCodeAndIdNot(code, id)) {
            throw new InstituteConflictException(code);
        }

        institute.update(
                request.name().trim(),
                code,
                normalizeOptional(request.email()),
                normalizeOptional(request.phone()),
                normalizeOptional(request.website()),
                normalizeOptional(request.address())
        );
        return save(institute, code);
    }

    public void delete(UUID id) {
        repository.delete(getInstitute(id));
    }

    private Institute getInstitute(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new InstituteNotFoundException(id));
    }

    private InstituteResponse save(Institute institute, String code) {
        try {
            return InstituteResponse.from(repository.saveAndFlush(institute));
        } catch (DataIntegrityViolationException exception) {
            throw new InstituteConflictException(code);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
