package com.example.LibraryManagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pre_registered_members")
public class PreRegisteredMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    private String name;

    public PreRegisteredMember() {}
    public PreRegisteredMember(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}