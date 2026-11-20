package com.accenture.sast.tracker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role;
    private String email;
    private LocalDate onboardingDate;

    // Added for V7: IDOR — these fields are intentionally sensitive
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private BigDecimal salary;

@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String nationalId;

    public Employee() {}

    public Employee(String name, String role, String email, LocalDate onboardingDate) {
        this.name = name;
        this.role = role;
        this.email = email;
        this.onboardingDate = onboardingDate;
    }

    public Long getId()                       { return id; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public String getRole()                   { return role; }
    public void setRole(String role)          { this.role = role; }
    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }
    public LocalDate getOnboardingDate()      { return onboardingDate; }
    public void setOnboardingDate(LocalDate d){ this.onboardingDate = d; }
    public BigDecimal getSalary()             { return salary; }
    public void setSalary(BigDecimal salary)  { this.salary = salary; }
    public String getNationalId()             { return nationalId; }
    public void setNationalId(String id)      { this.nationalId = id; }
}
