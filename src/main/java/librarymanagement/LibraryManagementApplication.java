package librarymanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableResilientMethods
public class LibraryManagementApplication {
    void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
