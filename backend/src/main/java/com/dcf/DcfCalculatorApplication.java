package com.dcf;

import org.springframework.boot.SpringApplication;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DcfCalculatorApplication {

    public static void main(String[] args) {// Load .env.local and put values into System properties
        Dotenv dotenv = Dotenv.configure()
                              .filename(".env.local") // ensure this matches your file name
                              .ignoreIfMissing()       // don't crash if file is missing
                              .load();

        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );
        
        SpringApplication.run(DcfCalculatorApplication.class, args);
    }

}