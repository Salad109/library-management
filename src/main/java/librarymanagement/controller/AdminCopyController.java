package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.dto.CopyCreateRequest;
import librarymanagement.model.Copy;
import librarymanagement.service.CopyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Admin Copy Operations", description = "Librarians can do CRUD operations on copies. Requires LIBRARIAN role")
public class AdminCopyController {

    private final CopyService copyService;

    public AdminCopyController(CopyService copyService) {
        this.copyService = copyService;
    }

    @Operation(summary = "Get all copies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies")
    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyService.getAllCopies(pageable);
    }

    @Operation(summary = "Get a specific copy by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copy found"),
            @ApiResponse(responseCode = "404", description = "Copy not found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies/{id}")
    public Copy getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @Operation(summary = "Get all copies of a book by its ISBN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Copies found"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role")
    })
    @GetMapping("/api/admin/copies/book/{isbn}")
    public Page<Copy> getCopiesByBookIsbn(@PathVariable String isbn, Pageable pageable) {
        return copyService.getCopiesByBookIsbn(isbn, pageable);
    }

    @Operation(summary = "Add new copies of a book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Copies created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
            @ApiResponse(responseCode = "403", description = "Requires LIBRARIAN role"),
            @ApiResponse(responseCode = "404", description = "Book not found with the given ISBN")
    })
    @PostMapping("/api/admin/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Copy> createCopies(@Valid @RequestBody CopyCreateRequest copyCreateRequest) {
        return copyService.addCopies(copyCreateRequest.bookIsbn(), copyCreateRequest.quantity());
    }
}
