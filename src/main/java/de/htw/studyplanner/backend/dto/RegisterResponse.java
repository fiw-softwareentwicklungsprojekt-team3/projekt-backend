package de.htw.studyplanner.backend.dto;

public record RegisterResponse(
        Integer id,
        String email,
        String rolle,
        String startsemester
) {
}
