package org.ikigaidigital;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests: they pin the current behaviour of {@link TimeDepositCalculator#updateBalance},
 * which the requirements declare correct and which must not change during refactoring.
 * Expected values are exact results of the existing implementation (one month of interest per call).
 */
class TimeDepositCalculatorTest {

    private static final double BALANCE = 1000.00;

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} plan at 30 days earns no interest")
    @ValueSource(strings = {"basic", "student", "premium"})
    void noPlanEarnsInterestUpToDay30(String planType) {
        assertThat(balanceAfterUpdate(planType, BALANCE, 30)).isEqualTo(BALANCE);
    }

    @Nested
    class BasicPlan {

        @Test
        void earnsOnePercentYearlyInterestMonthlyFromDay31() {
            assertThat(balanceAfterUpdate("basic", BALANCE, 31)).isEqualTo(1000.83);
        }
    }

    @Nested
    class StudentPlan {

        @ParameterizedTest(name = "{0} days -> {1}")
        @CsvSource({
                "31,  1002.50",
                "365, 1002.50",
                "366, 1000.00",
        })
        void earnsThreePercentYearlyInterestMonthlyOnlyDuringTheFirstYear(int days, double expectedBalance) {
            assertThat(balanceAfterUpdate("student", BALANCE, days)).isEqualTo(expectedBalance);
        }
    }

    @Nested
    class PremiumPlan {

        @ParameterizedTest(name = "{0} days -> {1}")
        @CsvSource({
                "45, 1000.00",
                "46, 1004.17",
        })
        void earnsFivePercentYearlyInterestMonthlyOnlyAfter45Days(int days, double expectedBalance) {
            assertThat(balanceAfterUpdate("premium", BALANCE, days)).isEqualTo(expectedBalance);
        }
    }

    @ParameterizedTest(name = "plan type \"{0}\" earns no interest")
    @ValueSource(strings = {"gold", "Basic", ""})
    void unknownPlanTypeLeavesBalanceUnchanged(String planType) {
        assertThat(balanceAfterUpdate(planType, BALANCE, 100)).isEqualTo(BALANCE);
    }

    @Test
    void missingPlanTypeWithinTheFirst30DaysLeavesBalanceUnchanged() {
        assertThat(balanceAfterUpdate(null, BALANCE, 30)).isEqualTo(BALANCE);
    }

    @Test
    void missingPlanTypeAfter30DaysLeavesBalanceUnchanged() {
        // Intended deviation: the original implementation threw a NullPointerException here.
        // A missing plan type is now treated like an unknown one (README: invalid input handling is not required).
        assertThat(balanceAfterUpdate(null, BALANCE, 31)).isEqualTo(BALANCE);
    }

    @Nested
    class Rounding {

        @Test
        void interestIsRoundedToTwoDecimalPlaces() {
            // 1234567.00 * 0.01 / 12 = 1028.805833...
            assertThat(balanceAfterUpdate("basic", 1234567.00, 31)).isEqualTo(1235595.81);
        }

        @Test
        void onlyTheInterestIsRoundedNotTheResultingBalance() {
            // 100.555 * 0.01 / 12 = 0.0837... -> 0.08; balance keeps its third decimal
            assertThat(balanceAfterUpdate("basic", 100.555, 31)).isEqualTo(100.635);
        }

        @Test
        void exactHalfCentIsRoundedUp() {
            // 150.00 * 0.01 / 12 = 0.125, exactly representable as a double
            assertThat(balanceAfterUpdate("basic", 150.00, 31)).isEqualTo(150.13);
        }

        @Test
        void roundingAppliesToTheBinaryDoubleValueOfTheInterest() {
            // Decimal interest is 0.015, but the double is 0.01499999... so HALF_UP yields 0.01, not 0.02.
            // Existing (declared correct) behaviour - must be preserved.
            assertThat(balanceAfterUpdate("basic", 18.00, 31)).isEqualTo(18.01);
        }
    }

    @Test
    void updatesEveryDepositInTheListAccordingToItsOwnPlan() {
        TimeDeposit basic = new TimeDeposit(1, "basic", BALANCE, 31);
        TimeDeposit student = new TimeDeposit(2, "student", BALANCE, 31);
        TimeDeposit premium = new TimeDeposit(3, "premium", BALANCE, 46);
        TimeDeposit tooYoung = new TimeDeposit(4, "premium", BALANCE, 30);

        calculator.updateBalance(List.of(basic, student, premium, tooYoung));

        assertThat(List.of(basic, student, premium, tooYoung))
                .extracting(TimeDeposit::getBalance)
                .containsExactly(1000.83, 1002.50, 1004.17, 1000.00);
    }

    private double balanceAfterUpdate(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
