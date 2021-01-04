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
package org.n52.series.db.assembler.value;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DataQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

import java.util.stream.Stream;

public abstract class TrajectoryValueAssembler<E extends DataEntity<T>, V extends AbstractValue<?>, T>
    extends AbstractValueAssembler<E, V, T> {

    private final DataRepository<E> trajectoryDataRepository;

    public TrajectoryValueAssembler(DataRepository<E> trajectoryDataRepository,
                                    DatasetRepository datasetRepository) {
        super(trajectoryDataRepository, datasetRepository);
        this.trajectoryDataRepository = trajectoryDataRepository;
    }

    @Override
    protected Stream<E> findAll(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.<E>of(query);
        Specification<E> predicate = dataFilterSpec.matchFiltersParentsNotNull();
        Iterable<E> entities = trajectoryDataRepository.findAll(predicate);
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

}
