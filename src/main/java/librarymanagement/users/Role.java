package librarymanagement.users;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Role {
    ROLE_LIBRARIAN,
    ROLE_CUSTOMER
}