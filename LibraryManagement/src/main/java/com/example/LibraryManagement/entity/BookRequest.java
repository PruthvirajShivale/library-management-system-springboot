package com.example.LibraryManagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class BookRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate requestDate;
    private String status; 
    private String adminNotes;
}