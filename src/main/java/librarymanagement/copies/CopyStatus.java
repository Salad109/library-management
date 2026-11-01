package librarymanagement.copies;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum CopyStatus {
    AVAILABLE, RESERVED, BORROWED, LOST
}