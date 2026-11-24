package org.ikigaidigital;

import java.util.Arrays;
import java.util.Optional;

/**
 * Plan types with an interest policy. {@link TimeDeposit} keeps the plan type as a String (shared class, must not change),
 * so codes are resolved here by exact, case-sensitive match - same as the original implementation.
 */
enum PlanType {
    BASIC("basic"),
    STUDENT("student"),
    PREMIUM("premium");

    private final String code;

    PlanType(String code) {
        this.code = code;
    }

    /** Empty for unknown or null codes. */
    static Optional<PlanType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(planType -> planType.code.equals(code))
                .findFirst();
    }
}
