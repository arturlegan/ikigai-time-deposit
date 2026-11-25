package org.ikigaidigital.infrastructure.web;

import org.ikigaidigital.application.GetAllTimeDeposits;
import org.ikigaidigital.application.UpdateAllBalances;
import org.ikigaidigital.infrastructure.web.api.TimeDepositsApi;
import org.ikigaidigital.infrastructure.web.model.TimeDepositDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Web adapter: implements the interface generated from openapi.yaml, so code and contract cannot drift. */
@RestController
class TimeDepositController implements TimeDepositsApi {

    private final GetAllTimeDeposits getAllTimeDeposits;
    private final UpdateAllBalances updateAllBalances;

    TimeDepositController(GetAllTimeDeposits getAllTimeDeposits, UpdateAllBalances updateAllBalances) {
        this.getAllTimeDeposits = getAllTimeDeposits;
        this.updateAllBalances = updateAllBalances;
    }

    @Override
    public ResponseEntity<List<TimeDepositDto>> getAllTimeDeposits() {
        return ResponseEntity.ok(getAllTimeDeposits.execute().stream().map(TimeDepositDtoMapper::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> updateAllBalances() {
        updateAllBalances.execute();
        return ResponseEntity.noContent().build();
    }
}
