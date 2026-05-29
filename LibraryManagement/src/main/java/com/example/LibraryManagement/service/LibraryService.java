package com.example.LibraryManagement.service;

import com.example.LibraryManagement.entity.*;
import com.example.LibraryManagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class LibraryService {

    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PreRegisteredMemberRepository preRegisteredRepository;
    @Autowired private BookIssueRepository bookIssueRepository;
    @Autowired private BookRequestRepository bookRequestRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Book> getAllBooks() { return bookRepository.findAll(); }
    public Book getBookById(Long id) { return bookRepository.findById(id).orElse(null); }
    public void saveBook(Book book) { bookRepository.save(book); }
    public void deleteBook(Long id) { bookRepository.deleteById(id); }

    public void preRegisterMember(PreRegisteredMember member) { preRegisteredRepository.save(member); }
    public List<PreRegisteredMember> getAllPreRegistered() { return preRegisteredRepository.findAll(); }

    public String registerUser(User user) {
        Optional<PreRegisteredMember> whitelisted = preRegisteredRepository.findByEmail(user.getEmail());
        if (whitelisted.isEmpty()) return "Registration Denied: Your email is not whitelisted by the Admin.";
        if (userRepository.findByEmail(user.getEmail()).isPresent()) return "Account already exists with this email.";
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("MEMBER");
        userRepository.save(user);
        return "SUCCESS";
    }

    public User loginUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt.get();
        }
        return null;
    }
    public BookIssue issueBook(Long bookId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
                if (user != null && getTotalOutstandingFines(user) > 100.0) {
            return null; 
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && user != null && !book.isIssued()) {
            book.setIssued(true);
            bookRepository.save(book);

            BookIssue issue = new BookIssue(book, user, LocalDate.now(), LocalDate.now().plusDays(14));
            return bookIssueRepository.save(issue);
        }
        return null;
    }

    public boolean returnBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && book.isIssued()) {
            book.setIssued(false);
            bookRepository.save(book);

            List<BookIssue> activeIssues = bookIssueRepository.findByReturnedFalse();
            for (BookIssue issue : activeIssues) {
                if (issue.getBook().getId().equals(bookId)) {
                    issue.setReturned(true);
                    if (LocalDate.now().isAfter(issue.getDueDate())) {
                        long overdays = ChronoUnit.DAYS.between(issue.getDueDate(), LocalDate.now());
                        issue.setFineAmount(overdays * 10.0);
                    }
                    bookIssueRepository.save(issue);
                    return true;
                }
            }
        }
        return false;
    }

    public List<BookIssue> getActiveIssuesForMember(User user) { return bookIssueRepository.findByUserAndReturnedFalse(user); }
    public List<BookIssue> getAllActiveIssues() { return bookIssueRepository.findByReturnedFalse(); }


    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateDailyOverdueFinesAutomatedEngine() {
        List<BookIssue> activeLendings = bookIssueRepository.findByReturnedFalse();
        LocalDate today = LocalDate.now();
        for (BookIssue issue : activeLendings) {
            if (today.isAfter(issue.getDueDate())) {
                long overdueDays = ChronoUnit.DAYS.between(issue.getDueDate(), today);
                issue.setFineAmount(overdueDays * 10.0); 
                bookIssueRepository.save(issue);
            }
        }
    }

    public double getTotalOutstandingFines(User user) {
        return bookIssueRepository.findAll().stream()
                .filter(issue -> issue.getUser().getId().equals(user.getId()) && issue.getFineAmount() > 0 && !issue.isFinePaid())
                .mapToDouble(BookIssue::getFineAmount)
                .sum();
    }

    public List<BookIssue> getAllUnpaidFines() {
        return bookIssueRepository.findAll().stream()
                .filter(issue -> issue.getFineAmount() > 0 && !issue.isFinePaid())
                .toList();
    }

    public void settleUserFineBalance(Long issueId) {
        BookIssue issue = bookIssueRepository.findById(issueId).orElse(null);
        if (issue != null) {
            issue.setFinePaid(true);
            bookIssueRepository.save(issue);
        }
    }

    public void submitBookProcurementRequest(String title, String author, User user) {
        BookRequest req = new BookRequest();
        req.setTitle(title);
        req.setAuthor(author);
        req.setUser(user);
        req.setRequestDate(LocalDate.now());
        req.setStatus("PENDING");
        req.setAdminNotes("Awaiting Review");
        bookRequestRepository.save(req);
    }

    public List<BookRequest> getRequestsForUser(User user) { return bookRequestRepository.findByUser(user); }
    public List<BookRequest> getAllRequests() { return bookRequestRepository.findAll(); }
    
    public void processProcurementRequest(Long id, String status, String notes) {
        BookRequest req = bookRequestRepository.findById(id).orElse(null);
        if (req != null) {
            req.setStatus(status);
            req.setAdminNotes(notes);
            bookRequestRepository.save(req);
            
            if ("APPROVED".equalsIgnoreCase(status)) {
                Book autoBook = new Book();
                autoBook.setTitle(req.getTitle());
                autoBook.setAuthor(req.getAuthor());
                autoBook.setIsbn("REQ-AUTO-" + req.getId());
                autoBook.setIssued(false);
                bookRepository.save(autoBook);
            }
        }
    }
}