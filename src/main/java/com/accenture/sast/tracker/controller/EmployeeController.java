package com.accenture.sast.tracker.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
@PreAuthorize("isAuthenticated()")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody CreateEmployeeRequest request) {
        Employee employee = new Employee(
                request.name(),
                request.role(),
                request.email(),
                request.onboardingDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(employee));
    }

    @GetMapping
    public List<Employee> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // The search endpoint is wired to the vulnerable EmployeeService.search()
    @GetMapping("/search")
    public List<Employee> search(@RequestParam String name) {
        return service.search(name);
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')") // stricter rule for sensitive profile endpoint
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long id) {
        Optional<Employee> opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Employee e = opt.get();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", e.getId());
        body.put("name", e.getName());
        body.put("salary", e.getSalary());
        body.put("nationalId", e.getNationalId());
        return ResponseEntity.ok(body);
    }

    // New profile update endpoint
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employee> updateProfile(
            @PathVariable Long id,
            @RequestBody ProfileUpdateRequest request) {
        if (request.salary() != null && request.salary().signum() < 0) {
            return ResponseEntity.badRequest().build();
        }

        return service.findById(id)
                .map(employee -> {
                    employee.setSalary(request.salary());
                    employee.setNationalId(request.nationalId());
                    return ResponseEntity.ok(service.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateEmployeeRequest(
            String name,
            String role,
            String email,
            LocalDate onboardingDate) {}

    public record ProfileUpdateRequest(BigDecimal salary, String nationalId) {}
}
