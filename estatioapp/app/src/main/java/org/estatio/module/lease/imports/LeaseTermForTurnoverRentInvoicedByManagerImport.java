package org.estatio.module.lease.imports;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Nature;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.isisaddons.module.excel.dom.ExcelFixture;
import org.isisaddons.module.excel.dom.ExcelFixtureRowHandler;

import org.estatio.module.base.dom.Importable;
import org.estatio.module.charge.dom.Charge;
import org.estatio.module.charge.dom.ChargeRepository;
import org.estatio.module.invoice.dom.PaymentMethod;
import org.estatio.module.lease.dom.InvoicingFrequency;
import org.estatio.module.lease.dom.Lease;
import org.estatio.module.lease.dom.LeaseAgreementRoleTypeEnum;
import org.estatio.module.lease.dom.LeaseItem;
import org.estatio.module.lease.dom.LeaseItemRepository;
import org.estatio.module.lease.dom.LeaseItemStatus;
import org.estatio.module.lease.dom.LeaseItemType;
import org.estatio.module.lease.dom.LeaseRepository;
import org.estatio.module.lease.dom.LeaseTermForTurnoverRent;
import org.estatio.module.lease.dom.LeaseTermRepository;

import lombok.Getter;
import lombok.Setter;

@DomainObject(
        nature = Nature.VIEW_MODEL,
        objectType = "org.estatio.dom.viewmodels.LeaseTermForTurnoverRentInvoicedByManagerImport"
)
public class LeaseTermForTurnoverRentInvoicedByManagerImport implements ExcelFixtureRowHandler, Importable {

    @Deprecated
    public LeaseTermForTurnoverRentInvoicedByManagerImport() {
    }

    public LeaseTermForTurnoverRentInvoicedByManagerImport(
            final String leaseReference,
            final String chargeReference,
            final LocalDate termStartDate,
            final BigDecimal rentNetAmount
    ) {
        this.leaseReference = leaseReference;
        this.chargeReference = chargeReference;
        this.termStartDate = termStartDate;
        this.rentNetAmount = rentNetAmount;
    }

    @Getter @Setter
    private String leaseReference;

    @Getter @Setter
    private String chargeReference;

    @Getter @Setter
    private LocalDate termStartDate;

    @Getter @Setter
    private BigDecimal rentNetAmount;

    @Override
    public List<Object> handleRow(FixtureScript.ExecutionContext executionContext, ExcelFixture excelFixture, Object previousRow) {
        return importData(previousRow);
    }

    // REVIEW: other import view models have @Action annotation here...  but in any case, is this view model actually ever surfaced in the UI?
    public List<Object> importData() {
        return importData(null);
    }

    @Programmatic
    @Override
    public List<Object> importData(final Object previousRow) {
        LeaseItem item = importItem();
        LeaseTermForTurnoverRent term = (LeaseTermForTurnoverRent) item.findTerm(termStartDate);
        if (term==null) {
            term = (LeaseTermForTurnoverRent) leaseTermRepository.newLeaseTerm(item, null, termStartDate, termStartDate.plusYears(1).minusDays(1));
            term.setManualTurnoverRent(rentNetAmount);
        }
        return Lists.newArrayList(term);
    }

    @Programmatic
    public LeaseItem importItem() {
        final Lease lease = fetchLease(leaseReference);
        final Charge charge = fetchCharge(chargeReference);
        LeaseItem item = leaseItemRepository.findByLeaseAndTypeAndStartDateAndInvoicedBy(lease, LeaseItemType.TURNOVER_RENT, lease.getStartDate(), LeaseAgreementRoleTypeEnum.MANAGER);
        if (item == null) {
            item = lease.newItem(LeaseItemType.TURNOVER_RENT, LeaseAgreementRoleTypeEnum.MANAGER, charge, InvoicingFrequency.YEARLY_IN_ARREARS, PaymentMethod.MANUAL_PROCESS, lease.getStartDate());
            item.setSequence(BigInteger.ONE);
            item.setApplicationTenancyPath(lease.getAtPath());
            item.setStatus(LeaseItemStatus.ACTIVE);
        }
        return item;
    }

    private Lease fetchLease(final String leaseReference) {
        final Lease lease;
        lease = leaseRepository.findLeaseByReference(leaseReference.trim().replaceAll("~", "+"));
        if (lease == null) {
            throw new ApplicationException(String.format("Lease with reference %s not found.", leaseReference));
        }
        return lease;
    }

    private Charge fetchCharge(final String chargeReference) {
        final Charge charge = chargeRepository
                .findByReference(chargeReference);
        if (charge == null) {
            throw new ApplicationException(String.format("Charge with reference %s not found.", chargeReference));
        }
        return charge;
    }

    @Inject
    LeaseRepository leaseRepository;

    @Inject
    LeaseItemRepository leaseItemRepository;

    @Inject
    LeaseTermRepository leaseTermRepository;

    @Inject
    private ChargeRepository chargeRepository;

}
