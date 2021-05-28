/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.n52.io.request.Parameters.FEATURES;
import static org.n52.io.request.Parameters.OFFERINGS;
import static org.n52.io.request.Parameters.PHENOMENA;
import static org.n52.io.request.Parameters.PROCEDURES;
import static org.n52.sensorweb.server.test.TestUtils.fromWkt;
import static org.n52.sensorweb.server.test.TestUtils.getIdAsString;
import static org.n52.sensorweb.server.test.TestUtils.toList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.io.ParseException;
import org.n52.io.request.Parameters;
import org.n52.sensorweb.server.db.TestBase;
import org.n52.sensorweb.server.db.TestRepositories;
import org.n52.sensorweb.server.db.TestRepositoryConfig;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class DatasetQuerySpecificationsTest extends TestBase {

    @Autowired
    private DatasetRepository datasetRepository;

    @Test
    @DisplayName("Dataset without a feature is not found")
    public void given_aDatasetWithoutFeature_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1");
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "dataset without feature found");
    }

    @Test
    @DisplayName("Unpublished dataset is not found")
    public void given_anUnpublishedDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1").setPublished(false);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "non public dataset found");
    }

    @Test
    @DisplayName("Disabled dataset is not found")
    public void given_aDisabledDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1").setDisabled(true);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "disabled dataset found");
    }

    @Test
    @DisplayName("Deleted dataset are not found")
    public void given_aDeletedDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1").setDeleted(true);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "deleted dataset found");
    }

    @Test
    @DisplayName("Filter datasets via offering ids")
    public void given_multipleDatasets_when_noFilter_then_returnAllDatasets() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1");
        final DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2");
        final DatasetEntity d3 = quantityDataset("ph1", "of2", "pr2", "format2");
        assertThat(datasetRepository.findAll()).containsAll(toList(d1, d2, d3));
    }

    @Test
    @DisplayName("Filter value types")
    public void given_textDatasets_when_queryingWithDefault_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = textDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        Assertions.assertAll("Return all by default", () -> {
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(defaultQuery);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1, d2);
        });

        Assertions.assertAll("'all' is not a valid filter", () -> {
            final DbQuery query = defaultQuery.replaceWith(Parameters.FILTER_VALUE_TYPES, "all");
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });

        Assertions.assertAll("Return text datasets only", () -> {
            final DbQuery query = defaultQuery.replaceWith(Parameters.FILTER_VALUE_TYPES, "text");
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d2);
        });

    }

    @Test
    @DisplayName("Filter platform types")
    public void given_mobileDatasets_when_queryingWithDefault_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = quantityDataset("ph1", "of1", "pr2", "format1", "fe1", "fe_format");
        final ProcedureEntity p2 = d2.getProcedure();
        d2.setMobile(true);

        // assert test requirement
        assertThat(d1.getProcedure()).as("Datasets share the same procedure").isNotSameAs(d2.getProcedure());

//        Assertions.assertAll("Empty Query matches all", () -> {
//            final DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(defaultQuery);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes())).containsOnly(d1, d2);
//        });
//
//        Assertions.assertAll("Query matches 'stationary' datasets", () -> {
//            final DbQuery query = defaultQuery.replaceWith(FILTER_PLATFORM_TYPES, PLATFORM_TYPE_STATIONARY);
//            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes())).containsOnly(d1);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes(PLATFORM_TYPE_STATIONARY))).containsOnly(d1);
//        });
//
//        Assertions.assertAll("Default query does not match 'mobile' datasets", () -> {
//            final DbQuery query = defaultQuery.replaceWith(FILTER_PLATFORM_TYPES, PLATFORM_TYPE_MOBILE);
//            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes())).containsOnly(d2);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes(PLATFORM_TYPE_MOBILE))).containsOnly(d2);
//        });
//
//        Assertions.assertAll("Default query does match 'remote' datasets ", () -> {
//            d2.setInsitu(false);
//            final DbQuery query = defaultQuery.replaceWith(FILTER_PLATFORM_TYPES, PLATFORM_TYPE_REMOTE);
//            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes())).containsOnly(d2);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes(PLATFORM_TYPE_REMOTE))).containsOnly(d2);
//        });
//
//        Assertions.assertAll("Default query does match 'mobile/remote' datasets ", () -> {
//            d2.setInsitu(false);
//            d2.setMobile(true);
//            final DbQuery query = defaultQuery.replaceWith(FILTER_PLATFORM_TYPES, PLATFORM_TYPE_MOBILE, PLATFORM_TYPE_REMOTE);
//            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes())).containsOnly(d2);
//            assertThat(datasetRepository.findAll(filterSpec.matchPlatformTypes(PLATFORM_TYPE_MOBILE, PLATFORM_TYPE_REMOTE))).containsOnly(d2);
//        });
    }

    @Test
    @DisplayName("Filter datasets via offering ids")
    public void given_multipleDatasets_when_queryingWithOfferingList_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        final String id1 = getIdAsString(d1.getOffering());
        final String id2 = getIdAsString(d2.getOffering());

        Assertions.assertAll("Offering filter matches one dataset", () -> {
            final DbQuery query = defaultQuery.replaceWith(OFFERINGS, id1);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Offering filter matches all datasets", () -> {
            final DbQuery query = defaultQuery.replaceWith(OFFERINGS, id1, id2);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Offering filter matches no dataset", () -> {
            final String notExistingId = "42";
            final DbQuery query = defaultQuery.replaceWith(OFFERINGS, notExistingId);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @DisplayName("Filter datasets via procedure ids")
    public void given_multipleDatasets_when_queryingWithProcedureList_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        final String id1 = getIdAsString(d1.getProcedure());
        final String id2 = getIdAsString(d2.getProcedure());

        Assertions.assertAll("Procedure filter matches one dataset", () -> {
            final DbQuery query = defaultQuery.replaceWith(PROCEDURES, id1);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Procedure filter matches all datasets", () -> {
            final DbQuery query = defaultQuery.replaceWith(PROCEDURES, id1, id2);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Procedure filter matches no dataset", () -> {
            final String notExistingId = "42";
            final DbQuery query = defaultQuery.replaceWith(PROCEDURES, notExistingId);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @DisplayName("Filter datasets via phenomenon ids")
    public void given_multipleDatasets_when_queryingWithPhenomenonList_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        final String id1 = getIdAsString(d1.getPhenomenon());
        final String id2 = getIdAsString(d2.getPhenomenon());

        Assertions.assertAll("Phenomenon filter matches one dataset", () -> {
            final DbQuery query = defaultQuery.replaceWith(PHENOMENA, id1);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Phenomenon filter matches all datasets", () -> {
            final DbQuery query = defaultQuery.replaceWith(PHENOMENA, id1, id2);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Phenomenon filter matches no dataset", () -> {
            final String notExistingId = "42";
            final DbQuery query = defaultQuery.replaceWith(PHENOMENA, notExistingId);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @DisplayName("Filter datasets via feature ids")
    public void given_multipleDatasets_when_queryingWithFeatureList_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format3", "fe2", "fe_format");

        final String id1 = getIdAsString(d1.getFeature());
        final String id2 = getIdAsString(d2.getFeature());

        Assertions.assertAll("Feature filter matches one dataset", () -> {
            final DbQuery query = defaultQuery.replaceWith(FEATURES, id1);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Feature filter matches all datasets", () -> {
            final DbQuery query = defaultQuery.replaceWith(FEATURES, id1, id2);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Feature filter matches no dataset", () -> {
            final String notExistingId = "42";
            final DbQuery query = defaultQuery.replaceWith(FEATURES, notExistingId);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @DisplayName("Filter datasets via mixed parameter ids")
    public void given_multipleDatasets_when_queryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly() {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        final DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        final String offId1 = getIdAsString(d1.getOffering());
        final String offId2 = getIdAsString(d2.getOffering());

        Assertions.assertAll("Mixed filter with existing parameters", () -> {
            final String phId1 = getIdAsString(d1.getPhenomenon());
            final String phId2 = getIdAsString(d2.getPhenomenon());
            final DbQuery query = defaultQuery.replaceWith(PHENOMENA, phId1, phId2)
                                        .replaceWith(OFFERINGS, offId1, offId2);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Matching nothing at all", () -> {
            final String notExistingId = "42";
            final DbQuery query = defaultQuery.replaceWith(PHENOMENA, notExistingId)
                                        .replaceWith(OFFERINGS, offId1, offId2);
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @DisplayName("Filter datasets via bbox")
    public void given_datasetsWithGeometries_when_queryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly()
            throws ParseException {
        final DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1");
        final FeatureEntity f1 = testRepositories.persistSimpleFeature("f1", "format1");
        f1.setGeometry(fromWkt("SRID=4326;POINT (7.3 52.8)"));
        d1.setFeature(f1);

        Assertions.assertAll("No match when laying outside bbox", () -> {
            final DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "5, 50, 6, 51");
            final DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query, entityManager);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });

        final DatasetEntity d2 = quantityDataset("ph1", "of1", "pr1", "format1");
        final FeatureEntity f2 = testRepositories.persistSimpleFeature("f2", "format1");
        f2.setGeometry(fromWkt("SRID=4326;POINT (7.2 55)"));
        d2.setFeature(f2);

        Assertions.assertAll("Match when laying inside bbox", () -> {
            final DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "7, 52, 7.5, 53");
            final DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query, entityManager);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).hasSize(1);
        });
    }

    private

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = DatasetRepository.class)
    static class Config extends TestRepositoryConfig<DatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories testRepositories() {
            return new TestRepositories();
        }
    }
}
