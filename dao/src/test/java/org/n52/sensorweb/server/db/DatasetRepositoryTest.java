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
package org.n52.sensorweb.server.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.n52.io.request.Parameters.FILTER_VALUE_TYPES;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.dataset.ObservationType;
import org.n52.series.db.beans.dataset.ValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.cosium.spring.data.jpa.entity.graph.repository.support.EntityGraphJpaRepositoryFactoryBean;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class DatasetRepositoryTest extends TestBase {

    @Autowired
    private DatasetRepository datasetRepository;

    @Test
    @DisplayName("Uninitialized valueType is not found")
    public void given_uninitializedDatasetWithFeature_when_queryDatasets_then_noDatasetsFound() {
        uninitializedDataset("ph", "of", "pr", "pr_format", "f1", "format_2");
        assertAll("Uninitialized dataset returned on query", () -> {
            assertThat(datasetRepository.findAll(defaultFilterSpec.matchValueTypes())).isEmpty();
        });
    }

    @Test
    @DisplayName("Uninitialized valueType becomes findable when feature and valueType are set")
    public void given_uninitializedDataset_when_setFeatureAndQualifyAsQuantity_then_entityGetsFoundViaValueType() {
        final DatasetEntity entity = uninitializedDataset("ph", "of", "pr", "pr_format");

        entity.setFeature(testRepositories.persistSimpleFeature("f1", "format_xy"));
        entity.setDatasetType(DatasetType.timeseries);
        entity.setObservationType(ObservationType.simple);
        entity.setValueType(ValueType.quantity);
        DatasetEntity save = testRepositories.save(entity);


        assertAll("qualified quantity dataset is found", () -> {
            final DbQuery query = defaultQuery.replaceWith(FILTER_VALUE_TYPES, ValueType.quantity.name());
            final DatasetQuerySpecifications filterSpec = getDatasetQuerySpecification(query);
            final Optional<DatasetEntity> result = datasetRepository.findOne(filterSpec.matchValueTypes());
            assertTrue(result.isPresent());
            assertThat(result).get().isInstanceOf(DatasetEntity.class);
        });
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = DatasetRepository.class,
            repositoryFactoryBeanClass = EntityGraphJpaRepositoryFactoryBean.class)
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
