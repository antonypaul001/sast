package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody Employee employee) {
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

    // ── VULNERABILITY V7: Insecure Direct Object Reference (CWE-639) ─────────
    // Any caller can retrieve any employee's salary and national ID by supplying
    // an employee ID. There is no check that the requester is authorised to view
    // that specific employee's sensitive data. Incrementing the id exposes every
    // record in the system.
    // Fix: extract the caller's identity from a session or bearer token and verify
    // that the requesting user either owns the record or holds an admin role before
    // returning sensitive fields.
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long id,
            Authentication authentication) {

        /*
         * Spring Security populates Authentication from the authenticated
         * HTTP session or from a validated bearer token.
         */
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Authentication is required"
                    ));
        }

        // Check if the user has the "ADMIN" role
        if (!authentication.getAuthorities().stream()
                .anyMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Access denied"
                    ));
        }

        Optional<Employee> opt = service.findById(id);
        
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Employee e = opt.get();
        String callerUsername = authentication.getName();

        boolean isOwner =
                callerUsername.equals(e.getName()); // Assuming username is stored in the name field

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isOwner && !isAdmin) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You are not authorised to view this profile"
                    ));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", e.getId());
        body.put("name", e.getName());
        body.put("salary", e.getSalary());
        body.put("nationalId", e.getNationalId());

        return ResponseEntity.ok(body);
    }
}
