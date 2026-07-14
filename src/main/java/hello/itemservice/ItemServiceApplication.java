package hello.itemservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class ItemServiceApplication {

	public static void main(String[] args) {
		Locale.setDefault(Locale.KOREA);
		SpringApplication.run(ItemServiceApplication.class, args);
	}

}
