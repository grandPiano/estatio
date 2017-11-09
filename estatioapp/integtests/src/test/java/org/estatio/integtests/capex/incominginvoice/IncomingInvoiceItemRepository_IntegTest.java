package org.estatio.integtests.capex.incominginvoice;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.module.application.fixtures.property.personas.PropertyAndOwnerAndManagerForOxfGb;
import org.estatio.module.capex.dom.invoice.IncomingInvoice;
import org.estatio.module.capex.dom.invoice.IncomingInvoiceItem;
import org.estatio.module.capex.dom.invoice.IncomingInvoiceItemRepository;
import org.estatio.module.capex.dom.invoice.IncomingInvoiceRepository;
import org.estatio.module.capex.dom.invoice.IncomingInvoiceType;
import org.estatio.module.capex.dom.invoice.approval.IncomingInvoiceApprovalState;
import org.estatio.module.asset.dom.Property;
import org.estatio.module.asset.dom.PropertyRepository;
import org.estatio.module.charge.dom.Charge;
import org.estatio.module.charge.dom.ChargeRepository;
import org.estatio.module.invoice.dom.InvoiceStatus;
import org.estatio.module.invoice.dom.PaymentMethod;
import org.estatio.module.party.dom.Party;
import org.estatio.module.party.dom.PartyRepository;
import org.estatio.module.tax.dom.Tax;
import org.estatio.module.tax.dom.TaxRepository;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.module.party.fixtures.organisation.personas.OrganisationForHelloWorldGb;
import org.estatio.module.party.fixtures.organisation.personas.OrganisationForTopModelGb;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class IncomingInvoiceItemRepository_IntegTest extends EstatioIntegrationTest {

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(final ExecutionContext executionContext) {
                executionContext.executeChild(this, new EstatioBaseLineFixture());
                executionContext.executeChild(this, new OrganisationForTopModelGb());
                executionContext.executeChild(this, new OrganisationForHelloWorldGb());
                executionContext.executeChild(this, new PropertyAndOwnerAndManagerForOxfGb());
            }
        });
    }

    @Inject
    IncomingInvoiceItemRepository incomingInvoiceItemRepository;
    @Inject
    PropertyRepository propertyRepository;

    Party seller;
    Party buyer;
    Property property;
    String invoiceNumber;
    String atPath;
    LocalDate invoiceDate;
    LocalDate dueDate;
    PaymentMethod paymentMethod;
    InvoiceStatus invoiceStatus;
    String description;
    Charge charge;
    Tax tax;

    @Test
    public void findByInvoiceAndChargeAndSequence_works() throws Exception {

        // given
        IncomingInvoice invoice = createIncomingInvoiceAndTwoItemsWithSameCharge();

        // when
        Charge chargeToFindOnItem = charge;
        IncomingInvoiceItem item1 = incomingInvoiceItemRepository.findByInvoiceAndChargeAndSequence(invoice, chargeToFindOnItem, BigInteger.valueOf(1L));
        IncomingInvoiceItem item2 = incomingInvoiceItemRepository.findByInvoiceAndChargeAndSequence(invoice, chargeToFindOnItem, BigInteger.valueOf(2L));

        // then
        assertThat(item1.getInvoice()).isEqualTo(invoice);
        assertThat(item1.getCharge()).isEqualTo(charge);
        assertThat(item1.getSequence()).isEqualTo(BigInteger.valueOf(1L));

        assertThat(item2.getInvoice()).isEqualTo(invoice);
        assertThat(item2.getCharge()).isEqualTo(charge);
        assertThat(item2.getSequence()).isEqualTo(BigInteger.valueOf(2L));

        // and when
        Charge chargeNotToBeFoundOnItem = chargeRepository.findByReference("OTHER");
        IncomingInvoiceItem itemNotToBeFound = incomingInvoiceItemRepository.findByInvoiceAndChargeAndSequence(invoice, chargeNotToBeFoundOnItem, BigInteger.valueOf(1L));

        // then
        assertThat(itemNotToBeFound).isNull();

    }

    private IncomingInvoice createIncomingInvoiceAndTwoItemsWithSameCharge(){
        seller = partyRepository.findPartyByReference(OrganisationForTopModelGb.REF);
        buyer = partyRepository.findPartyByReference(OrganisationForHelloWorldGb.REF);
        property = propertyRepository.findPropertyByReference(PropertyAndOwnerAndManagerForOxfGb.REF);
        invoiceNumber = "123";
        invoiceDate = new LocalDate(2017,1,1);
        dueDate = invoiceDate.minusMonths(1);
        paymentMethod = PaymentMethod.BANK_TRANSFER;
        invoiceStatus = InvoiceStatus.NEW;
        atPath = "/GBR";

        IncomingInvoice invoice = incomingInvoiceRepository.create(IncomingInvoiceType.CAPEX, invoiceNumber, property,
                atPath,
                buyer, seller, invoiceDate, dueDate, paymentMethod, invoiceStatus, null,null, IncomingInvoiceApprovalState.PAID);

        charge = chargeRepository.findByReference("WORKS");
        description = "some description";
        tax = taxRepository.findByReference("FRF");

        invoice.addItem(
                invoice.getType(), charge, description,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                tax, dueDate, null,  null, null, null);

        invoice.addItem(
                invoice.getType(), charge, description,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                tax, dueDate, null,  null, null, null);

        return invoice;
    }

    @Inject
    PartyRepository partyRepository;

    @Inject
    ChargeRepository chargeRepository;

    @Inject
    IncomingInvoiceRepository incomingInvoiceRepository;

    @Inject
    TaxRepository taxRepository;

}
