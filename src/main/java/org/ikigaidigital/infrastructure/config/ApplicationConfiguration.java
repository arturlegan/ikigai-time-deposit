package org.ikigaidigital.infrastructure.config;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.GetAllTimeDeposits;
import org.ikigaidigital.application.TimeDepositRepository;
import org.ikigaidigital.application.UpdateAllBalances;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers domain and application classes as beans, so they stay free of Spring stereotypes.
 * Being Spring beans also makes the use cases' @Transactional (and with it the row lock) effective.
 */
@Configuration(proxyBeanMethods = false)
class ApplicationConfiguration {

    @Bean
    TimeDepositCalculator timeDepositCalculator() {
        return new TimeDepositCalculator();
    }

    @Bean
    UpdateAllBalances updateAllBalances(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        return new UpdateAllBalances(repository, calculator);
    }

    @Bean
    GetAllTimeDeposits getAllTimeDeposits(TimeDepositRepository repository) {
        return new GetAllTimeDeposits(repository);
    }
}
