package de.htw.studyplanner.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startklasse des Studyplanner-Backends.
 */
@SpringBootApplication
public class BackendApplication {

	/**
	 * Startet die Spring-Boot-Anwendung.
	 *
	 * @param args Startargumente
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
