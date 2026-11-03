package de.htw.studyplanner.backend.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.htw.studyplanner.backend.model.Einladungscode;

/**
 * Zugriff auf die Tabelle {@code einladungscode}.
 */
public interface EinladungscodeRepository extends JpaRepository<Einladungscode, String> {

    /**
     * Lädt einen Code und sperrt die Zeile bis zum Ende der Transaktion
     * ({@code SELECT ... FOR UPDATE}).
     *
     * <p>So können zwei gleichzeitige Registrierungen mit demselben Code nicht beide
     * durchkommen: Die zweite wartet, bis die erste fertig ist, und sieht dann den
     * schon eingelösten Code.
     *
     * @param code der Code
     * @return der gesperrte Code, falls es ihn gibt
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Einladungscode e WHERE e.code = :code")
    Optional<Einladungscode> findByCodeForUpdate(@Param("code") String code);
}