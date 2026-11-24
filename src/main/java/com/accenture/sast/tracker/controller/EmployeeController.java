package com.accenture.sast.tracker.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.model.ProfileUpdateRequest;
import com.accenture.sast.tracker.service.EmployeeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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

    @GetMapping("/{id}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @PathVariable Long id,
            HttpServletRequest request) {

        ResponseEntity<Void> authorizationError = authorizeProfileAccess(request, id);

        if (authorizationError != null) {
            return ResponseEntity.status(authorizationError.getStatusCode()).build();
        }

        Optional<Employee> opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Employee e = opt.get();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", e.getName());
        body.put("salary", e.getSalary());
        body.put("nationalId", e.getNationalId());
        return ResponseEntity.ok(body);
    }

    // New profile update endpoint
    @PutMapping("/{id}/profile")
    public ResponseEntity<Employee> updateProfile(
            @PathVariable Long id,
            @RequestBody ProfileUpdateRequest updateRequest,
            HttpServletRequest request) {
        ResponseEntity<Void> authorizationError = authorizeProfileAccess(request, id);

        if (authorizationError != null) {
            return ResponseEntity.status(authorizationError.getStatusCode()).build();
        }

        if (updateRequest.salary() != null && updateRequest.salary().signum() < 0) {
            return ResponseEntity.badRequest().build();
        }

        return service.findById(id)
                .map(employee -> {
                    employee.setSalary(updateRequest.salary());
                    employee.setNationalId(updateRequest.nationalId());
                    return ResponseEntity.ok(service.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<Void> authorizeProfileAccess(
            HttpServletRequest request,
            Long requestedId) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object value = session.getAttribute("userId");
        if (!(value instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!userId.equals(requestedId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return null;
    }
}
