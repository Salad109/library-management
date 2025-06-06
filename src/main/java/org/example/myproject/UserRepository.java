package org.example.myproject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Boot automatically gives you:
    // save(), findAll(), findById(), deleteById(), etc.

    // But you can add custom queries too:
    Optional<User> findByName(String name);
    List<User> findByCity(String city);
    List<User> findByAgeGreaterThan(int age);
}