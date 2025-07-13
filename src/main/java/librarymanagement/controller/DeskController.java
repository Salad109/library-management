package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.dto.CheckoutRequest;
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
}
