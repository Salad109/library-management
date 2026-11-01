package librarymanagement.copies;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('LIBRARIAN')")
@Tag(name = "Desk Operations", description = "Librarians can manage copy checkouts, returns, and lost copies. " +
        "Requires LIBRARIAN role")
public class DeskController {

    private final CopyService copyService;

    public DeskController(CopyService copyService) {
        this.copyService = copyService;
    }

    @Operation(summary = "Checkout a copy to a customer",
            description = "Desk operation. Librarians can checkout a copy to a customer. The copy must be either " +
                    "available or reserved by the specific customer. Requires LIBRARIAN role")
    @PostMapping("/api/desk/checkout")
    public CopyResponse checkout(@Valid @RequestBody CopyCheckoutRequest request) {
        return copyService.checkout(request.copyId(), request.customerId());
    }

    @Operation(summary = "Return a copy from a customer",
            description = "Desk operation. Librarians can return a copy from a customer. The copy must be checked out " +
                    "to the specific customer. Requires LIBRARIAN role")
    @PostMapping("/api/desk/return")
    public CopyResponse returnCopy(@Valid @RequestBody CopyReturnRequest request) {
        return copyService.returnCopy(request.copyId(), request.customerId());
    }

    @Operation(summary = "Mark a copy as lost",
            description = "Desk operation. Librarians mark a copy as lost instead of deleting it from the database. " +
                    "Requires LIBRARIAN role")
    @PostMapping("/api/desk/mark-lost")
    public CopyResponse markLost(@Valid @RequestBody CopyMarkLostRequest request) {
        return copyService.markLost(request.copyId());
    }
}
