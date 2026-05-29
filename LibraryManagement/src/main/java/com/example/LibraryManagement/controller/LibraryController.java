package com.example.LibraryManagement.controller;

import com.example.LibraryManagement.entity.*;
import com.example.LibraryManagement.service.LibraryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class LibraryController {

    @Autowired private LibraryService libraryService;

    @GetMapping("/")
    public String showLoginPage() { return "login"; }

    @GetMapping("/signup")
    public String showSignupPage() { return "signup"; }

    @PostMapping("/auth/signup")
    public String handleSignup(@ModelAttribute User user, Model model) {
        String status = libraryService.registerUser(user);
        if (!status.equals("SUCCESS")) {
            model.addAttribute("error", status);
            return "signup";
        }
        return "redirect:/?success=Account created! Please sign in.";
    }

    @PostMapping("/auth/login")
    public String handleLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        if(email.equals("admin@library.com") && password.equals("admin123")) {
            session.setAttribute("userRole", "ADMIN");
            return "redirect:/admin/dashboard";
        }

        User authenticatedUser = libraryService.loginUser(email, password);
        if (authenticatedUser != null) {
            session.setAttribute("loggedInUser", authenticatedUser);
            session.setAttribute("userRole", authenticatedUser.getRole());
            return "redirect:/member/dashboard";
        }

        model.addAttribute("error", "Access Denied: Invalid Email or Password.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        model.addAttribute("books", libraryService.getAllBooks());
        model.addAttribute("whitelistedMembers", libraryService.getAllPreRegistered());
        model.addAttribute("issuedBooks", libraryService.getAllActiveIssues());
        return "admin_dashboard";
    }

    @PostMapping("/admin/books/add")
    public String adminAddBook(@ModelAttribute Book book, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        libraryService.saveBook(book);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/members/whitelist")
    public String adminWhitelistMember(@ModelAttribute PreRegisteredMember member, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        libraryService.preRegisterMember(member);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/member/dashboard")
    public String showMemberDashboard(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null || !"MEMBER".equals(session.getAttribute("userRole"))) return "redirect:/";

        model.addAttribute("userProfile", loggedUser);
        model.addAttribute("books", libraryService.getAllBooks());
        model.addAttribute("myIssuedBooks", libraryService.getActiveIssuesForMember(loggedUser));
        model.addAttribute("myFines", libraryService.getTotalOutstandingFines(loggedUser));
        return "member_dashboard";
    }
    @GetMapping("/admin/analytics")
    public String showVisualAnalyticsPanel(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        
        long stockCount = libraryService.getAllBooks().stream().filter(b -> !b.isIssued()).count();
        long issuedCount = libraryService.getAllBooks().stream().filter(Book::isIssued).count();
        
        long totalBooks = stockCount + issuedCount;
       
        model.addAttribute("stockCount", stockCount);
        model.addAttribute("issuedCount", issuedCount);
        model.addAttribute("totalBooks", totalBooks); // <-- Yeh line miss ho rahi thi!
        model.addAttribute("requestsCount", libraryService.getAllRequests().size());
        
        return "admin_analytics";
    }

    @GetMapping("/admin/fines")
    public String showAdminFineLedgerDesk(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        model.addAttribute("unpaidFinesList", libraryService.getAllUnpaidFines());
        return "admin_fines";
    }

    @PostMapping("/admin/fines/settle/{id}")
    public String handleFineSettlementCollectionPayment(@PathVariable Long id, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        libraryService.settleUserFineBalance(id);
        return "redirect:/admin/fines";
    }

    @GetMapping("/member/requests")
    public String showUserRequestLounge(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null) return "redirect:/";
        model.addAttribute("myRequests", libraryService.getRequestsForUser(loggedUser));
        return "member_requests";
    }

    @PostMapping("/member/requests/submit")
    public String processUserRequestEntryFormSubmit(@RequestParam String title, @RequestParam String author, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser != null) {
            libraryService.submitBookProcurementRequest(title, author, loggedUser);
        }
        return "redirect:/member/requests";
    }

    @GetMapping("/admin/requests")
    public String showAdminProcurementControlPanel(HttpSession session, Model model) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        model.addAttribute("globalRequests", libraryService.getAllRequests());
        return "admin_requests";
    }

    @PostMapping("/admin/requests/resolve/{id}")
    public String executeRequestResolution(@PathVariable Long id, @RequestParam String status, @RequestParam String notes, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return "redirect:/";
        libraryService.processProcurementRequest(id, status, notes);
        return "redirect:/admin/requests";
    }

    @DeleteMapping("/api/books/delete/{id}")
    @ResponseBody
    public Map<String, Object> apiDeleteBook(@PathVariable Long id, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) return Map.of("success", false, "message", "Unauthorized");
        try {
            libraryService.deleteBook(id);
            return Map.of("success", true, "message", "Book completely expunged from database catalog.");
        } catch(Exception e) {
            return Map.of("success", false, "message", "Error: File is currently locked or referenced.");
        }
    }

    @PostMapping("/api/books/return/{id}")
    @ResponseBody
    public Map<String, Object> apiReturnBook(@PathVariable Long id) {
        boolean status = libraryService.returnBook(id);
        if(status) return Map.of("success", true, "message", "Book transaction balanced. Returned to storage.");
        return Map.of("success", false, "message", "Could not complete operation.");
    }

    @PostMapping("/api/books/booknow/{id}")
    @ResponseBody
    public Map<String, Object> apiBookNow(@PathVariable Long id, HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null) return Map.of("success", false, "message", "Session timed out.");
        
        if(libraryService.getTotalOutstandingFines(loggedUser) > 100.0) {
            return Map.of("success", false, "message", "Operation Blocked: Your outstanding fine penalty balance exceeds ₹100 threshold limit.");
        }

        BookIssue issue = libraryService.issueBook(id, loggedUser.getId());
        if(issue != null) {
            return Map.of(
                "success", true, "message", "Asset allocation complete!",
                "title", issue.getBook().getTitle(), "author", issue.getBook().getAuthor(),
                "issueDate", issue.getIssueDate().toString(), "dueDate", issue.getDueDate().toString()
            );
        }
        return Map.of("success", false, "message", "Book is already reserved or unavailable.");
    }
}