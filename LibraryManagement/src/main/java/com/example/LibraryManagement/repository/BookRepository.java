// BookRepository.java
package com.example.LibraryManagement.repository;
import com.example.LibraryManagement.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {}
