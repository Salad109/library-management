package librarymanagement.controller;

import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.Customer;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import librarymanagement.service.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminWebController {

    private final BookService bookService;
    private final CopyService copyService;
    private final CustomerService customerService;

    public AdminWebController(BookService bookService, CopyService copyService, CustomerService customerService) {
        this.bookService = bookService;
        this.copyService = copyService;
        this.customerService = customerService;
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
    public String bookBrowsePage(
            Model model, Pageable pageable, @RequestParam(required = false) String q) {
        Page<Book> books;

        if (q != null && !q.trim().isEmpty()) {
            books = bookService.searchBooks(q.trim(), pageable);
            model.addAttribute("searchQuery", q.trim());
        } else {
            books = bookService.getAllBooks(pageable);
            model.addAttribute("searchQuery", "");
        }

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
    public String copyBrowsePage(
            Model model, Pageable pageable,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String searchType) {

        Page<Copy> copies;
        String cleanQuery = (q != null) ? q.trim() : "";

        if (cleanQuery.isEmpty() || searchType == null) {
            copies = copyService.getAllCopies(pageable);
            model.addAttribute("searchQuery", "");
            model.addAttribute("searchType", "");
        } else {
            switch (searchType) {
                case "isbn":
                    copies = copyService.getCopiesByBookIsbn(cleanQuery, pageable);
                    break;
                case "customer":
                    try {
                        Long customerId = Long.parseLong(cleanQuery);
                        copies = copyService.getCopiesByCustomerId(customerId, pageable);
                    } catch (NumberFormatException e) {
                        copies = Page.empty(pageable);
                    }
                    break;
                case "title":
                    copies = copyService.getCopiesByBookTitle(cleanQuery, pageable);
                    break;
                default:
                    copies = copyService.getAllCopies(pageable);
            }

            model.addAttribute("searchQuery", cleanQuery);
            model.addAttribute("searchType", searchType);
        }

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

    @GetMapping("/admin/customers/browse")
    public String customerBrowsePage(Model model, Pageable pageable) {
        Page<Customer> customers = customerService.getAllCustomers(pageable);
        model.addAttribute("customers", customers);
        model.addAttribute("customerCount", customers.getTotalElements());

        int currentPage = customers.getNumber();
        int totalPages = customers.getTotalPages();
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);

        int windowSize = 3;
        int startPage = Math.max(0, currentPage - windowSize);
        int endPage = Math.min(totalPages - 1, currentPage + windowSize);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        int startItem = currentPage * customers.getSize() + 1;
        int endItem = Math.min(startItem + customers.getSize() - 1, (int) customers.getTotalElements());
        model.addAttribute("startItem", startItem);
        model.addAttribute("endItem", endItem);

        return "customers-browse";
    }

    @GetMapping("/admin/customers/add")
    public String customerAddPage() {
        return "customers-add";
    }
}