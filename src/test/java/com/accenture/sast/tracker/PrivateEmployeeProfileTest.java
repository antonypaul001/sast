package com.accenture.sast.tracker;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.model.Employee;
import com.accenture.sast.tracker.repository.AppUserRepository;
import com.accenture.sast.tracker.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PrivateEmployeeProfileTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EmployeeRepository employees;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    private final String password = UUID.randomUUID().toString();
    private Employee alice;
    private Employee bob;

    @BeforeEach
    void provisionAccounts() {
        alice = createEmployee("Alice", "TEST-ALICE");
        bob = createEmployee("Bob", "TEST-BOB");

        AppUser employee = new AppUser("profile-alice", encoder.encode(password));
        employee.setEmployee(alice);
        users.save(employee);

        AppUser hr = new AppUser("profile-hr", encoder.encode(password));
        hr.setHr(true);
        users.save(hr);
    }

    @AfterEach
    void removeAccountsAndEmployees() {
        users.findByUsername("profile-alice").ifPresent(users::delete);
        users.findByUsername("profile-hr").ifPresent(users::delete);
        employees.deleteById(alice.getId());
        employees.deleteById(bob.getId());
    }

    @Test
    void anonymousCallerCanSeeOnlyThePublicProfile() throws Exception {
        mvc.perform(get("/api/employees/{id}/profile", alice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys()", containsInAnyOrder("id", "name")));

        mvc.perform(get("/api/employees/{id}/private-profile", alice.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCanSeeOwnSalaryButNotAnotherEmployeesPrivateProfile() throws Exception {
        MockHttpSession session = signIn("profile-alice");
        mvc.perform(get("/api/employees/{id}/private-profile", alice.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(95000))
                .andExpect(jsonPath("$.keys()", containsInAnyOrder("id", "name", "salary")));

        mvc.perform(get("/api/employees/{id}/private-profile", bob.getId()).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrCanReadThePermittedFieldsOnAnotherEmployeesProfile() throws Exception {
        mvc.perform(get("/api/employees/{id}/private-profile", bob.getId()).session(signIn("profile-hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(95000))
                .andExpect(jsonPath("$.nationalId").value("TEST-BOB"))
                .andExpect(jsonPath("$.keys()", containsInAnyOrder("id", "name", "salary", "nationalId")));
    }

    private MockHttpSession signIn(String username) throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = mapper.readTree(csrf.getResponse().getContentAsString());
        return (MockHttpSession) mvc.perform(post("/api/auth/session")
                        .session((MockHttpSession) csrf.getRequest().getSession(false))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);
    }

    private Employee createEmployee(String name, String nationalId) {
        Employee employee = new Employee(name, "Engineer", null, null);
        employee.setSalary(new BigDecimal("95000"));
        employee.setNationalId(nationalId);
        return employees.save(employee);
    }
}
