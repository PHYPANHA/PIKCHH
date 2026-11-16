package com.pikchh.institutes.institute;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstituteRepository extends JpaRepository<Institute, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Institute> findAllByOrderByNameAsc();
}
