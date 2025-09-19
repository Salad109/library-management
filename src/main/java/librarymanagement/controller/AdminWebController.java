package librarymanagement.controller;

import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {

    private final BookService bookService;
    private final CopyService copyService;

    public AdminWebController(BookService bookService, CopyService copyService) {
        this.bookService = bookService;
        this.copyService = copyService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }

    @GetMapping("/admin/books/browse")
    public String bookBrowsePage(Model model, Pageable pageable) {
        Page<Book> books = bookService.getAllBooks(pageable);
        model.addAttribute("books", books);
        model.addAttribute("bookCount", books.getTotalElements());

        int currentPage = books.getNumber();
        int totalPages = books.getTotalPages();
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);

        int windowSize = 3;
        int startPage = Math.max(0, currentPage - windowSize);
        int endPage = Math.min(totalPages - 1, currentPage + windowSize);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("windowSize", windowSize);

        int startItem = currentPage * books.getSize() + 1;
        int endItem = Math.min(startItem + books.getSize() - 1, (int) books.getTotalElements());
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);

        return "books-browse";
    }

    @GetMapping("/admin/books/add")
    public String bookAddPage() {
        return "books-add";
    }

    @GetMapping("/admin/copies/browse")
    public String copyBrowsePage(Model model, Pageable pageable) {
        Page<Copy> copies = copyService.getAllCopies(pageable);
        model.addAttribute("copies", copies);
        model.addAttribute("copyCount", copies.getTotalElements());

        int currentPage = copies.getNumber();
        int totalPages = copies.getTotalPages();
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);

        int windowSize = 3;
        int startPage = Math.max(0, currentPage - windowSize);
        int endPage = Math.min(totalPages - 1, currentPage + windowSize);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("windowSize", windowSize);

        int startItem = currentPage * copies.getSize() + 1;
        int endItem = Math.min(startItem + copies.getSize() - 1, (int) copies.getTotalElements());
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);

        return "copies-browse";
    }

    @GetMapping("/admin/copies/add")
    public String copiesAddPage() {
        return "copies-add";
    }
}