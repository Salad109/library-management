package librarymanagement.users;

import jakarta.validation.Validator;
import librarymanagement.constants.Messages;
import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibrarianBootstrapTest extends IntegrationTest {

    @Autowired
    private LibrarianBootstrap librarianBootstrap;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Validator validator;

    @Test
    void testCreatesLibrarianWhenNoneExists() {
        librarianBootstrap.run(new DefaultApplicationArguments());

        assertThat(userRepository.findByEmail(librarianEmail))
                .hasValueSatisfying(user -> assertThat(user.getRole()).isEqualTo(Role.ROLE_LIBRARIAN));
    }

    @Test
    void testRunningTwiceCreatesOneLibrarian() {
        librarianBootstrap.run(new DefaultApplicationArguments());
        librarianBootstrap.run(new DefaultApplicationArguments());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void testDoesNothingWhenAnotherLibrarianExists() {
        userService.addLibrarian(new LibrarianCreateRequest("someone.else@example.com", librarianPassword));

        librarianBootstrap.run(new DefaultApplicationArguments());

        assertThat(userRepository.findByEmail(librarianEmail)).isEmpty();
    }

    @Test
    void testFailsWithoutCredentials() {
        LibrarianBootstrap bootstrap = new LibrarianBootstrap(userService, validator, "", "");

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Messages.USER_EMAIL_VALIDATION_MESSAGE)
                .hasMessageContaining(Messages.USER_PASSWORD_VALIDATION_MESSAGE);
    }

    @Test
    void testIgnoresMissingCredentialsWhenLibrarianExists() {
        userService.addLibrarian(new LibrarianCreateRequest("someone.else@example.com", librarianPassword));

        new LibrarianBootstrap(userService, validator, "", "").run(new DefaultApplicationArguments());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void testFailsOnInvalidCredentials() {
        LibrarianBootstrap bootstrap = new LibrarianBootstrap(userService, validator, "a".repeat(51), librarianPassword);

        assertThatThrownBy(() -> bootstrap.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(userService.librarianExists()).isFalse();
    }
}
