
package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.service.EmployeeService;
import com.accenture.sast.tracker.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.service.AuthService;
import jakarta.servlet.http.HttpSession;

import static com.accenture.sast.tracker.controller.AuthController.AUTHENTICATED_USERNAME;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;
    private final AuthService authService;

    public EmployeeController(
            EmployeeService service,
            AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody Employee employee) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.save(employee));
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

    @GetMapping("/search")
    public List<Employee> search(@RequestParam String name) {
        return service.search(name);
    }

    // V7: IDOR remediation
    // Verify authenticated identity and ownership or admin role
    // before returning sensitive employee information.

    @GetMapping("/{id}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
        @PathVariable Long id,
        HttpSession session) {

        Object usernameAttribute =
            session.getAttribute(AUTHENTICATED_USERNAME);

        if (!(usernameAttribute instanceof String username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<AppUser> user = authService.findByUsername(username);

        if (user.isEmpty() || user.get().getEmployee() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Employee authorisedEmployee = user.get().getEmployee();

        if (!authorisedEmployee.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Employee> employee = service.findById(id);

        if (employee.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Employee e = employee.get();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", e.getId());
        body.put("name", e.getName());
        body.put("salary", e.getSalary());
        body.put("nationalId", e.getNationalId());

        return ResponseEntity.ok(body);
    }
}
