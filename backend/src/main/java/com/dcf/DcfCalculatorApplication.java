package com.dcf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DcfCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DcfCalculatorApplication.class, args);
    }

}