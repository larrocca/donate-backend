package com.donate.app.repository;

import com.donate.app.model.BloodRequest;
import com.donate.app.model.BloodRequestStatus;
import com.donate.app.model.DonorEnrollment;
import com.donate.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DonorEnrollmentRepository extends JpaRepository<DonorEnrollment, Long> {

    @Query("""
            SELECT de FROM DonorEnrollment de
            JOIN FETCH de.donor
            WHERE de.bloodRequest.id = :bloodRequestId
            ORDER BY de.createdAt ASC
            """)
    List<DonorEnrollment> findByBloodRequestIdWithDonor(@Param("bloodRequestId") Long bloodRequestId);

    Optional<DonorEnrollment> findByBloodRequestAndDonor(BloodRequest bloodRequest, User donor);

    boolean existsByBloodRequestIdAndDonorId(Long bloodRequestId, Long donorId);

    @Query("""
            SELECT CASE WHEN COUNT(de) > 0 THEN true ELSE false END
            FROM DonorEnrollment de
            WHERE de.donor.id = :donorId AND de.bloodRequest.status = :status
            """)
    boolean existsActiveEnrollmentByDonorId(
            @Param("donorId") Long donorId,
            @Param("status") BloodRequestStatus status
    );

    @Query("""
            SELECT de FROM DonorEnrollment de
            JOIN FETCH de.bloodRequest br
            JOIN FETCH br.provincia
            JOIN FETCH br.localidad
            JOIN FETCH br.creator
            WHERE de.donor.id = :donorId AND br.status = :status
            """)
    Optional<DonorEnrollment> findActiveByDonorId(
            @Param("donorId") Long donorId,
            @Param("status") BloodRequestStatus status
    );

}
