package io.cinema.mstickets;

import io.cinema.config.AuditingConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = "io.cinema")
@Import({AuditingConfig.class})
public class MsTicketsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsTicketsApplication.class, args);
    }

}
