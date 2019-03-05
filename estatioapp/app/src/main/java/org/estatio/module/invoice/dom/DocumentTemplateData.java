/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.estatio.module.invoice.dom;

import org.incode.module.apptenancy.fixtures.enums.ApplicationTenancy_enum;
import org.incode.module.document.dom.impl.applicability.AttachmentAdvisor;
import org.incode.module.document.dom.impl.applicability.RendererModelFactory;
import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.docs.DocumentSort;
import org.incode.module.document.dom.impl.types.DocumentType;

import org.estatio.module.capex.dom.order.Order;
import org.estatio.module.capex.spiimpl.docs.aa.AttachToSameForOrder;
import org.estatio.module.capex.spiimpl.docs.rml.RendererModelFactoryForOrder;
import org.estatio.module.lease.dom.invoicing.summary.InvoiceSummaryForPropertyDueDateStatus;
import org.estatio.module.lease.spiimpl.document.binders.AttachToNone;
import org.estatio.module.lease.spiimpl.document.binders.ForPrimaryDocOfInvoiceAttachToInvoiceAndAnyRelevantSupportingDocuments;
import org.estatio.module.lease.spiimpl.document.binders.FreemarkerModelOfPrelimLetterOrInvoiceDocForEmailCover;
import org.estatio.module.lease.spiimpl.document.binders.StringInterpolatorToSsrsUrlOfInvoice;
import org.estatio.module.lease.spiimpl.document.binders.StringInterpolatorToSsrsUrlOfInvoiceSummary;

import lombok.Getter;
import static org.estatio.module.capex.seed.ordertmplt.DocumentTemplateFSForOrderConfirm.loadCharsForOrderConfirmTemplateTitleItaFtl;
import static org.estatio.module.lease.seed.DocumentTypeAndTemplatesFSForInvoicesUsingSsrs.loadResource;

/**
 * maximum length is 24 ({@link DocumentType.ReferenceType.Meta#MAX_LEN}).
 */
@Getter
public enum DocumentTemplateData {

    // cover notes
    COVER_NOTE_PRELIM_LETTER_ITA(

            ApplicationTenancy_enum.It.getPath(), " (Italy)",
            "html", "text/html", "text/html",
            DocumentSort.CLOB,
            //loadResource("PrelimLetterEmailCoverNote-ITA.html.ftl"),
            RenderingStrategyData.FMK,
            loadResource("PrelimLetterEmailCoverNoteSubjectLine-ITA.ftl"),
            RenderingStrategyData.FMK,
            false,
            Document.class, FreemarkerModelOfPrelimLetterOrInvoiceDocForEmailCover.class,
            AttachToNone.class

            // upsertDocumentTemplateForTextHtmlWithApplicability

    ),
    COVER_NOTE_INVOICE_ITA(

            ApplicationTenancy_enum.It.getPath(), " (Italy)",
            "html", "text/html", "text/html",
            DocumentSort.CLOB,
            //loadResource("InvoiceEmailCoverNote-ITA.html.ftl"),
            RenderingStrategyData.FMK,
            loadResource("InvoiceEmailCoverNoteSubjectLine-ITA.ftl"),
            RenderingStrategyData.FMK,
            false,
            Document.class, FreemarkerModelOfPrelimLetterOrInvoiceDocForEmailCover.class,
            AttachToNone.class

            // upsertDocumentTemplateForTextHtmlWithApplicability
    ),

    // primary docs
    PRELIM_LETTER_ITA(

            ApplicationTenancy_enum.It.getPath(), " (Italy)",
            "pdf", "application/pdf", null,
            DocumentSort.TEXT,

            //"${reportServerBaseUrl}PreliminaryLetterV2&id=${this.id}&rs:Command=Render&rs:Format=PDF",
            RenderingStrategyData.SIPC,
            loadResource("PrelimLetterTitle-ITA.ftl"),
            RenderingStrategyData.SI,
            false,
            Invoice.class, StringInterpolatorToSsrsUrlOfInvoice.class,
            ForPrimaryDocOfInvoiceAttachToInvoiceAndAnyRelevantSupportingDocuments.class

            // upsertTemplateForPdfWithApplicability
    ),
    INVOICE_ITA(

            ApplicationTenancy_enum.It.getPath(), "( Italy)",
            "pdf", "application/pdf", null,
            DocumentSort.TEXT,

            //"${reportServerBaseUrl}InvoiceItaly&id=${this.id}&rs:Command=Render&rs:Format=PDF",
            RenderingStrategyData.SIPC,
            loadResource("InvoiceTitle-ITA.ftl"),
            RenderingStrategyData.SI,
            false,
            Invoice.class, StringInterpolatorToSsrsUrlOfInvoice.class,
            ForPrimaryDocOfInvoiceAttachToInvoiceAndAnyRelevantSupportingDocuments.class

            // upsertTemplateForPdfWithApplicability
    ),


    // preview only
    INVOICES(
            ApplicationTenancy_enum.Global.getPath(), null,
            "pdf", "application/pdf", null,
            DocumentSort.TEXT,
            //"${reportServerBaseUrl}Invoices&dueDate=${this.dueDate}&${this.seller.id}&atPath=${this.atPath}&rs:Command=Render&rs:Format=PDF",
            RenderingStrategyData.SIPC,
            "Invoices overview",
            RenderingStrategyData.SI,
            true,
            InvoiceSummaryForPropertyDueDateStatus.class, StringInterpolatorToSsrsUrlOfInvoiceSummary.class,
            AttachToNone.class // since preview only

            // upsertTemplateForPdfWithApplicability
    ),
    INVOICES_PRELIM(

            ApplicationTenancy_enum.Global.getPath(), null,
            "pdf", "application/pdf", null,
            DocumentSort.TEXT,
            //"${reportServerBaseUrl}PreliminaryLetterV2&dueDate=${this.dueDate}&sellerId=${this.seller.id}&atPath=${this.atPath}&rs:Command=Render&rs:Format=PDF",
            RenderingStrategyData.SIPC,
            "Preliminary letter for Invoices",
            RenderingStrategyData.SI,
            true,
            InvoiceSummaryForPropertyDueDateStatus.class, StringInterpolatorToSsrsUrlOfInvoiceSummary.class,
            AttachToNone.class // since preview only

            // upsertTemplateForPdfWithApplicability
    ),
    INVOICES_FOR_SELLER(

            ApplicationTenancy_enum.Global.getPath(), null,
            "pdf", "application/pdf", null,
            DocumentSort.TEXT,
            //"${reportServerBaseUrl}PreliminaryLetterV2&dueDate=${this.dueDate}&sellerId=${this.seller.id}&atPath=${this.atPath}&rs:Command=Render&rs:Format=PDF",
            RenderingStrategyData.SIPC,
            "Preliminary Invoice for Seller", RenderingStrategyData.SI,
            true,
            InvoiceSummaryForPropertyDueDateStatus.class, StringInterpolatorToSsrsUrlOfInvoiceSummary.class,
            AttachToNone.class // since preview only

            // upsertTemplateForPdfWithApplicability
    ),

    ORDER_CONFIRM_ITA(

            ApplicationTenancy_enum.It.getPath(), "(Italy)",
            "docx", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            DocumentSort.BLOB,
            //loadBytesForOrderConfirmTemplateItaDocx(),
            RenderingStrategyData.XGP,
            loadCharsForOrderConfirmTemplateTitleItaFtl(),
            RenderingStrategyData.FMK,
            false,
            Order.class, RendererModelFactoryForOrder.class,
            AttachToSameForOrder.class

            // DocumentTemplateFSForOrderConfirm
    );

    private final String atPath;
    private final String extension;
    private final String mimeTypeBase;
    private final String inputMimeTypeBase;
    private final String nameSuffixIfAny;
    private final DocumentSort contentSort;
    //private final Object content;
    private final RenderingStrategyData contentRenderingStrategy;
    private final String nameText;
    private final RenderingStrategyData nameRenderingStrategy;
    private final boolean previewOnly;
    private final Class<? extends AttachmentAdvisor> attachmentAdvisorClass;
    private final Class<?> domainClass;
    private final Class<? extends RendererModelFactory> rendererModelFactoryClass;

    public enum Nature {
        INCOMING,
        OUTGOING,
        NOT_SPECIFIED
    }

    DocumentTemplateData(
            final String atPath,
            final String nameSuffixIfAny,
            final String extension,
            final String mimeTypeBase,
            final String inputMimeTypeBase,
            final DocumentSort contentSort,
            //final Object content,
            final RenderingStrategyData contentRenderingStrategy,
            final String nameText,
            final RenderingStrategyData nameRenderingStrategy,
            final boolean previewOnly,
            final Class<?> domainClass,
            final Class<? extends RendererModelFactory> rendererModelFactoryClass,
            final Class<? extends AttachmentAdvisor> attachmentAdvisorClass
    ) {
        this.attachmentAdvisorClass = attachmentAdvisorClass;
        this.domainClass = domainClass;
        this.rendererModelFactoryClass = rendererModelFactoryClass;
        this.atPath = atPath;
        this.nameSuffixIfAny = nameSuffixIfAny;
        this.contentSort = contentSort;
        this.extension = extension;
        this.mimeTypeBase = mimeTypeBase;
        this.inputMimeTypeBase = inputMimeTypeBase;
        //this.content = content;
        this.previewOnly = previewOnly;
        this.contentRenderingStrategy = contentRenderingStrategy;
        this.nameText = nameText;
        this.nameRenderingStrategy = nameRenderingStrategy;
    }

}
