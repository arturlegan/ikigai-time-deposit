package org.ikigaidigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Root package on purpose: the existing domain (TimeDeposit) cannot move without breaking its consumers,
// and component scanning from here covers the application and infrastructure sub-packages.
@SpringBootApplication
public class TimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeDepositApplication.class, args);
    }
}
