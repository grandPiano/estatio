/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
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
package org.estatio.module.asset.dom;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.PromptStyle;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.RenderType;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.schema.utils.jaxbadapters.PersistentEntityAdapter;

import org.incode.module.base.dom.types.NameType;
import org.incode.module.base.dom.types.ReferenceType;
import org.incode.module.base.dom.utils.TitleBuilder;
import org.incode.module.base.dom.valuetypes.LocalDateInterval;
import org.incode.module.base.dom.with.WithNameComparable;
import org.incode.module.base.dom.with.WithReferenceUnique;
import org.incode.module.communications.dom.impl.commchannel.CommunicationChannelOwner;

import org.estatio.module.asset.dom.ownership.FixedAssetOwnership;
import org.estatio.module.asset.dom.ownership.FixedAssetOwnershipRepository;
import org.estatio.module.asset.dom.role.FixedAssetRole;
import org.estatio.module.asset.dom.role.FixedAssetRoleRepository;
import org.estatio.module.asset.dom.role.FixedAssetRoleTypeEnum;
import org.estatio.module.base.dom.EstatioRole;
import org.estatio.module.base.dom.UdoDomainObject2;
import org.estatio.module.party.dom.Party;

import lombok.Getter;
import lombok.Setter;

@javax.jdo.annotations.PersistenceCapable(
        identityType = IdentityType.DATASTORE
        ,schema = "dbo" // Isis' ObjectSpecId inferred @Discriminator
)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version"
)
@javax.jdo.annotations.Discriminator(
        strategy = DiscriminatorStrategy.VALUE_MAP,
        column = "discriminator",
        value = "org.estatio.dom.asset.FixedAsset"
)
@javax.jdo.annotations.Uniques({
        @javax.jdo.annotations.Unique(
                name = "FixedAsset_reference_UNQ", members = { "reference" })
})
@javax.jdo.annotations.Indices({
        // to cover the 'findAssetsByReferenceOrName' query
        // both in this superclass and the subclasses
        @javax.jdo.annotations.Index(
                name = "FixedAsset_reference_name_IDX", members = { "reference", "name" })
})
@javax.jdo.annotations.Queries({
        @javax.jdo.annotations.Query(
                name = "matchByReferenceOrName", language = "JDOQL",
                value = "SELECT FROM org.estatio.module.asset.dom.FixedAsset "
                        + "WHERE reference.matches(:regex) "
                        + "|| name.matches(:regex) ")
})
@DomainObject(
        editing = Editing.DISABLED,
        autoCompleteRepository = FixedAssetRepository.class,
        autoCompleteAction = "autoComplete"
)
@DomainObjectLayout(bookmarking = BookmarkPolicy.AS_ROOT)
@XmlJavaTypeAdapter(PersistentEntityAdapter.class)
public abstract class FixedAsset<X extends FixedAsset<X>>
        extends UdoDomainObject2<X>
        implements WithNameComparable<X>, WithReferenceUnique, CommunicationChannelOwner {

    public FixedAsset() {
        super("name");
    }

    public String title() {
        return TitleBuilder.start()
                .withReference(getReference())
                .withName(getName())
                .toString();
    }

    @Inject
    FixedAssetRoleRepository fixedAssetRoleRepository;

    // //////////////////////////////////////

    @javax.jdo.annotations.Column(allowsNull = "false", length = ReferenceType.Meta.MAX_LEN)
    @Property(regexPattern = ReferenceType.Meta.REGEX)
    @PropertyLayout(describedAs = "Unique reference code for this asset")
    @Getter @Setter
    private String reference;

    // //////////////////////////////////////

    // /**
    // * Although both {@link Property} and {@link Unit} (the two subclasses)
    // have
    // * a name, they are mapped separately because they have different
    // uniqueness
    // * constraints.
    // *
    // * <p>
    // * For {@link Property}, the {@link Property#getName() name} by itself is
    // unique.
    // *
    // * <p>
    // * For {@link Unit}, the combination of ({@link Unit#getProperty()
    // property}, {@link Unit#getName() name})
    // * is unique.
    // */
    // public abstract String getName();

    @javax.jdo.annotations.Column(allowsNull = "false", length = NameType.Meta.MAX_LEN)
    @Property(editing = Editing.ENABLED)
    @PropertyLayout(promptStyle = PromptStyle.INLINE)
    @Getter @Setter
    private String name;

    public void modifyName(final String name) {
        setName(name);
    }

    // //////////////////////////////////////

    @javax.jdo.annotations.Column(allowsNull = "true", length = ReferenceType.Meta.MAX_LEN)
    @Property(optionality = Optionality.OPTIONAL, editing = Editing.ENABLED)
    @PropertyLayout(promptStyle = PromptStyle.INLINE)
    @Getter @Setter
    private String externalReference;

    @MemberOrder(name = "externalReference", sequence = "1")
    public void modifyExternalReference(final String externalReference) {
        setExternalReference(externalReference);
    }

    public String disableExternalReference(){
        return !EstatioRole.SUPERUSER.isApplicableFor(getUser()) ? "Only Superusers can change existing external references" : null ;
    }

    // //////////////////////////////////////

    @javax.jdo.annotations.Persistent(mappedBy = "fixedAsset")
    @CollectionLayout(defaultView = "table")
    @Getter @Setter
    private SortedSet<FixedAssetOwnership> owners = new TreeSet<>();

    @MemberOrder(name = "owners", sequence = "1")
    public FixedAsset addOwner(final Party newOwner, final OwnershipType type) {
        FixedAssetOwnership fixedAssetOwnership = fixedAssetOwnershipRepository.newOwnership(newOwner, type, this);
        getOwners().add(fixedAssetOwnership);
        return this;
    }

    public List<Party> choices0AddOwner() {
        return ownerCandidates().stream().map(FixedAssetRole::getParty).collect(Collectors.toList());
    }

    @Programmatic
    public List<FixedAssetRole> ownerCandidates() {
        return fixedAssetRoleRepository.findByAssetAndType(this, FixedAssetRoleTypeEnum.PROPERTY_OWNER);
    }

    public String validateAddOwner(final Party newOwner, final OwnershipType type) {
        if (getOwners().stream().filter(owner -> owner.getOwner().equals(newOwner)).toArray().length > 0) {
            return "This owner already has its share defined";
        } else if (type.equals(OwnershipType.FULL) && getOwners().size() != 0 && (getOwners().stream().filter(owner -> owner.getOwnershipType().equals(OwnershipType.FULL)).toArray().length == 0)) {
            return "This owner can not be a full owner as there is already a shared owner defined";
        } else {
            return null;
        }
    }

    public String disableAddOwner() {
        if (getOwners().stream().filter(owner -> owner.getOwnershipType().equals(OwnershipType.FULL)).toArray().length > 0) {
            return "This property is fully owned. To add another owner, first change the ownership type of the current owner to 'shared'";
        } else {
            return null;
        }
    }

    @Action(hidden = Where.ALL_TABLES)
    public boolean getFullOwnership() {
        final SortedSet<FixedAssetOwnership> owners = getOwners();
        return (owners.size() == 1 && owners.first().getOwnershipType().equals(OwnershipType.FULL));
    }

    // //////////////////////////////////////

    @CollectionLayout(render = RenderType.EAGERLY)
    @javax.jdo.annotations.Persistent(mappedBy = "asset")
    @Getter @Setter
    private SortedSet<FixedAssetRole> roles = new TreeSet<>();

    public FixedAsset newRole(
            final FixedAssetRoleTypeEnum type,
            final Party party,
            @Nullable
            final LocalDate startDate,
            @Nullable
            final LocalDate endDate) {
        createRole(type, party, startDate, endDate);
        return this;
    }

    public String validateNewRole(
            final FixedAssetRoleTypeEnum type,
            final Party party,
            final LocalDate startDate,
            final LocalDate endDate) {
        List<FixedAssetRole> currentRoles = fixedAssetRoleRepository.findAllForPropertyAndPartyAndType(this, party, type);
        for (FixedAssetRole fixedAssetRole : currentRoles) {
            LocalDateInterval existingInterval = new LocalDateInterval(fixedAssetRole.getStartDate(), fixedAssetRole.getEndDate());
            LocalDateInterval newInterval = new LocalDateInterval(startDate, endDate);
            if (existingInterval.overlaps(newInterval)) {
                return "The provided dates overlap with a current role of this type and party";
            }
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return "End date cannot be earlier than start date";
        }

        return null;
    }

    @Programmatic
    public FixedAssetRole createRole(
            final FixedAssetRoleTypeEnum type, final Party party, final LocalDate startDate, final LocalDate endDate) {
        final FixedAssetRole role = newTransientInstance(FixedAssetRole.class);
        role.setStartDate(startDate);
        role.setEndDate(endDate);
        role.setType(type); // must do before associate with agreement, since
        // part of AgreementRole#compareTo impl.

        // JDO will manage the relationship for us
        // see http://markmail.org/thread/b6lpzktr6hzysisp, Dan's email
        // 2013-7-17
        role.setParty(party);
        role.setAsset(this);

        persistIfNotAlready(role);

        return role;
    }

    @Inject
    FixedAssetOwnershipRepository fixedAssetOwnershipRepository;
}
