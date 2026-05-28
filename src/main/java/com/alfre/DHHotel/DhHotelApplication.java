package com.alfre.DHHotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * This class contains main method of the application the start point of the app execution.
 *
 * @author Alfredo Sobrados González
 */
@SpringBootApplication
@EnableCaching
public class DhHotelApplication {

	public static void main(String[] args) {
		SpringApplication.run(DhHotelApplication.class, args);
	}

}
