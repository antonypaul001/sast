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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.repository.AppUserRepository;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "API_SECRET=test-only-secret")
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EmployeeRepository repo;
    @Autowired AppUserRepository userRepo;

    @BeforeEach
    void clean() { 
        userRepo.deleteAll();
        repo.deleteAll();
    }

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("username", "testadmin");

    return session;
}
    @Test
    void createEmployee_returns201() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "name", "Alice Smith",
                "role", "Engineer",
                "email", "alice@example.com",
                "onboardingDate", "2026-12-01"
        ));
        mvc.perform(post("/api/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice Smith"));
    }

    @Test
    void listEmployees_returnsAll() throws Exception {
        repo.save(new Employee("Bob Jones", "Analyst", "bob@example.com", LocalDate.of(2026, 12, 5)));
        repo.save(new Employee("Carol Lee",  "Manager", "carol@example.com", LocalDate.of(2026, 12, 10)));

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
        repo.save(new Employee("Diana Prince", "Developer", "diana@example.com", LocalDate.of(2026, 12, 15)));
        repo.save(new Employee("Eve Adams",    "Tester",    "eve@example.com",   LocalDate.of(2026, 12, 20)));

        mvc.perform(get("/api/employees/search").param("name", "diana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Diana Prince"));
    }

    @Test
    void searchByName_noMatch_returnsEmptyList() throws Exception {
        mvc.perform(get("/api/employees/search").param("name", "nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getProfile_returnsProfileForAuthorisedUser() throws Exception {
    Employee emp = new Employee(
            "Alice Smith",
            "Engineer",
            "alice@example.com",
            LocalDate.of(2026, 12, 1)
    );

    emp.setSalary(new BigDecimal("95000"));
    emp.setNationalId("NI-123456");

    Employee saved = repo.save(emp);

    AppUser user = new AppUser("alice", "test-password-hash");
    user.setEmployee(saved);
    userRepo.save(user);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("authenticatedUsername", "alice");

    mvc.perform(get("/api/employees/" + saved.getId() + "/profile")
                    .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice Smith"))
            .andExpect(jsonPath("$.salary").value(95000))
            .andExpect(jsonPath("$.nationalId").value("NI-123456"));
    }

    @Test
    void getProfile_withoutLogin_returns403() throws Exception {
        mvc.perform(get("/api/employees/9999/profile"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_forDifferentEmployee_returns403() throws Exception {
        Employee alice = new Employee(
            "Alice Smith",
            "Engineer",
            "alice@example.com",
            LocalDate.of(2026, 12, 1)
        );

        alice.setSalary(new BigDecimal("95000"));
        alice.setNationalId("NI-123456");
        Employee savedAlice = repo.save(alice);

        Employee bob = new Employee(
            "Bob Jones",
            "Analyst",
            "bob@example.com",
            LocalDate.of(2026, 12, 5)
        );

        bob.setSalary(new BigDecimal("80000"));
        bob.setNationalId("NI-654321");
        Employee savedBob = repo.save(bob);

        AppUser user = new AppUser("alice", "test-password-hash");
        user.setEmployee(savedAlice);
        userRepo.save(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("authenticatedUsername", "alice");

        mvc.perform(get("/api/employees/" + savedBob.getId() + "/profile")
                    .session(session))
            .andExpect(status().isForbidden());
    }

        @Test
    void getProfile_owner_returns200() throws Exception {
        Employee emp = new Employee(
                "Alice Smith",
                "Engineer",
                "alice@example.com",
                LocalDate.of(2026, 12, 1));

        emp.setSalary(new BigDecimal("95000"));
        emp.setNationalId("NI-123456");

        Employee saved = repo.save(emp);

        AppUser owner = new AppUser("testowner", "unused-test-hash");
        owner.setRole("USER");
        owner.setEmployeeId(saved.getId());
        userRepo.save(owner);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "testowner");

        mvc.perform(get("/api/employees/" + saved.getId() + "/profile")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.salary").value(95000))
                .andExpect(jsonPath("$.nationalId").value("NI-123456"));
    }
}
