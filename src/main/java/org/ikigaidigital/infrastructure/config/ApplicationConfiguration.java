package org.ikigaidigital.infrastructure.config;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.GetAllTimeDeposits;
import org.ikigaidigital.application.TimeDepositRepository;
import org.ikigaidigital.application.UpdateAllBalances;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers domain and application classes as beans, so they need no Spring stereotypes (@Component/@Service);
 * the domain has no Spring dependency at all, the use cases only declare @Transactional.
 * Being Spring beans makes that @Transactional (and with it the row lock) effective.
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
