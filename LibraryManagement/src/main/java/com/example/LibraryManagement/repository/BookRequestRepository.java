package com.example.LibraryManagement.repository;

import com.example.LibraryManagement.entity.BookRequest;
import com.example.LibraryManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRequestRepository extends JpaRepository<BookRequest, Long> {
    List<BookRequest> findByUser(User user);
    List<BookRequest> findByStatus(String status);
}