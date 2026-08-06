package com.donate.app.repository;

import com.donate.app.model.BloodRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.BloodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    @Query("""
            SELECT br FROM BloodRequest br
            JOIN FETCH br.provincia
            JOIN FETCH br.localidad
            JOIN FETCH br.creator
            WHERE br.id = :id
            """)
    Optional<BloodRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT br FROM BloodRequest br
            JOIN FETCH br.provincia
            JOIN FETCH br.localidad
            JOIN FETCH br.creator
            WHERE br.creator.id = :creatorId
            ORDER BY br.createdAt DESC
            """)
    List<BloodRequest> findByCreatorIdWithDetails(@Param("creatorId") Long creatorId);

    @Query("""
            SELECT br FROM BloodRequest br
            JOIN FETCH br.provincia
            JOIN FETCH br.localidad
            JOIN FETCH br.creator
            WHERE br.status = :status
              AND br.bloodType IN :bloodTypes
              AND br.localidad.id IN :localidadIds
              AND br.creator.id <> :userId
            ORDER BY br.createdAt DESC
            """)
    List<BloodRequest> findForUserInterests(
            @Param("status") BloodRequestStatus status,
            @Param("bloodTypes") Collection<BloodType> bloodTypes,
            @Param("localidadIds") Collection<Long> localidadIds,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT br FROM BloodRequest br
            JOIN FETCH br.provincia
            JOIN FETCH br.localidad
            JOIN FETCH br.creator
            WHERE (:status IS NULL OR br.status = :status)
              AND (:provinciaId IS NULL OR br.provincia.id = :provinciaId)
              AND (:localidadIds IS NULL OR br.localidad.id IN :localidadIds)
              AND (:bloodType IS NULL OR br.bloodType = :bloodType)
            ORDER BY br.createdAt DESC
            """)
    List<BloodRequest> search(
            @Param("status") BloodRequestStatus status,
            @Param("provinciaId") Long provinciaId,
            @Param("localidadIds") List<Long> localidadIds,
            @Param("bloodType") BloodType bloodType
    );

}
