package de.htw.studyplanner.backend.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

@Component
public class StudierendenGeneratorService implements CommandLineRunner {

    // Erlaubte Noten laut HTW-System und Team-Absprache[cite: 227]
    private static final List<Double> ERLAUBTE_NOTEN = Arrays.asList(
        1.0, 1.3, 1.7, 2.0, 2.3, 2.7, 3.0, 3.3, 3.7, 4.0, 5.0
    );

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Starte Studierenden-Generator (Ticket E1.5) ---");

        // Fester Seed für reproduzierbare Ergebnisse (gleiche Eingabe -> gleiche Daten)[cite: 223]
        Random random = new Random(42);

        // Generierung von 5 Kohorten (Startsemester)[cite: 223]
        for (int kohorte = 1; kohorte <= 5; kohorte++) {
            
            // Zufällige Anzahl von Studierenden pro Kohorte (zwischen 30 und 60 Personen)[cite: 223]
            int anzahlStudierende = 30 + random.nextInt(31); 
            
            for (int i = 1; i <= anzahlStudierende; i++) {
                
                // 1. Nutzer-Daten vorbereiten
                // Synthetische E-Mail-Adressen generieren (keine echten Daten)[cite: 223]
                String email = "dummy_k" + kohorte + "_" + i + "@htw-berlin.de";
                
                // Zu speichernde Nutzer-Attribute (für das NutzerRepository):
                // email = email
                // passwort_hash = "hashed_pw_123"
                // rolle = "STUDENT"
                // is_synthetic = true (wichtig für die Erkennung von Dummy-Daten)[cite: 223]

                // 2. Studierenden-Daten verknüpfen
                // Zu speichernde Studierenden-Attribute (für das StudierendeRepository):
                // nutzer_id = (generierte ID aus Schritt 1)
                // startsemester_id = kohort

                // 3. Leistungen (Noten) generieren
                // Eine zufällige Note aus der Liste der erlaubten Noten auswählen
                double zufallsNote = ERLAUBTE_NOTEN.get(random.nextInt(ERLAUBTE_NOTEN.size()));
                
                // Status basierend auf der Note setzen (5.0 = NICHT_BESTANDEN)
                String status = (zufallsNote == 5.0) ? "NICHT_BESTANDEN" : "BESTANDEN";

                // Zu speichernde Leistungs-Attribute (für das LeistungRepository):
                // nutzer_id = (generierte ID)
                // modul_code = "B11" (als Beispiel)
                // status = status
                // note = zufallsNote
            }
            System.out.println("Kohorte " + kohorte + " generiert mit " + anzahlStudierende + " Studierenden.");
        }
        
        System.out.println("--- Studierenden-Generator abgeschlossen ---");
    }
}