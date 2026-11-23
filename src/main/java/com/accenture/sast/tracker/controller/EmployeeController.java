package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.service.AuthService;
import com.accenture.sast.tracker.service.AuthService.ProfileAccess;
import com.accenture.sast.tracker.service.EmployeeService;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;
    private final AuthService authService;

    public EmployeeController(EmployeeService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@RequestBody CreateEmployeeRequest request) {
        Employee saved = service.save(new Employee(
                request.name(), request.role(), request.email(), request.onboardingDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeResponse.from(saved));
    }

    @GetMapping
    public List<EmployeeResponse> list() {
        return service.findAll().stream().map(EmployeeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(EmployeeResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<EmployeeResponse> search(@RequestParam String name) {
        return service.search(name).stream().map(EmployeeResponse::from).toList();
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<EmployeeProfile> getProfile(@PathVariable Long id) {
        return service.findById(id)
                .map(employee -> new EmployeeProfile(employee.getId(), employee.getName()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/private-profile")
    public ResponseEntity<PrivateEmployeeProfile> getPrivateProfile(@PathVariable Long id, Principal caller) {
        if (caller == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ProfileAccess access = authService.profileAccess(caller.getName(), id);
        if (access == ProfileAccess.DENIED) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return service.findById(id)
                .map(employee -> new PrivateEmployeeProfile(employee.getId(), employee.getName(),
                        employee.getSalary(), access == ProfileAccess.HR ? employee.getNationalId() : null))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateEmployeeRequest(String name, String role, String email, LocalDate onboardingDate) {}

    public record EmployeeResponse(Long id, String name, String role, String email, LocalDate onboardingDate) {
        static EmployeeResponse from(Employee employee) {
            return new EmployeeResponse(employee.getId(), employee.getName(), employee.getRole(),
                    employee.getEmail(), employee.getOnboardingDate());
        }
    }

    public record EmployeeProfile(Long id, String name) {}

    public record PrivateEmployeeProfile(Long id, String name, BigDecimal salary,
                                         @JsonInclude(JsonInclude.Include.NON_NULL) String nationalId) {}
}
