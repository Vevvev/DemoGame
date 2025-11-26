package io.subnoize.fatdaddygames.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.subnoize.fatdaddygames.model.PlayerAccount;
import io.subnoize.fatdaddygames.repository.PlayerAccountRepository;

@Service
public class PlayerAccountService {

	@Autowired
	private PlayerAccountRepository accountRepo;
	
	@Transactional
	public void savePlayer(String playerName) {
		accountRepo.save(new PlayerAccount(playerName));
	}
	
	@Transactional
	public List<PlayerAccount> loadPlayers() {
		return accountRepo.findAll();
	}
}
