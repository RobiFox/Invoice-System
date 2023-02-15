package me.robi.invoicesystem;

import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InvoiceSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(InvoiceSystemApplication.class, args);
	}

	@Bean
	public CommandLineRunner fillProductRepo(ProductRepository repository) {
		return (args -> {
			repository.save(new ProductEntity("Item 1", 11));
			repository.save(new ProductEntity("Item 2", 18));
		});
	}
}
