package org.ikigaidigital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface WithdrawalJpaRepository extends JpaRepository<WithdrawalEntity, Integer> {
}
