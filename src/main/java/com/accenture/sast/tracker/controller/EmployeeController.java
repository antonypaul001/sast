package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @GetMapping("/{id}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long id) {
        Optional<Employee> opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Employee e = opt.get();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        // presumption that the employee email is the user's login username for auth
        boolean isOwner = e.getEmail().equalsIgnoreCase(auth.getName());

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", e.getId());
        body.put("name", e.getName());
        body.put("salary", e.getSalary());
        body.put("nationalId", e.getNationalId());
        return ResponseEntity.ok(body);
    }
}
