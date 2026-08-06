package com.donate.app.repository;

import com.donate.app.model.Localidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    List<Localidad> findByProvinciaIdOrderByNombreAsc(Long provinciaId);

    List<Localidad> findByIdInAndProvinciaId(Collection<Long> ids, Long provinciaId);

}
