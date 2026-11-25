package se.sundsvall.caremanagement.usersettings.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.usersettings.integration.db.model.UserSettingsEntity;

@Transactional
@CircuitBreaker(name = "userSettingsRepository")
public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, Long> {

	Optional<UserSettingsEntity> findByMunicipalityIdAndAdAccount(String municipalityId, String adAccount);

	void deleteByMunicipalityIdAndAdAccount(String municipalityId, String adAccount);
}
