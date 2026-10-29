package de.htw.studyplanner.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Stellt den {@link PasswordEncoder} für Passwort-Hashes bereit.
 */
@Configuration
public class PasswortConfig {

    /**
     * BCrypt-Encoder für Passwort-Hashes.
     *
     * @return ein neuer {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
