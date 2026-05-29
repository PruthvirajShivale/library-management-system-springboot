// BookIssueRepository.java
package com.example.LibraryManagement.repository;
import com.example.LibraryManagement.entity.BookIssue;
import com.example.LibraryManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookIssueRepository extends JpaRepository<BookIssue, Long> {
    List<BookIssue> findByUserAndReturnedFalse(User user);
    List<BookIssue> findByReturnedFalse();
}