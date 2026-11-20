package com.donate.app.repository;

import com.donate.app.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByAuth0Id(String auth0Id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.auth0Id = :auth0Id")
    Optional<User> findByAuth0IdForUpdate(@Param("auth0Id") String auth0Id);

}
