package com.donate.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DonateApplication {

    public static void main(String[] args) {
        activateProductionProfileOnRailway();
        SpringApplication.run(DonateApplication.class, args);
    }

    private static void activateProductionProfileOnRailway() {
        String configuredProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (configuredProfile != null && !configuredProfile.isBlank()) {
            return;
        }

        String mysqlHost = System.getenv("MYSQLHOST");
        String railwayEnvironment = System.getenv("RAILWAY_ENVIRONMENT");
        if ((mysqlHost != null && !mysqlHost.isBlank())
                || (railwayEnvironment != null && !railwayEnvironment.isBlank())) {
            System.setProperty("spring.profiles.active", "prod");
        }
    }
}
