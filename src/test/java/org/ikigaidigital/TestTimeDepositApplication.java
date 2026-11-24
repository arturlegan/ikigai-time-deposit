package org.ikigaidigital;

import org.springframework.boot.SpringApplication;

/** Runs the real application against a throw-away PostgreSQL container: {@code ./mvnw spring-boot:test-run}. */
public class TestTimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.from(TimeDepositApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
