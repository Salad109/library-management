package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.CheckoutRequest;
import librarymanagement.dto.MarkLostRequest;
import librarymanagement.dto.ReturnRequest;
import librarymanagement.model.Copy;
import librarymanagement.service.CopyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeskController {

    private final CopyService copyService;

    public DeskController(CopyService copyService) {
        this.copyService = copyService;
    }

    @PostMapping("/api/desk/checkout")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public Copy checkout(@Valid @RequestBody CheckoutRequest request) {
        return copyService.checkout(request.copyId(), request.customerId());
    }

    @PostMapping("/api/desk/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public Copy returnCopy(@Valid @RequestBody ReturnRequest request) {
        return copyService.returnCopy(request.copyId(), request.customerId());
    }

    @PostMapping("/api/desk/mark-lost")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public Copy markLost(@Valid @RequestBody MarkLostRequest request) {
        return copyService.markLost(request.copyId());
    }

}
