/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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

import java.math.BigDecimal;
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
import org.n52.io.response.ProcedureOutput;
import org.n52.sensorweb.server.db.TestRepositories;
import org.n52.sensorweb.server.db.assembler.core.ProcedureAssembler;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.ProcedureRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.Describable;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.beans.i18n.I18nEntity;
import org.n52.series.db.beans.i18n.I18nProcedureEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.db.beans.parameter.feature.FeatureQuantityParameterEntity;
import org.n52.series.db.beans.parameter.procedure.ProcedureQuantityParameterEntity;
import org.n52.shetland.ogc.OGCConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ProcedureAssemblerTest extends AbstractAssemblerTest {

    @Autowired
    private TestRepositories testRepositories;

    @Autowired
    private ProcedureRepository repository;

    @Autowired
    private ProcedureAssembler assembler;

    @Test
    @DisplayName("Procedure of non-public dataset is not found")
    public void given_aNonPublicDataset_when_queryingProcedures_then_procedureIsNotPartOfCollection() {
        final DatasetEntity dataset = quantityDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        dataset.setPublished(false);

        Assertions.assertAll("Procedure is not part of collection", () -> {
            assertThat(assembler.getAllCondensed(defaultQuery)).isEmpty();
        });

        Assertions.assertAll("Procedure id does not exist", () -> {
            final String id = getIdAsString(dataset.getProcedure());
            assertThat(assembler.exists(id, defaultQuery)).isFalse();
            assertThat(assembler.getInstance(id, defaultQuery)).isNull();
        });

        Assertions.assertAll("Procedure identifier does not exist", () -> {
            final DbQuery matchDomainIds = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString());
            assertThat(assembler.exists("proc", matchDomainIds)).isFalse();
            assertThat(assembler.getInstance("proc", matchDomainIds)).isNull();
        });
    }

    @Test
    @DisplayName("Filtering works properly")
    public void given_publicDatasets_when_filteringViaParameters_then_outputContainsMatchingProcedures() {
        quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "format2");
        quantityDataset("ph1", "of2", "pr2", "format3", "fe2", "format4");
        quantityDataset("ph2", "of3", "pr2", "format3", "fe2", "format4");

        Assertions.assertAll("Procedures with matching (by Domain ID) Phenomena filters", () -> {
            final DbQuery ph1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.PHENOMENA, "ph1");
            final List<ProcedureOutput> procedures = assembler.getAllCondensed(ph1Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId).anyMatch(it -> it.equals("pr1"))
                    .anyMatch(it -> it.equals("pr2")).noneMatch(it -> it.equals("pr3"));
        });

        Assertions.assertAll("Procedures with matching (by Domain ID) Procedure filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.PROCEDURES, "pr1");

            List<ProcedureOutput> procedures = assembler.getAllCondensed(pr1Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId).anyMatch(it -> it.equals("pr1"))
                    .noneMatch(it -> it.equals("pr2")).noneMatch(it -> it.equals("pr3"));
        });

        Assertions.assertAll("Procedures with matching (by Domain ID) Feature filters", () -> {
            DbQuery fe2Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.FEATURES, "fe2");

            List<ProcedureOutput> procedures = assembler.getAllCondensed(fe2Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId).noneMatch(it -> it.equals("pr1"))
                    .anyMatch(it -> it.equals("pr2")).noneMatch(it -> it.equals("pr3"));
        });

        Assertions.assertAll("Procedures with matching (by Domain ID) Offerings filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                    .replaceWith(Parameters.OFFERINGS, "of1,of2");

            List<ProcedureOutput> procedures = assembler.getAllCondensed(pr1Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId).anyMatch(it -> it.equals("pr1"))
                    .anyMatch(it -> it.equals("pr2")).noneMatch(it -> it.equals("pr3"));
        });

        // TODO: Test Services Filter
        // TODO: Test Categories Filter
        // TODO: Test Platforms Filter
        // TODO: Test Stations Filter
        // TODO: Test platformTypes Filter
        // TODO: Test valueTypes Filter
    }

    @Test
    @DisplayName("Procedure output assembled properly")
    public void given_validDataset_when_queryingProcedure_then_outputGetsAssembledProperly() {

        final String formatIdentifier = "TestFormat";
        final String procedureIdentifier = "TestProcedure";
        final String procedureLabel = "TestLabel";

        ProcedureEntity procedure = testRepositories.upsertSimpleProcedure(procedureIdentifier, formatIdentifier);
        procedure.setIdentifier(procedureIdentifier);
        procedure.setName(procedureLabel);
        testRepositories.save(procedure);

        final DatasetEntity dataset =
                quantityDataset("phen", "off", procedureIdentifier, "sml", "feat", formatIdentifier);

        final String expectedId = Long.toString(procedure.getId());

        final DbQuery query = defaultQuery.replaceWith(HREF_BASE, "https://foo.com/");
        Assertions.assertAll("Assert members of serialized output assemble", () -> {
            final List<ProcedureOutput> procedures = assembler.getAllCondensed(query);
            assertThat(procedures).element(0).returns(expectedId, ProcedureOutput::getId)
                    .returns(procedureIdentifier, ProcedureOutput::getDomainId)
                    .returns("https://foo.com/procedures/" + expectedId, ProcedureOutput::getHref);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Condensed)", () -> {
            List<ProcedureOutput> procedures = assembler.getAllCondensed(defaultQuery);

            ListAssert<ProcedureOutput> element = assertThat(procedures);
            element.extracting(ProcedureOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(ProcedureOutput::getDomainId).allMatch(it -> it.equals(procedureIdentifier));
            element.extracting(ProcedureOutput::getLabel).allMatch(it -> it.equals(procedureLabel));

            // Does not return unserialized fields
            element.extracting(ProcedureOutput::getExtras).allMatch(it -> it == null);
            element.extracting(ProcedureOutput::getService).allMatch(it -> it == null);
            element.extracting(ProcedureOutput::getHref).allMatch(it -> it != null);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Expanded)", () -> {
            List<ProcedureOutput> procedures = assembler.getAllExpanded(defaultQuery);

            ListAssert<ProcedureOutput> element = assertThat(procedures);
            element.extracting(ProcedureOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(ProcedureOutput::getDomainId).allMatch(it -> it.equals(procedureIdentifier));
            element.extracting(ProcedureOutput::getLabel).allMatch(it -> it.equals(procedureLabel));
            // TODO: Check if getExtras is supposed to return null or empty collection
            element.extracting(ProcedureOutput::getExtras).allMatch(it -> it == null);

            element.extracting(ProcedureOutput::getService)
                    .allMatch(it -> it.getLabel().equals("TestService") && it.getId().equals("42"));
        });
    }

    @Test
    public void insert_procedure_with_simple_parameter() {
        final String formatIdentifier = "TestParamFormat";
        final String procedureIdentifier = "TestParamProcedure";
        final String procedureLabel = "TestParamLabel";

        final FormatEntity formatEntity = new FormatEntity();
        formatEntity.setFormat(formatIdentifier);
        ProcedureEntity entity = new ProcedureEntity();
        entity.setFormat(formatEntity);
        entity.setIdentifier(procedureIdentifier);
        entity.setName(procedureLabel);

        ProcedureQuantityParameterEntity parameter = new ProcedureQuantityParameterEntity();
        parameter.setName("param_name");
        parameter.setValue(new BigDecimal("1.0"));
        parameter.setProcedure(entity);
        UnitEntity unit = new UnitEntity();
        unit.setIdentifier("m");
        parameter.setUnit(unit);
        entity.addParameter(parameter);

        ProcedureEntity inserted = assembler.getOrInsertInstance(entity);
        repository.flush();
        Assertions.assertNotNull(inserted);

        ProcedureEntity procedure = repository.getReferenceById(inserted.getId());
        Assertions.assertNotNull(procedure);
        Assertions.assertTrue(procedure.hasParameters());

        Assertions.assertEquals(1, procedure.getParameters().size());
        ParameterEntity<?> param = procedure.getParameters().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof ProcedureQuantityParameterEntity);
        Assertions.assertNotNull(((ProcedureQuantityParameterEntity) param).getUnit());

        repository.delete(entity);
        repository.flush();
        Optional<ProcedureEntity> deleted = repository.findById(procedure.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }

    @Test
    public void insert_procedure_with_translation() {
        final String formatIdentifier = "TestFormat";
        final String procedureIdentifier = "TestProcedure";
        final String procedureLabel = "TestLabel";

        ProcedureEntity entity = testRepositories.upsertSimpleProcedure(procedureIdentifier, formatIdentifier);
        entity.setIdentifier(procedureIdentifier);
        entity.setName(procedureLabel);

        I18nProcedureEntity i18n = new I18nProcedureEntity();
        i18n.setLocale("en");
        i18n.setEntity(entity);
        i18n.setName("test");
        Set<I18nEntity<? extends Describable>> i18ns = new LinkedHashSet<>();
        i18ns.add(i18n);
        entity.setTranslations(i18ns);

        ProcedureEntity inserted = assembler.getOrInsertInstance(entity);
        repository.flush();
        Assertions.assertNotNull(inserted);

        ProcedureEntity procedure = repository.getReferenceById(inserted.getId());
        Assertions.assertNotNull(procedure);
        Assertions.assertTrue(procedure.hasTranslations());

        Assertions.assertEquals(1, procedure.getTranslations().size());
        I18nEntity<? extends Describable> param = procedure.getTranslations().iterator().next();
        Assertions.assertNotNull(param);
        Assertions.assertTrue(param instanceof I18nProcedureEntity);

        repository.delete(entity);
        repository.flush();
        Optional<ProcedureEntity> deleted = repository.findById(procedure.getId());
        Assertions.assertNotNull(deleted);
        Assertions.assertFalse(deleted.isPresent());
    }
}
