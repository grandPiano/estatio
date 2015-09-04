/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.estatio.integtests.assets.financial;

import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.wrapper.InvalidException;

import org.estatio.dom.asset.FixedAsset;
import org.estatio.dom.asset.FixedAssetRepository;
import org.estatio.dom.asset.financial.FixedAssetFinancialAccount;
import org.estatio.dom.asset.financial.FixedAssetFinancialAccounts;
import org.estatio.dom.financial.bankaccount.BankAccount;
import org.estatio.dom.financial.bankaccount.BankAccounts;
import org.estatio.dom.party.Parties;
import org.estatio.fixture.EstatioBaseLineFixture;
import org.estatio.fixture.asset._PropertyForOxfGb;
import org.estatio.fixture.financial.BankAccountForOxford;
import org.estatio.fixture.party.OrganisationForHelloWorldGb;
import org.estatio.integtests.EstatioIntegrationTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @version $Rev$ $Date$
 */
public class FixedAssetFinancialAccountsTest extends EstatioIntegrationTest {

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, new EstatioBaseLineFixture());
                executionContext.executeChild(this, new _PropertyForOxfGb());
                executionContext.executeChild(this, new BankAccountForOxford());
            }
        });
    }

    @Inject
    FixedAssetRepository fixedAssetRepository;

    @Inject
    FixedAssetFinancialAccounts fixedAssetFinancialAccounts;

    @Inject
    BankAccounts bankAccounts;

    @Inject
    Parties parties;

    public static class FindByFixedAsset extends FixedAssetFinancialAccountsTest {

        @Test
        public void findByFixedAsset() throws Exception {
            // given
            List<FixedAsset> fixedAsset = fixedAssetRepository.matchAssetsByReferenceOrName(_PropertyForOxfGb.REF);
            assertThat(fixedAsset.size(), is(1));

            // when
            final List<FixedAssetFinancialAccount> results = fixedAssetFinancialAccounts.findByFixedAsset(fixedAsset.get(0));

            // then
            assertThat(results.size(), is(1));
        }
    }

    public static class FindByFinancialAccount extends FixedAssetFinancialAccountsTest {

        @Test
        public void findByFinancialAccount() throws Exception {
            // given
            List<FixedAsset> fixedAsset = fixedAssetRepository.matchAssetsByReferenceOrName(_PropertyForOxfGb.REF);
            assertThat(fixedAsset.size(), is(1));

            // when
            final List<FixedAssetFinancialAccount> results = fixedAssetFinancialAccounts.findByFinancialAccount(bankAccounts.findBankAccountByReference(BankAccountForOxford.BANK_ACCOUNT_REF));

            // then
            assertThat(results.size(), is(1));
        }
    }

    public static class Find extends FixedAssetFinancialAccountsTest {

        @Test
        public void find() throws Exception {
            // given
            List<FixedAsset> fixedAsset = fixedAssetRepository.matchAssetsByReferenceOrName(_PropertyForOxfGb.REF);
            assertThat(fixedAsset.size(), is(1));

            // when
            final FixedAssetFinancialAccount result = fixedAssetFinancialAccounts.find(fixedAsset.get(0), bankAccounts.findBankAccountByReference(BankAccountForOxford.BANK_ACCOUNT_REF));

            // then
            assertNotNull(result);
        }
    }

    public static class OnBankAccountRemove extends FixedAssetFinancialAccountsTest {

        BankAccount oldBankAccount;
        BankAccount newBankAccount;

        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUp() throws Exception {
            oldBankAccount = bankAccounts.findBankAccountByReference(BankAccountForOxford.BANK_ACCOUNT_REF);
            newBankAccount = bankAccounts.newBankAccount(parties.findPartyByReference(OrganisationForHelloWorldGb.REF), "NEWBANKACCOUNT", "New Bank Account");
        }

        @Test
        public void removeFixedAssetFinancialAccount() throws Exception {
            // Given
            FixedAssetFinancialAccount fixedAssetFinancialAccount = fixedAssetFinancialAccounts.findByFinancialAccount(oldBankAccount).get(0);

            // When
            wrap(fixedAssetFinancialAccount).remove(true);

            // Then
            Assert.assertThat(fixedAssetFinancialAccounts.findByFinancialAccount(oldBankAccount).size(), is(0));
        }

        @Test
        public void whenVetoingSubscriber() {
            // Then
            expectedException.expect(InvalidException.class);

            // WHen
            wrap(oldBankAccount).remove(true);
        }
    }
}
