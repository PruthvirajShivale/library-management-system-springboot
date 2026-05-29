package com.example.LibraryManagement.service;

import com.example.LibraryManagement.entity.Book;
import com.example.LibraryManagement.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    public boolean issueBook(Long id) {
        Book book = getBookById(id);
        if (book != null && !book.isIssued()) {
            book.setIssued(true);
            bookRepository.save(book);
            return true;
        }
        return false;
    }

    public boolean returnBook(Long id) {
        Book book = getBookById(id);
        if (book != null && book.isIssued()) {
            book.setIssued(false);
            bookRepository.save(book);
            return true;
        }
        return false;
    }
}