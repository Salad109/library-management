package librarymanagement.users;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Arrays;
import java.util.Set;

@Schema(enumAsRef = true)
public enum Role {
    ROLE_LIBRARIAN,
    ROLE_CUSTOMER;

    public static Role of(Authentication auth) {
        Set<String> authorities = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        return Arrays.stream(values())
                .filter(role -> authorities.contains(role.name()))
                .findFirst()
                .orElseThrow();
    }
}
