package com.accenture.sast.tracker;

import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "test-user", roles = "USER")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EmployeeRepository repo;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void createEmployee_returns201() throws Exception {

        String body = mapper.writeValueAsString(
                Map.of(
                        "name", "Alice Smith",
                        "role", "Engineer",
                        "email", "alice@example.com",
                        "username", "alice",
                        "onboardingDate", "2026-12-01"
                )
        );

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void listEmployees_returnsAll() throws Exception {

        Employee bob = new Employee(
                "Bob Jones",
                "Analyst",
                "bob@example.com",
                LocalDate.of(2026, 12, 5)
        );
        bob.setName("bob");

        Employee carol = new Employee(
                "Carol Lee",
                "Manager",
                "carol@example.com",
                LocalDate.of(2026, 12, 10)
        );
        carol.setName("carol");

        repo.save(bob);
        repo.save(carol);

        mvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getById_notFound_returns404() throws Exception {

        mvc.perform(get("/api/employees/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchByName_returnsMatchingEmployee() throws Exception {

        Employee diana = new Employee(
                "Diana Prince",
                "Developer",
                "diana@example.com",
                LocalDate.of(2026, 12, 15)
        );
        diana.setName("diana");

        Employee eve = new Employee(
                "Eve Adams",
                "Tester",
                "eve@example.com",
                LocalDate.of(2026, 12, 20)
        );
        eve.setName("eve");

        repo.save(diana);
        repo.save(eve);

        mvc.perform(get("/api/employees/search")
                        .param("name", "diana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Diana Prince"));
    }

    @Test
    void searchByName_noMatch_returnsEmptyList() throws Exception {

        mvc.perform(get("/api/employees/search")
                        .param("name", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getProfile_owner_returnsProfile() throws Exception {

        Employee employee = createEmployee(
                "Alice Smith",
                "Engineer",
                "alice@example.com",
                "alice"
        );

        Employee saved = repo.save(employee);

        mvc.perform(get(
                        "/api/employees/{id}/profile",
                        saved.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.salary").value(95000))
                .andExpect(jsonPath("$.nationalId")
                        .value("NI-123456"));
    }

    @Test
    @WithMockUser(username = "system-admin", roles = "ADMIN")
    void getProfile_admin_returnsProfile() throws Exception {

        Employee employee = createEmployee(
                "Alice Smith",
                "Engineer",
                "alice@example.com",
                "alice"
        );

        Employee saved = repo.save(employee);

        mvc.perform(get(
                        "/api/employees/{id}/profile",
                        saved.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.salary").value(95000))
                .andExpect(jsonPath("$.nationalId")
                        .value("NI-123456"));
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    void getProfile_otherUser_returns403() throws Exception {

        Employee employee = createEmployee(
                "Alice Smith",
                "Engineer",
                "alice@example.com",
                "alice"
        );

        Employee saved = repo.save(employee);

        mvc.perform(get(
                        "/api/employees/{id}/profile",
                        saved.getId()
                ))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getProfile_notFound_returns404() throws Exception {

        mvc.perform(get("/api/employees/9999/profile"))
                .andExpect(status().isNotFound());
    }

    private Employee createEmployee(
            String name,
            String role,
            String email,
            String username) {

        Employee employee = new Employee(
                name,
                role,
                email,
                LocalDate.of(2026, 12, 1)
        );

        employee.setName(username);
        employee.setSalary(new BigDecimal("95000"));
        employee.setNationalId("NI-123456");

        return employee;
    }
}
