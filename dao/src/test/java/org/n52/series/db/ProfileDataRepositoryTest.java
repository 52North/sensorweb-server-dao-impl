/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
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
package org.n52.series.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.n52.io.request.Parameters.FILTER_VALUE_TYPES;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DataQuerySpecifications;
import org.n52.series.db.repositories.core.DataRepository;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Runs tests against a {@link DataRepository} which has {@link ProfileDataEntity} as generic type.
 *
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ProfileDataRepositoryTest extends TestBase {

    @Autowired
    private DataRepository<DataEntity<?>> dataRepository;

    @Test
    @DisplayName("ProfileDataEntity data is found")
    public void given_aDatasetWithoutFeature_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        final DatasetEntity dataset = quantityProfileDataset("ph1", "of1", "pr1", "fmt1", "f1", "fmt2");
        saveProfileValues(dataset, 20.3, 42d, 0d, -1d);

        assertAll("Query quantity data", () -> {
            final DbQuery query = defaultQuery.replaceWith(FILTER_VALUE_TYPES, ValueType.quantity.name());
            final DataQuerySpecifications filterSpec = DataQuerySpecifications.of(query);
            final Iterable<DataEntity> results = dataRepository.findAll(filterSpec.matchDatasets());
            assertThat(results).allMatch(it -> it instanceof QuantityDataEntity)
                               .extracting(it -> Objects.castIfBelongsToType(it, QuantityDataEntity.class));
        });

        assertAll("Query profile values", () -> {

        });
    }

    private QuantityDataEntity saveProfileValues(final DatasetEntity dataset, final Double... values) {
        final Stream<Double> valueStream = values != null
            ? Stream.of(values)
            : Stream.empty();

        final Date now = new Date();
        final QuantityDataEntity profileData = new QuantityDataEntity();
        profileData.setDataset(dataset);
        profileData.setSamplingTimeStart(now);
        profileData.setSamplingTimeEnd(now);
        profileData.setResultTime(now);
        profileData.setStaIdentifier(UUID.randomUUID().toString());
        valueStream.map(it -> toQuantityData(dataset, it))
                   .forEach(dataRepository::save);
        return dataRepository.save(profileData);
    }

    private QuantityDataEntity toQuantityData(final DatasetEntity dataset, final Double it) {
        final QuantityDataEntity dataEntity = new QuantityDataEntity();
        dataEntity.setDataset(dataset);
        dataEntity.setValue(BigDecimal.valueOf(it));
        dataEntity.setSamplingTimeStart(new Date());
        dataEntity.setSamplingTimeEnd(new Date()); // XXX why is this required?
        dataEntity.setResultTime(new Date()); // XXX why is this required?
        dataEntity.setVerticalFrom(BigDecimal.valueOf(it));
        dataEntity.setVerticalTo(BigDecimal.valueOf(it));
        dataEntity.setStaIdentifier(UUID.randomUUID().toString());
        return dataEntity;
    }

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
