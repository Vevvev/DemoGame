package io.subnoize.fatdaddygames.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.subnoize.fatdaddygames.model.PlayerAccount;

public interface PlayerAccountRepository extends JpaRepository<PlayerAccount, Long> {

}
