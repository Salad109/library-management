package librarymanagement.controller;

import librarymanagement.model.CopyStatus;
import librarymanagement.service.CopyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CopyController {

    private final CopyService copyService;

    public CopyController(CopyService copyService) {
        this.copyService = copyService;
    }

    @GetMapping("/api/copies/book/{isbn}/count")
    public long countAvailableCopies(@PathVariable String isbn) {
        return copyService.countCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
    }
}
