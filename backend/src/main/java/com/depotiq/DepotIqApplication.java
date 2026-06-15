package com.depotiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DepotIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepotIqApplication.class, args);
    }
}

