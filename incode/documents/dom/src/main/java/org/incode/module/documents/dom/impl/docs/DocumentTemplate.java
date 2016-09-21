/*
 *  Copyright 2016 Dan Haywood
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.incode.module.documents.dom.impl.docs;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.jdo.JDOHelper;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;

import com.google.common.eventbus.Subscribe;

import org.axonframework.eventhandling.annotation.EventHandler;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import org.apache.isis.applib.AbstractSubscriber;
import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.i18n.TranslatableString;
import org.apache.isis.applib.services.queryresultscache.QueryResultsCache;
import org.apache.isis.applib.services.registry.ServiceRegistry2;
import org.apache.isis.applib.services.title.TitleService;
import org.apache.isis.applib.value.Blob;
import org.apache.isis.applib.value.Clob;

import org.incode.module.documents.dom.DocumentsModule;
import org.incode.module.documents.dom.impl.applicability.Applicability;
import org.incode.module.documents.dom.impl.applicability.ApplicabilityRepository;
import org.incode.module.documents.dom.impl.applicability.Binder;
import org.incode.module.documents.dom.impl.links.PaperclipRepository;
import org.incode.module.documents.dom.impl.renderers.Renderer;
import org.incode.module.documents.dom.impl.renderers.RendererFromBytesToBytes;
import org.incode.module.documents.dom.impl.renderers.RendererFromBytesToBytesWithPreviewToUrl;
import org.incode.module.documents.dom.impl.renderers.RendererFromBytesToChars;
import org.incode.module.documents.dom.impl.renderers.RendererFromBytesToCharsWithPreviewToUrl;
import org.incode.module.documents.dom.impl.renderers.RendererFromCharsToBytes;
import org.incode.module.documents.dom.impl.renderers.RendererFromCharsToBytesWithPreviewToUrl;
import org.incode.module.documents.dom.impl.renderers.RendererFromCharsToChars;
import org.incode.module.documents.dom.impl.renderers.RendererFromCharsToCharsWithPreviewToUrl;
import org.incode.module.documents.dom.impl.rendering.RenderingStrategy;
import org.incode.module.documents.dom.services.ClassNameViewModel;
import org.incode.module.documents.dom.services.ClassService;
import org.incode.module.documents.dom.spi.BinderClassNameService;
import org.incode.module.documents.dom.impl.types.DocumentType;
import org.incode.module.documents.dom.valuetypes.FullyQualifiedClassNameSpecification;

import lombok.Getter;
import lombok.Setter;

@PersistenceCapable(
        identityType= IdentityType.DATASTORE,
        schema = "incodeDocuments",
        table = "DocumentTemplate"
)
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Queries({
        @javax.jdo.annotations.Query(
                name = "findByTypeAndAtPath", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.documents.dom.impl.docs.DocumentTemplate "
                        + "WHERE typeCopy   == :type "
                        + "   && atPathCopy == :atPath "
                        + "ORDER BY date DESC"
        ),
        @javax.jdo.annotations.Query(
                name = "findByTypeAndApplicableToAtPath", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.documents.dom.impl.docs.DocumentTemplate "
                        + "WHERE typeCopy   == :type "
                        + "   && :atPath.startsWith(atPathCopy) "
                        + "ORDER BY atPathCopy DESC, date DESC "
        ),
        @javax.jdo.annotations.Query(
                name = "findByApplicableToAtPath", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.documents.dom.impl.docs.DocumentTemplate "
                        + "WHERE :atPath.startsWith(atPathCopy) "
                        + "ORDER BY typeCopy ASC, atPathCopy DESC, date DESC "
        ),
        @javax.jdo.annotations.Query(
                name = "findByTypeAndApplicableToAtPathAndCurrent", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.documents.dom.impl.docs.DocumentTemplate "
                        + "WHERE typeCopy   == :type "
                        + "   && :atPath.startsWith(atPathCopy) "
                        + "   && (date == null || date <= :now) "
                        + "ORDER BY atPathCopy DESC, date DESC "
        ),
        @javax.jdo.annotations.Query(
                name = "findByType", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.documents.dom.impl.docs.DocumentTemplate "
                        + "WHERE typeCopy   == :type "
                        + "ORDER BY atPathCopy DESC, date DESC "
        ),
        @javax.jdo.annotations.Query(
                name = "findByApplicableToAtPathAndCurrent", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.incode.module.documents.dom.impl.docs.DocumentTemplate "
                        + "   && :atPath.startsWith(atPathCopy) "
                        + "   && (date == null || date <= :now) "
                        + "ORDER BY atPathCopy DESC, typeCopy, date DESC "
        )
})
@Uniques({
        @Unique(
                name = "DocumentTemplate_type_atPath_date_IDX",
                members = { "typeCopy", "atPathCopy", "date" }
        ),
})
@Indices({
        @Index(
                name = "DocumentTemplate_atPath_date_IDX",
                members = { "atPathCopy", "date" }
        ),
        @Index(
                name = "DocumentTemplate_type_date_IDX",
                members = { "typeCopy", "date" }
        ),
})
@DomainObject(
        objectType = "incodeDocuments.DocumentTemplate",
        editing = Editing.DISABLED
)
@DomainObjectLayout(
        titleUiEvent = DocumentTemplate.TitleUiEvent.class,
        iconUiEvent = DocumentTemplate.IconUiEvent.class,
        cssClassUiEvent = DocumentTemplate.CssClassUiEvent.class,
        bookmarking = BookmarkPolicy.AS_ROOT
)
public class DocumentTemplate extends DocumentAbstract<DocumentTemplate> {


    //region > ui event classes
    public static class TitleUiEvent extends DocumentsModule.TitleUiEvent<DocumentTemplate>{}
    public static class IconUiEvent extends DocumentsModule.IconUiEvent<DocumentTemplate>{}
    public static class CssClassUiEvent extends DocumentsModule.CssClassUiEvent<DocumentTemplate>{}
    //endregion

    //region > title, icon, cssClass
    /**
     * Implemented as a subscriber so can be overridden by consuming application if required.
     */
    @DomainService
    public static class TitleSubscriber extends AbstractSubscriber {

        public String getId() {
            return "incodeDocuments.DocumentTemplate$TitleSubscriber";
        }

        @EventHandler
        @Subscribe
        public void on(DocumentTemplate.TitleUiEvent ev) {
            if(ev.getTitle() != null) {
                return;
            }
            ev.setTranslatableTitle(titleOf(ev.getSource()));
        }
        private TranslatableString titleOf(final DocumentTemplate template) {
            if(template.getDate() != null) {
                return TranslatableString.tr("[{type}] {name}, (from {date})",
                        "name", template.getName(),
                        "type", template.getType().getReference(),
                        "date", template.getDate());
            } else {
                return TranslatableString.tr("[{type}] {name}",
                        "name", template.getName(),
                        "type", template.getType().getReference());
            }
        }
        @Inject
        TitleService titleService;
    }

    /**
     * Implemented as a subscriber so can be overridden by consuming application if required.
     */
    @DomainService
    public static class IconSubscriber extends AbstractSubscriber {

        public String getId() {
            return "incodeDocuments.DocumentTemplate$IconSubscriber";
        }

        @EventHandler
        @Subscribe
        public void on(DocumentTemplate.IconUiEvent ev) {
            if(ev.getIconName() != null) {
                return;
            }
            ev.setIconName("");
        }
    }

    /**
     * Implemented as a subscriber so can be overridden by consuming application if required.
     */
    @DomainService
    public static class CssClassSubscriber extends AbstractSubscriber {

        public String getId() {
            return "incodeDocuments.DocumentTemplate$CssClassSubscriber";
        }

        @EventHandler
        @Subscribe
        public void on(DocumentTemplate.CssClassUiEvent ev) {
            if(ev.getCssClass() != null) {
                return;
            }
            ev.setCssClass("");
        }
    }
    //endregion

    //region > constructor
    public DocumentTemplate(
            final DocumentType type,
            final LocalDate date,
            final String atPath,
            final String fileSuffix,
            final boolean previewOnly, final Blob blob,
            final RenderingStrategy contentRenderingStrategy,
            final String subjectText,
            final RenderingStrategy subjectRenderingStrategy) {
        super(type, atPath, blob);
        init(type, date, atPath, fileSuffix, previewOnly, contentRenderingStrategy, subjectText, subjectRenderingStrategy);
    }

    public DocumentTemplate(
            final DocumentType type,
            final LocalDate date,
            final String atPath,
            final String fileSuffix,
            final boolean previewOnly,
            final String name, final String mimeType, final String text,
            final RenderingStrategy contentRenderingStrategy,
            final String subjectText,
            final RenderingStrategy subjectRenderingStrategy) {
        super(type, atPath, name, mimeType, text);
        init(type, date, atPath, fileSuffix, previewOnly, contentRenderingStrategy, subjectText, subjectRenderingStrategy);
    }

    public DocumentTemplate(
            final DocumentType type,
            final LocalDate date,
            final String atPath,
            final String fileSuffix,
            final boolean previewOnly,
            final Clob clob,
            final RenderingStrategy contentRenderingStrategy,
            final String subjectText,
            final RenderingStrategy subjectRenderingStrategy) {
        super(type, atPath, clob);
        init(type, date, atPath, fileSuffix, previewOnly, contentRenderingStrategy, subjectText, subjectRenderingStrategy);
    }

    private void init(
            final DocumentType type,
            final LocalDate date,
            final String atPath,
            final String fileSuffix,
            final boolean previewOnly,
            final RenderingStrategy contentRenderingStrategy,
            final String subjectText,
            final RenderingStrategy subjectRenderingStrategy) {
        this.typeCopy = type;
        this.atPathCopy = atPath;
        this.date = date;
        this.fileSuffix = stripLeadingDotAndLowerCase(fileSuffix);
        this.previewOnly = previewOnly;
        this.contentRenderingStrategy = contentRenderingStrategy;
        this.subjectText = subjectText;
        this.subjectRenderingStrategy = subjectRenderingStrategy;
    }

    static String stripLeadingDotAndLowerCase(final String fileSuffix) {
        final int lastDot = fileSuffix.lastIndexOf(".");
        final String stripLeadingDot = fileSuffix.substring(lastDot+1);
        return stripLeadingDot.toLowerCase();
    }

    //endregion


    //region > typeCopy (derived property, persisted)
    /**
     * Copy of {@link #getType()}, for query purposes only.
     */
    @Getter @Setter
    @Column(allowsNull = "false", name = "typeId")
    @Property(
            notPersisted = true, // ignore for auditing
            hidden = Where.EVERYWHERE
    )
    private DocumentType typeCopy;
    //endregion

    //region > atPathCopy (derived property, persisted)
    /**
     * Copy of {@link #getAtPath()}, for query purposes only.
     */
    @Getter @Setter
    @Column(allowsNull = "false", length = DocumentsModule.JdoColumnLength.AT_PATH)
    @Property(
            notPersisted = true, // ignore for auditing
            hidden = Where.EVERYWHERE
    )
    private String atPathCopy;
    //endregion

    //region > date (property)
    public static class DateDomainEvent extends DocumentTemplate.PropertyDomainEvent<LocalDate> { }
    @Getter @Setter
    @Column(allowsNull = "false")
    @Property(
            domainEvent = DateDomainEvent.class,
            editing = Editing.DISABLED
    )
    private LocalDate date;
    //endregion


    //region > contentRenderingStrategy (property)
    public static class RenderingStrategyDomainEvent extends PropertyDomainEvent<RenderingStrategy> { }
    @Getter @Setter
    @Column(allowsNull = "false", name = "contentRenderStrategyId")
    @Property(
            domainEvent = RenderingStrategyDomainEvent.class,
            editing = Editing.DISABLED
    )
    private RenderingStrategy contentRenderingStrategy;
    //endregion

    //region > fileSuffix (property)
    public static class FileSuffixDomainEvent extends PropertyDomainEvent<String> { }
    @Getter @Setter
    @Column(allowsNull = "false", length = DocumentsModule.JdoColumnLength.FILE_SUFFIX)
    @Property(
            domainEvent = FileSuffixDomainEvent.class,
            editing = Editing.DISABLED
    )
    private String fileSuffix;
    //endregion


    //region > subjectText (persisted property)
    public static class SubjectTextDomainEvent extends PropertyDomainEvent<Clob> { }
    @Getter @Setter
    @javax.jdo.annotations.Column(allowsNull = "false", length = DocumentsModule.JdoColumnLength.SUBJECT_TEXT)
    @Property(
            notPersisted = true, // exclude from auditing
            domainEvent = SubjectTextDomainEvent.class,
            editing = Editing.DISABLED
    )
    private String subjectText;
    //endregion

    //region > subjectRenderingStrategy (property)
    public static class SubjectRenderingStrategyDomainEvent extends PropertyDomainEvent<RenderingStrategy> { }
    @Getter @Setter
    @Column(allowsNull = "false", name = "subjectRenderStrategyId")
    @Property(
            domainEvent = SubjectRenderingStrategyDomainEvent.class,
            editing = Editing.DISABLED
    )
    private RenderingStrategy subjectRenderingStrategy;
    //endregion

    //region > PreviewOnly (property)
    public static class PreviewOnlyDomainEvent extends RenderingStrategy.PropertyDomainEvent<Boolean> { }

    /**
     * Whether this template can only be previewed (not used to also create a document).
     */
    @Getter @Setter
    @Column(allowsNull = "false")
    @Property(
            domainEvent = PreviewOnlyDomainEvent.class,
            editing = Editing.DISABLED
    )
    private boolean previewOnly;
    //endregion


    //region > applicabilities (collection)
    public static class ApplicabilitiesDomainEvent extends DocumentType.CollectionDomainEvent<Applicability> {
    }

    @javax.jdo.annotations.Persistent(mappedBy = "documentTemplate", dependentElement = "true")
    @Collection(
            domainEvent = ApplicabilitiesDomainEvent.class,
            editing = Editing.DISABLED
    )
    @Getter @Setter
    private SortedSet<Applicability> appliesTo = new TreeSet<>();

    //endregion

    //region > applicable (action)
    public static class ApplicableToDomainEvent extends DocumentType.ActionDomainEvent {
    }

    @Action(
            domainEvent = ApplicableToDomainEvent.class
    )
    @ActionLayout(
            cssClassFa = "fa-plus"
    )
    @MemberOrder(name = "appliesTo", sequence = "1")
    public DocumentTemplate applicable(
            @Parameter(maxLength = DocumentsModule.JdoColumnLength.FQCN, mustSatisfy = FullyQualifiedClassNameSpecification.class)
            @ParameterLayout(named = "Domain type")
            final String domainClassName,
            @Parameter(maxLength = DocumentsModule.JdoColumnLength.FQCN, mustSatisfy = FullyQualifiedClassNameSpecification.class)
            @ParameterLayout(named = "Binder")
            final ClassNameViewModel binderClassNameViewModel) {

        applicable(domainClassName, binderClassNameViewModel.getFullyQualifiedClassName());
        return this;
    }

    @Programmatic
    public Applicability applicable(
            final Class<?> domainClass,
            final Class<? extends Binder> binderClass) {
        return applicable(domainClass.getName(), binderClass);
    }

    @Programmatic
    public Applicability applicable(
            final String domainClassName,
            final Class<? extends Binder> binderClass) {
        return applicable(domainClassName, binderClass.getName() );
    }

    @Programmatic
    public Applicability applicable(
            final String domainClassName,
            final String binderClassName) {
        return applicabilityRepository.create(this, domainClassName, binderClassName);
    }

    public TranslatableString disableApplicable() {
        return binderClassNameService == null
                ? TranslatableString.tr(
                        "No BinderClassNameService registered to locate implementations of Binder")
                : null;
    }

    public List<ClassNameViewModel> choices1Applicable() {
        return binderClassNameService.binderClassNames();
    }
    
    public TranslatableString validateApplicable(
            final String domainTypeName,
            final ClassNameViewModel dataModelFactoryClassName) {

        // REVIEW
        return isApplicable(domainTypeName) ? null :
                TranslatableString.tr(
                        "Already applicable for '{domainTypeName}'",
                        "domainTypeName", domainTypeName);
    }

    private boolean isApplicable(final String domainClassName) {
        SortedSet<Applicability> applicabilities = getAppliesTo();
        for (Applicability applicability : applicabilities) {
            if (applicability.getDomainClassName().equals(domainClassName)) {
                return false;
            }
        }
        return true;
    }
    //endregion

    //region > notApplicable (action)
    public static class NotApplicableDomainEvent extends DocumentType.ActionDomainEvent {
    }

    @Action(
            domainEvent = NotApplicableDomainEvent.class,
            semantics = SemanticsOf.IDEMPOTENT_ARE_YOU_SURE
    )
    @ActionLayout(
            cssClassFa = "fa-minus"
    )
    @MemberOrder(name = "appliesTo", sequence = "2")
    public DocumentTemplate notApplicable(final Applicability applicability) {
        applicabilityRepository.delete(applicability);
        return this;
    }

    public TranslatableString disableNotApplicable() {
        final TranslatableString tr = disableApplicable();
        if(tr != null) {
            return tr;
        }
        return choices0NotApplicable().isEmpty() ? TranslatableString.tr("No applicabilities to remove") : null;
    }

    public SortedSet<Applicability> choices0NotApplicable() {
        return getAppliesTo();
    }
    //endregion


    //region > appliesTo, newBinder, newBinding

    /**
     * Whether this template is able to return (via {@link #newBinder(Object)}) a {@link Binder}.
     */
    @Programmatic
    public boolean appliesTo(final Object domainObject) {
        return newBinder(domainObject) != null;
    }

    /**
     * Whether there is an {@link Applicability}
     * @param domainObject
     * @return
     */
    @Programmatic
    public Binder newBinder(final Object domainObject) {
        final Class<?> domainObjectClass = domainObject.getClass();
        final Optional<Applicability> applicability = getAppliesTo().stream()
                .filter(x -> classService.load(x.getDomainClassName()).isAssignableFrom(domainObjectClass))
                .findFirst();
        if (!applicability.isPresent()) {
            return null;
        }
        final Binder binder = (Binder) classService.instantiate(applicability.get().getBinderClassName());
        serviceRegistry2.injectServicesInto(binder);
        return binder;
    }

    @Programmatic
    public Binder.Binding newBinding(final Object domainObject) {
        final Binder binder = newBinder(domainObject);
        if(binder == null) {
            throw new IllegalStateException(String.format(
                    "For domain template %s, could not locate Applicability for domain object: %s",
                    getName(), domainObject.getClass().getName()));
        }
        final Binder.Binding binding = binder.newBinding(this, domainObject);
        serviceRegistry2.injectServicesInto(binding);
        return binding;
    }

    //endregion


    //region > asChars, asBytes (programmatic)
    @Programmatic
    public String asChars() {
        return getSort().asChars(this);
    }
    @Programmatic
    public byte[] asBytes() {
        return getSort().asBytes(this);
    }
    //endregion


    //region > preview (programmatic)


    @Programmatic
    public URL preview(
            final Object contentDataModel,
            final Object subjectDataModelIfAny,
            final String documentName) throws IOException {

        final Object subjectDataModel = coalesce(subjectDataModelIfAny, contentDataModel);

        serviceRegistry2.injectServicesInto(contentDataModel);
        serviceRegistry2.injectServicesInto(subjectDataModel);

        if(!getContentRenderingStrategy().isPreviewsToUrl()) {
            throw new IllegalStateException(String.format("RenderingStrategy '%s' does not support previewing to URL",
                    getContentRenderingStrategy().getReference()));
        }

        final DocumentNature inputNature = getContentRenderingStrategy().getInputNature();
        final DocumentNature outputNature = getContentRenderingStrategy().getOutputNature();

        final Renderer renderer = getContentRenderingStrategy().newRenderer();
        switch (inputNature){
        case BYTES:
            switch (outputNature) {
            case BYTES:
                return ((RendererFromBytesToBytesWithPreviewToUrl) renderer).previewBytesToBytes(
                        getType(), getAtPath(), getVersion(),
                        asBytes(), contentDataModel);
            case CHARACTERS:
                return ((RendererFromBytesToCharsWithPreviewToUrl) renderer).previewBytesToChars(
                        getType(), getAtPath(), getVersion(),
                        asBytes(), contentDataModel);
            default:
                // shouldn't happen, above switch statement is complete
                throw new IllegalArgumentException(String.format("Unknown output DocumentNature '%s'", outputNature));
            }
        case CHARACTERS:
            switch (outputNature) {
            case BYTES:
                return ((RendererFromCharsToBytesWithPreviewToUrl) renderer).previewCharsToBytes(
                        getType(), getAtPath(), getVersion(),
                        asChars(), contentDataModel);
            case CHARACTERS:
                return ((RendererFromCharsToCharsWithPreviewToUrl) renderer).previewCharsToChars(
                        getType(), getAtPath(), getVersion(),
                        asChars(), contentDataModel);
            default:
                // shouldn't happen, above switch statement is complete
                throw new IllegalArgumentException(String.format("Unknown output DocumentNature '%s'", outputNature));
            }

        default:
            // shouldn't happen, above switch statement is complete
            throw new IllegalArgumentException(String.format("Unknown input DocumentNature '%s'", inputNature));
        }
    }

    //endregion

    //region > render (programmatic)
    @Programmatic
    public DocumentAbstract render(
            final Object contentDataModel,
            final Object subjectDataModelIfAny) {

        final Object subjectDataModel = coalesce(subjectDataModelIfAny, contentDataModel);

        serviceRegistry2.injectServicesInto(contentDataModel);
        serviceRegistry2.injectServicesInto(subjectDataModel);

        final DateTime createdAt = clockService.nowAsDateTime();

        try {
            // subject
            final RendererFromCharsToChars subjectRenderer =
                    (RendererFromCharsToChars) getSubjectRenderingStrategy().newRenderer();
            final String renderedDocumentName =
                    subjectRenderer.renderCharsToChars(
                            getType(), getAtPath(), getVersion(),
                            getSubjectText(), subjectDataModel);
            final String documentName = withFileSuffix(renderedDocumentName);

            // content
            final DocumentNature inputNature = getContentRenderingStrategy().getInputNature();
            final DocumentNature outputNature = getContentRenderingStrategy().getOutputNature();

            final Renderer renderer = getContentRenderingStrategy().newRenderer();
            switch (inputNature){
            case BYTES:
                switch (outputNature) {
                case BYTES:
                    final byte[] renderedBytes = ((RendererFromBytesToBytes) renderer).renderBytesToBytes(
                            getType(), getAtPath(), getVersion(),
                            asBytes(), contentDataModel);
                    return createBlob(getType(), documentName, renderedBytes, createdAt);
                case CHARACTERS:
                    final String renderedChars = ((RendererFromBytesToChars) renderer).renderBytesToChars(
                            getType(), getAtPath(), getVersion(),
                            asBytes(), contentDataModel);
                    return createTextOrClob(getType(), documentName, renderedChars, createdAt);
                default:
                    // shouldn't happen, above switch statement is complete
                    throw new IllegalArgumentException(String.format("Unknown output DocumentNature '%s'", outputNature));
                }
            case CHARACTERS:
                switch (outputNature) {
                case BYTES:
                    final byte[] renderedBytes = ((RendererFromCharsToBytes) renderer).renderCharsToBytes(
                            getType(), getAtPath(), getVersion(),
                            asChars(), contentDataModel);
                    return createBlob(getType(), documentName, renderedBytes, createdAt);
                case CHARACTERS:
                    final String renderedChars = ((RendererFromCharsToChars) renderer).renderCharsToChars(
                            getType(), getAtPath(), getVersion(),
                            asChars(), contentDataModel);
                    return createTextOrClob(getType(), documentName, renderedChars, createdAt);
                default:
                    // shouldn't happen, above switch statement is complete
                    throw new IllegalArgumentException(String.format("Unknown output DocumentNature '%s'", outputNature));
                }
            default:
                // shouldn't happen, above switch statement is complete
                throw new IllegalArgumentException(String.format("Unknown input DocumentNature '%s'", inputNature));
            }

        } catch (IOException e) {
            throw new ApplicationException("Unable to render document template", e);
        }
    }

    private static Object coalesce(final Object x, final Object y) {
        return x != null? x: y;
    }

    private DocumentAbstract createBlob(
            final DocumentType documentType,
            final String documentName,
            final byte[] renderedBytes,
            final DateTime createdAt) {
        final Blob blob = new Blob (documentName, getMimeType(), renderedBytes);
        return documentRepository.createBlob(documentType, getAtPath(), blob, createdAt);
    }

    private DocumentAbstract createTextOrClob(
            final DocumentType documentType,
            final String documentName,
            final String renderedChars,
            final DateTime createdAt) {
        if(renderedChars.length() <= DocumentsModule.JdoColumnLength.TEXT) {
            return documentRepository.createText(
                    documentType, getAtPath(), documentName, getMimeType(), renderedChars, createdAt);
        } else {
            final Clob clob = new Clob (documentName, getMimeType(), renderedChars);
            return documentRepository.createClob(documentType, getAtPath(), clob, createdAt);
        }
    }

    //endregion

    //region > withFileSuffix (programmatic)
    @Programmatic
    public String withFileSuffix(final String documentName) {
        final String suffix = getFileSuffix();
        final int lastPeriod = suffix.lastIndexOf(".");
        final String suffixNoDot = suffix.substring(lastPeriod + 1);
        final String suffixWithDot = "." + suffixNoDot;
        if (documentName.endsWith(suffixWithDot)) {
            return trim(documentName, DocumentsModule.JdoColumnLength.DOC_NAME);
        }
        else {
            return trim(documentName, DocumentsModule.JdoColumnLength.DOC_NAME - suffixWithDot.length()) + suffixWithDot;
        }
    }

    private static String trim(final String name, final int length) {
        return name.length() > length ? name.substring(0, length) : name;
    }
    //endregion




    //region > getVersion (programmatic)
    @Programmatic
    private long getVersion() {
        return (Long)JDOHelper.getVersion(this);
    }

    //endregion

    //region > injected services
    @Inject
    PaperclipRepository paperclipRepository;
    @Inject
    DocumentTemplateRepository documentTemplateRepository;
    @Inject
    BinderClassNameService binderClassNameService;
    @Inject
    ApplicabilityRepository applicabilityRepository;
    @Inject
    QueryResultsCache queryResultsCache;
    @Inject
    ClassService classService;
    @Inject
    ServiceRegistry2 serviceRegistry2;
    @Inject
    private ClockService clockService;
    //endregion

}