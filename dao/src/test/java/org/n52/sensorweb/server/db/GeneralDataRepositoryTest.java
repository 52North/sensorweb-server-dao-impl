/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.sensorweb.server.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.n52.io.request.Parameters.FILTER_VALUE_TYPES;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DataQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Runs tests against a {@link DataRepository} which has {@link DataEntity} as generic type.
 *
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class GeneralDataRepositoryTest extends TestBase {

    @Autowired
    private DataRepository<DataEntity<?>> dataRepository;

    @Test
    @DisplayName("Quality Data are found")
    public void given_aDatasetWithoutFeature_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        final DatasetEntity dataset = quantityDataset("ph1", "of1", "pr1", "format1", "f1", "format2");
        final QuantityDataEntity dataEntity = new QuantityDataEntity();
        dataEntity.setDataset(dataset);
        dataEntity.setValue(BigDecimal.valueOf(20));
        dataEntity.setSamplingTimeStart(new Date());
        dataEntity.setSamplingTimeEnd(new Date()); // XXX why is this required?
        dataEntity.setResultTime(new Date()); // XXX why is this required?
        dataEntity.setStaIdentifier(UUID.randomUUID().toString());
        QuantityDataEntity persisted = dataRepository.saveAndFlush(dataEntity);

        assertAll("Query quantity data", () -> {
            final DbQuery query = defaultQuery.replaceWith(FILTER_VALUE_TYPES, ValueType.quantity.name());
            final DataQuerySpecifications filterSpec = DataQuerySpecifications.of(query);
            assertThat(dataRepository.findAll()).isNotEmpty();
            final Optional<DataEntity> result = dataRepository.findOne(filterSpec.matchDatasets());
            assertTrue(result.isPresent());
            assertThat(result).get().isInstanceOf(QuantityDataEntity.class);
        });
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
