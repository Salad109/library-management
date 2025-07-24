package librarymanagement.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import librarymanagement.dto.CopyCheckoutRequest;
import librarymanagement.dto.CopyMarkLostRequest;
import librarymanagement.dto.CopyReturnRequest;
import librarymanagement.model.Copy;
import librarymanagement.service.CopyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Desk Operations", description = "Librarians can manage copy checkouts, returns, and lost copies. Requires LIBRARIAN role")
public class DeskController {

    private final CopyService copyService;

    public DeskController(CopyService copyService) {
        this.copyService = copyService;
    }

    @PostMapping("/api/desk/checkout")
    public Copy checkout(@Valid @RequestBody CopyCheckoutRequest request) {
        return copyService.checkout(request.copyId(), request.customerId());
    }

    @PostMapping("/api/desk/return")
    public Copy returnCopy(@Valid @RequestBody CopyReturnRequest request) {
        return copyService.returnCopy(request.copyId(), request.customerId());
    }

    @PostMapping("/api/desk/mark-lost")
    public Copy markLost(@Valid @RequestBody CopyMarkLostRequest request) {
        return copyService.markLost(request.copyId());
    }

}
