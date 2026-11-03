package librarymanagement.users;

import librarymanagement.constants.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void testLoadUserByEmail() {
        User user = new User();
        user.setId(7L);
        user.setEmail("joe@example.com");
        user.setPassword("joe123");
        user.setRole(Role.ROLE_LIBRARIAN);

        when(userRepository.findByEmail("joe@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(" Joe@Example.com ");

        assertThat(result.getUsername()).isEqualTo("joe@example.com");
        assertThat(((AuthenticatedUser) result).getCustomerId()).isNull();
        assertThat(result.getPassword()).isEqualTo("joe123");
        assertThat(result.getAuthorities().size()).isEqualTo(1);
    }

    @Test
    void testLoadNonexistentUser() {
        when(userRepository.findByEmail("goober@example.com")).thenReturn(Optional.empty());

        try {
            userDetailsService.loadUserByUsername("goober@example.com");
            fail("Expected UsernameNotFoundException to be thrown");
        } catch (UsernameNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo(Messages.USER_NOT_FOUND + "goober@example.com");
        }
    }
}