/**
 * Paket für die Datenzugriffsschicht des Backends.
 *
 * Dieses Paket enthält die Spring-Data-JPA-Repositories, die den Zugriff auf die
 * persistierten Entitäten kapseln. Die Interfaces hier stellen CRUD-Operationen und
 * fachlich sinnvolle Queries bereit, ohne dass der Rest der Anwendung selbst SQL
 * schreiben muss.
 *
 * Die Repository-Schicht trennt die fachliche Logik von der technischen Umsetzung
 * der Datenbankzugriffe. Sie ermöglicht strukturierte Suchfunktionen wie
 * {@code findByEmail(...)} oder {@code findByJahrAndArt(...)} und bildet damit die
 * zentrale Schnittstelle zwischen Domain-Model und Datenbank.
 */
package de.htw.studyplanner.backend.repository;
