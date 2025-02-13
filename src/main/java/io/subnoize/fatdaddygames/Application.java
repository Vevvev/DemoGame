package io.subnoize.fatdaddygames;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author youca
 *
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(Application.class, args);
	}

}
