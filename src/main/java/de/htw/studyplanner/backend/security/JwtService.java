package de.htw.studyplanner.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.htw.studyplanner.backend.model.Nutzende;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Erstellt und prüft die Tokens (JWT) für die Anmeldung.
 *
 * <p>Die Tokens werden mit einem geheimen Schlüssel aus der Konfiguration ({@code jwt.secret})
 * signiert. Wer den Schlüssel nicht kennt, kann kein gültiges Token herstellen.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    /**
     * Erstellt den Service.
     *
     * @param secret            geheimer Schlüssel zum Signieren, mindestens 32 Zeichen
     * @param expirationSeconds Gültigkeit eines Tokens in Sekunden (Standard 3600)
     * @throws IllegalArgumentException wenn der Schlüssel kürzer als 32 Zeichen ist
     */
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {

        if (secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET muss mindestens 32 Zeichen lang sein");
        }

        this.signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Stellt ein Token für die angegebene Person aus.
     *
     * <p>Im Token stehen die ID (Subject), die E-Mail ({@code email}) und die Rolle
     * ({@code rolle}) sowie Ausstellungs- und Ablaufzeit.
     *
     * @param nutzende die angemeldete Person
     * @return das signierte Token als Text
     */
    public String generateToken(Nutzende nutzende) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(nutzende.getNutzendeId().toString())
                .claim("email", nutzende.getEmail())
                .claim("rolle", nutzende.getRolle().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Prüft die Signatur und liest den Inhalt des Tokens.
     *
     * @param token das Token als Text
     * @return die Angaben aus dem Token
     * @throws io.jsonwebtoken.JwtException wenn das Token ungültig oder abgelaufen ist
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Prüft, ob ein Token gültig ist.
     *
     * @param token das Token als Text
     * @return true bei gültiger Signatur und nicht abgelaufen, sonst false
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Liefert die Gültigkeit eines Tokens in Sekunden.
     *
     * @return Gültigkeit in Sekunden
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}