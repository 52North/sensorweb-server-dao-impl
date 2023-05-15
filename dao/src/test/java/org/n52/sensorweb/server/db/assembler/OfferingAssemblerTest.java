/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.sensorweb.server.db.assembler;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.n52.io.request.Parameters.HREF_BASE;
import static org.n52.io.request.Parameters.MATCH_DOMAIN_IDS;
import static org.n52.sensorweb.server.test.TestUtils.getIdAsString;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.request.Parameters;
import org.n52.io.response.OfferingOutput;
import org.n52.sensorweb.server.db.TestRepositories;
import org.n52.sensorweb.server.db.assembler.core.OfferingAssembler;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.OfferingRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.Describable;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.i18n.I18nEntity;
import org.n52.series.db.beans.i18n.I18nOfferingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingAssemblerTest extends AbstractAssemblerTest {

    @Autowired
    private TestRepositories testRepositories;

    @Autowired
    private OfferingRepository repository;

    @Autowired
    private OfferingAssembler assembler;

    @Test
    @DisplayName("Offering of non-public dataset is not found")
    public void given_aNonPublicDataset_when_queryingOfferings_then_offeringIsNotPartOfCollection() {
        final DatasetEntity dataset = quantityDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        dataset.setPublished(false);

        Assertions.assertAll("Offering is not part of collection", () -> {
            assertThat(assembler.getAllCondensed(defaultQuery)).isEmpty();
        });

        Assertions.assertAll("Offering id does not exist", () -> {
            final String id = getIdAsString(dataset.getOffering());
            assertThat(assembler.exists(id, defaultQuery)).isFalse();
            assertThat(assembler.getInstance(id, defaultQuery)).isNull();
        });

        Assertions.assertAll("Offering identifier does not exist", () -> {
            final DbQuery matchDomainIds = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString());
            assertThat(assembler.exists("off", matchDomainIds)).isFalse();
            assertThat(assembler.getInstance("off", matchDomainIds)).isNull();
        });
    }

    @Test
    @DisplayName("Filtering works properly")
    public void given_publicDatasets_when_filteringViaParameters_then_outputContainsMatchingOfferings() {
        quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "format2");
        quantityDataset("ph1", "of2", "pr2", "format3", "fe2", "format4");
        quantityDataset("ph2", "of3", "pr2", "format3", "fe2", "format4");

        Assertions.assertAll("Offerings with matching (by Domain ID) Phenomena filters", () -> {
            final DbQuery ph1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.PHENOMENA, "ph1");
            final List<OfferingOutput> offerings = assembler.getAllCondensed(ph1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId).anyMatch(it -> it.equals("of1"))
                    .anyMatch(it -> it.equals("of2")).noneMatch(it -> it.equals("of3"));
        });

        Assertions.assertAll("Offerings with matching (by Domain ID) Procedure filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.PROCEDURES, "pr1");

            List<OfferingOutput> offerings = assembler.getAllCondensed(pr1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId).anyMatch(it -> it.equals("of1"))
                    .noneMatch(it -> it.equals("of2")).noneMatch(it -> it.equals("of3"));
        });

        Assertions.assertAll("Offerings with matching (by Domain ID) Feature filters", () -> {
            DbQuery fe2Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.FEATURES, "fe2");

            List<OfferingOutput> offerings = assembler.getAllCondensed(fe2Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId).noneMatch(it -> it.equals("of1"))
                    .anyMatch(it -> it.equals("of2")).anyMatch(it -> it.equals("of3"));
        });

        Assertions.assertAll("Offerings with matching (by Domain ID) Offerings filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.OFFERINGS, "of1,of2");

            List<OfferingOutput> offerings = assembler.getAllCondensed(pr1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId).anyMatch(it -> it.equals("of1"))
                    .anyMatch(it -> it.equals("of2")).noneMatch(it -> it.equals("of3"));
        });

        // TODO: Test Services Filter
        // TODO: Test Categories Filter
        // TODO: Test Platforms Filter
        // TODO: Test Stations Filter
        // TODO: Test platformTypes Filter
        // TODO: Test valueTypes Filter
    }

    @Test
    @DisplayName("Offering output assembled properly")
    public void given_validDataset_when_queryingOffering_then_outputGetsAssembledProperly() {

        final String offeringIdentifier = "off";
        final String offeringLabel = "TestLabel";

        final OfferingEntity offering = new OfferingEntity();
        offering.setIdentifier(offeringIdentifier);
        offering.setName(offeringLabel);
        testRepositories.save(offering);
        final DatasetEntity dataset = textDataset("phen", offeringIdentifier, "proc", "sml", "feat", "featFormat");

        final String expectedId = Long.toString(offering.getId());

        final DbQuery query = defaultQuery.replaceWith(HREF_BASE, "https://foo.com/");
        Assertions.assertAll("Assert members of serialized output assemble", () -> {
            final List<OfferingOutput> offerings = assembler.getAllCondensed(query);
            assertThat(offerings).element(0).returns(expectedId, OfferingOutput::getId)
                    .returns("off", OfferingOutput::getDomainId)
                    .returns("https://foo.com/offerings/" + expectedId, OfferingOutput::getHref);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Condensed)", () -> {
            List<OfferingOutput> offerings = assembler.getAllCondensed(defaultQuery);

            ListAssert<OfferingOutput> element = assertThat(offerings);
            element.extracting(OfferingOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(OfferingOutput::getDomainId).allMatch(it -> it.equals(offeringIdentifier));
            element.extracting(OfferingOutput::getLabel).allMatch(it -> it.equals(offeringLabel));

            // Does not return unserialized fields
            element.extracting(OfferingOutput::getExtras).allMatch(it -> it == null);
            element.extracting(OfferingOutput::getService).allMatch(it -> it == null);
            element.extracting(OfferingOutput::getHref).allMatch(it -> it != null);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Expanded)", () -> {
            List<OfferingOutput> offerings = assembler.getAllExpanded(defaultQuery);

            ListAssert<OfferingOutput> element = assertThat(offerings);
            element.extracting(OfferingOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(OfferingOutput::getDomainId).allMatch(it -> it.equals(offeringIdentifier));
            element.extracting(OfferingOutput::getLabel).allMatch(it -> it.equals(offeringLabel));
            // TODO: Check if getExtras is supposed to return null or empty collection
            element.extracting(OfferingOutput::getExtras).allMatch(it -> it == null);

            element.extracting(OfferingOutput::getService)
                    .allMatch(it -> it.getLabel().equals("TestService") && it.getId().equals("42"));
        });
    }

    @Test
    public void insert_offering_with_translation() {
        final String offeringIdentifier = "off";
        final String offeringLabel = "TestLabel";

        final OfferingEntity entity = new OfferingEntity();
        entity.setIdentifier(offeringIdentifier);
        entity.setName(offeringLabel);

        I18nOfferingEntity i18n = new I18nOfferingEntity();
        i18n.setLocale("en");
        i18n.setEntity(entity);
        i18n.setName("test");
        Set<I18nEntity<? extends Describable>> i18ns = new LinkedHashSet<>();
        i18ns.add(i18n);
        entity.setTranslations(i18ns);

        OfferingEntity inserted = assembler.getOrInsertInstance(entity);
        repository.flush();
        Assertions.assertNotNull(inserted);

        OfferingEntity offering = repository.getReferenceById(inserted.getId());
        Assertions.assertNotNull(offering);
        Assertions.assertTrue(offering.hasTranslations());

        Assertions.assertEquals(1, offering.getTranslations().size());
        I18nEntity<? extends Describable> param = offering.getTranslations().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof I18nOfferingEntity);

        repository.delete(entity);
        repository.flush();
        Optional<OfferingEntity> deleted = repository.findById(offering.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }

}
