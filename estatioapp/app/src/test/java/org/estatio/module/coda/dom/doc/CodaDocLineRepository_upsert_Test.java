package org.estatio.module.coda.dom.doc;

import java.math.BigDecimal;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.auto.Mock;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.core.unittestsupport.jmocking.JMockActions;
import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import static org.assertj.core.api.Assertions.assertThat;

public class CodaDocLineRepository_upsert_Test {

    @Rule
    public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

    @Mock
    RepositoryService mockRepositoryService;

    private CodaDocLineRepository codaDocLineRepository;
    private CodaDocHead docHead;

    @Before
    public void setUp() throws Exception {
        codaDocLineRepository = new CodaDocLineRepository();
        codaDocLineRepository.repositoryService = mockRepositoryService;

        docHead = new CodaDocHead("IT01", "FR-GEN", "12345");
    }

    @Test
    public void when_does_not_exist() throws Exception {

        // expecting
        context.checking(new Expectations() {{
            allowing(mockRepositoryService).uniqueMatch(with(any(QueryDefault.class)));
            will(returnValue(null));

            oneOf(mockRepositoryService).persist(with(any(CodaDocLine.class)));
            will(JMockActions.returnArgument(0));
        }});

        // when
        final CodaDocLine codaDocLine = codaDocLineRepository
                .upsert(docHead, 1, LineType.SUMMARY, "ACCOUNT_CODE", "description",
                        BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now(), "EXTREF2", "EXTREF3", "EXTREF4", "EXTREF5",
                        "IBAN", "USER_REF1", 'X', "MEDIA_CODE");

        // then
        assertThat(codaDocLine.getHandling()).isEqualTo(Handling.ATTENTION);
    }

    @Test
    public void when_does_exist() throws Exception {

        // given
        final CodaDocLine existingDocLine = new CodaDocLine();
        existingDocLine.setHandling(Handling.SYNCED);

        assertThat(existingDocLine.getHandling()).isEqualTo(Handling.SYNCED);

        // expecting
        context.checking(new Expectations() {{
            allowing(mockRepositoryService).uniqueMatch(with(any(QueryDefault.class)));
            will(returnValue(existingDocLine));
        }});

        // when
        final CodaDocLine codaDocLine = codaDocLineRepository
                .upsert(docHead, 1, LineType.SUMMARY, "ACCOUNT_CODE", "description",
                        BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now(), "EXTREF2", "EXTREF3", "EXTREF4", "EXTREF5",
                        "IBAN", "USER_REF1", 'X', "MEDIA_CODE");

        // then
        assertThat(codaDocLine).isSameAs(existingDocLine);
        assertThat(existingDocLine.getHandling()).isEqualTo(Handling.ATTENTION);
    }

}