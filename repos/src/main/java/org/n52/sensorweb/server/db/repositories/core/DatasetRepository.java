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
package org.n52.sensorweb.server.db.repositories.core;

import java.util.List;
import java.util.Set;

import org.n52.sensorweb.server.db.repositories.ParameterServiceRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface DatasetRepository extends ParameterServiceRepository<DatasetEntity> {

    default List<DatasetEntity> findByService(ServiceEntity service) {
        return findAll(createExample(new DatasetEntity(), createMatcher()));
    }

    void deleteByIdIn(Iterable<Long> ids);

    @Override
    default ExampleMatcher createMatcher() {
        return ExampleMatcher.matching().withIgnorePaths(DescribableEntity.PROPERTY_ID)
                .withMatcher(DatasetEntity.PROPERTY_SERVICE, GenericPropertyMatchers.ignoreCase());
    }

    @Query(value = "SELECT dataset_id, dataset_type, observation_type, value_type "
            + "FROM {h-schema}dataset WHERE dataset_id in (?1)", nativeQuery = true)
    Set<Object[]> getMetadataTypes(Set<Long> datasetId);

}
