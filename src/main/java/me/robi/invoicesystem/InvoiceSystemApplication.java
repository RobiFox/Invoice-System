package me.robi.invoicesystem;

import me.robi.invoicesystem.constants.PathConstants;
import me.robi.invoicesystem.entities.ProductEntity;
import me.robi.invoicesystem.repositories.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class InvoiceSystemApplication {
	public static void main(String[] args) {
		try {
			Files.createDirectories(Paths.get(PathConstants.PDF_FILE_STORAGE));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		SpringApplication.run(InvoiceSystemApplication.class, args);
	}

	@Bean
	public CommandLineRunner fillProductRepo(ProductRepository repository) {
		return (args -> {
			repository.save(new ProductEntity("Item 1", 11));
			repository.save(new ProductEntity("Item 2", 18));
			repository.save(new ProductEntity("Item 3", 45));
			repository.save(new ProductEntity("Item 4", 9));
			repository.save(new ProductEntity("Item 5", 1));
		});
	}
}
