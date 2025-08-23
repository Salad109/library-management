package librarymanagement.controller;

import librarymanagement.model.Book;
import librarymanagement.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {

    private final BookService bookService;

    public AdminWebController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        return "admin";
    }

    @GetMapping("/admin/books/browse")
    public String bookBrowsePage(Model model) {
        Page<Book> books = bookService.getAllBooks(Pageable.ofSize(20));
        model.addAttribute("books", books);
        model.addAttribute("bookCount", books.getTotalElements());
        return "books-browse";
    }
}