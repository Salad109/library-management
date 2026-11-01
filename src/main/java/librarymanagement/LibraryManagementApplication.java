package librarymanagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@OpenAPIDefinition(servers = @Server(url = "/"))
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableResilientMethods
public class LibraryManagementApplication {
    static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
