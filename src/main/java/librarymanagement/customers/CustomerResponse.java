package librarymanagement.customers;

import jakarta.validation.constraints.NotNull;

public record CustomerResponse(@NotNull Long id, @NotNull String firstName, @NotNull String lastName, String email) {

    public static CustomerResponse from(Customer customer) {
        String email = null;
        if (customer.getUser() != null) {
            email = customer.getUser().getEmail();
        }
        return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(), email);
    }
}
