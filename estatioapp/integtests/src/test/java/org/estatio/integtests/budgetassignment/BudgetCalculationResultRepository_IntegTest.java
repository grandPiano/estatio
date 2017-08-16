package org.estatio.integtests.budgetassignment;

import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.dom.asset.Property;
import org.estatio.dom.asset.PropertyRepository;
import org.estatio.dom.budgetassignment.calculationresult.BudgetCalculationResult;
import org.estatio.dom.budgetassignment.calculationresult.BudgetCalculationResultRepository;
import org.estatio.dom.budgetassignment.calculationresult.BudgetCalculationRun;
import org.estatio.dom.budgetassignment.calculationresult.BudgetCalculationRunRepository;
import org.estatio.dom.budgeting.budget.Budget;
import org.estatio.dom.budgeting.budget.BudgetRepository;
import org.estatio.dom.budgeting.budgetcalculation.BudgetCalculationType;
import org.estatio.dom.charge.Charge;
import org.estatio.dom.charge.ChargeRepository;
import org.estatio.dom.lease.Lease;
import org.estatio.dom.lease.LeaseRepository;
import org.estatio.fixture.asset.PropertyForOxfGb;
import org.estatio.fixture.budget.BudgetBaseLineFixture;
import org.estatio.fixture.budget.BudgetTeardownFixture;
import org.estatio.fixture.budget.BudgetsForOxf;
import org.estatio.fixture.charge.ChargeRefData;
import org.estatio.fixture.lease.LeaseForOxfTopModel001Gb;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class BudgetCalculationResultRepository_IntegTest extends EstatioIntegrationTest {

    @Inject
    BudgetRepository budgetRepository;

    @Inject
    BudgetCalculationRunRepository budgetCalculationRunRepository;

    @Inject
    BudgetCalculationResultRepository budgetCalculationResultRepository;

    @Inject
    PropertyRepository propertyRepository;

    @Inject
    LeaseRepository leaseRepository;

    @Inject
    ChargeRepository chargeRepository;

    Property propertyOxf;
    List<Budget> budgetsForOxf;
    Budget budget2015;
    Lease leaseTopModel;
    BudgetCalculationRun run;

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                executionContext.executeChild(this, new BudgetBaseLineFixture());
                executionContext.executeChild(this, new BudgetsForOxf());
                if (leaseRepository.findLeaseByReference(LeaseForOxfTopModel001Gb.REF)==null) {
                    executionContext.executeChild(this, new LeaseForOxfTopModel001Gb());
                }
            }
        });
        propertyOxf = propertyRepository.findPropertyByReference(PropertyForOxfGb.REF);
        budgetsForOxf = budgetRepository.findByProperty(propertyOxf);
        budget2015 = budgetRepository.findByPropertyAndStartDate(propertyOxf, BudgetsForOxf.BUDGET_2015_START_DATE);
        leaseTopModel = leaseRepository.findLeaseByReference(LeaseForOxfTopModel001Gb.REF);
        run = wrap(budgetCalculationRunRepository).findOrCreateNewBudgetCalculationRun(leaseTopModel, budget2015, BudgetCalculationType.BUDGETED);
    }

    @After
    public void teardownData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                executionContext.executeChild(this, new BudgetTeardownFixture());
            }
        });
    }

    public static class FindOrCreate extends BudgetCalculationResultRepository_IntegTest {

        @Test
        public void test() {

            // given
            assertThat(budgetCalculationResultRepository.allBudgetCalculationResults().size()).isEqualTo(0);
            Charge invoiceCharge = chargeRepository.findByReference(ChargeRefData.GB_SERVICE_CHARGE);

            // when
            BudgetCalculationResult result = wrap(budgetCalculationResultRepository).findOrCreateBudgetCalculationResult(run, invoiceCharge);

            // then
            assertThat(budgetCalculationResultRepository.allBudgetCalculationResults().size()).isEqualTo(1);
            assertThat(result.getBudgetCalculationRun()).isEqualTo(run);
            assertThat(result.getInvoiceCharge()).isEqualTo(invoiceCharge);

            // and when again
            wrap(budgetCalculationResultRepository).findOrCreateBudgetCalculationResult(run, invoiceCharge);

            // then is idemPotent
            assertThat(budgetCalculationResultRepository.allBudgetCalculationResults().size()).isEqualTo(1);

        }
    }

}
