package librarymanagement.service;

import librarymanagement.model.Role;
import librarymanagement.model.User;
import librarymanagement.repository.UserRepository;
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
    void testLoadUserByUsername() {
        User user = new User();
        user.setUsername("Joe");
        user.setPassword("joe123");
        user.setRole(Role.ROLE_LIBRARIAN);

        when(userRepository.findByUsername("Joe")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("Joe");

        assertThat(result.getUsername()).isEqualTo("Joe");
        assertThat(result.getPassword()).isEqualTo("joe123");
        assertThat(result.getAuthorities().size()).isEqualTo(1);
    }

    @Test
    void testLoadNonexistentUserByUsername() {
        when(userRepository.findByUsername("Goober")).thenReturn(Optional.empty());

        try {
            userDetailsService.loadUserByUsername("Goober");
            fail("Expected UsernameNotFoundException to be thrown");
        } catch (UsernameNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo("User not found: Goober");
        }
    }
}