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
package org.n52.io.extension.aggregation;

import javax.persistence.EntityManager;

import org.n52.io.extension.ExtensionAssembler;
import org.n52.io.extension.resulttime.ResultTimeAssembler;
import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.response.OptionalOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.AggregationOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.ValueAssembler;
import org.n52.series.db.assembler.value.AbstractNumericalValueAssembler;
import org.n52.series.db.assembler.value.AbstractValueAssembler;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregationAssembler extends ExtensionAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultTimeAssembler.class);

    private EntityManager entityManager;

    private final DataRepositoryTypeFactory dataRepositoryFactory;

    public AggregationAssembler(EntityManager entityManager, DatasetRepository datasetRepository,
            DataRepositoryTypeFactory dataRepositoryFactory, DbQueryFactory dbQueryFactory) {
        super(datasetRepository, dbQueryFactory);
        this.entityManager = entityManager;
        this.dataRepositoryFactory = dataRepositoryFactory;
    }

    public <V extends AbstractValue<?>> AggregationOutput<V> getExtras(String id, IoParameters parameters) {
        try {
            AggregationOutput<V> aggregation = new AggregationOutput<>();
            DbQuery query = getDbQuery(parameters);
            DatasetEntity entity = getDatasetRepository().getOne(Long.parseLong(id));
            ValueAssembler<?, ?, ?> assembler = dataRepositoryFactory.create(entity.getObservationType().name(),
                    entity.getValueType().name(), DatasetEntity.class);
            if (assembler instanceof AbstractValueAssembler) {
                addCount(aggregation, (AbstractValueAssembler<?, ?, ?>) assembler, entity, query, entityManager);
                if (checkNumerical(entity) && assembler instanceof AbstractNumericalValueAssembler) {
                    addAggregation(aggregation, (AbstractNumericalValueAssembler<DataEntity<?>, V, ?>) assembler,
                            entity, query, entityManager);
                }
            }
            return aggregation;
        } catch (DatasetFactoryException e) {
            LOGGER.debug("Could not create aggregation metadata!", e);
        }
        return null;
    }

    private void addCount(AggregationOutput<?> aggregation, AbstractValueAssembler<?, ?, ?> dataRepository,
            DatasetEntity dataset, DbQuery query, EntityManager entityManager) {
        aggregation.setCount(OptionalOutput.of(dataRepository.getCount(dataset, query)));
    }

    private <V extends AbstractValue<?>> void addAggregation(AggregationOutput<V> aggregation,
            AbstractNumericalValueAssembler<DataEntity<?>, V, ?> dataRepository, DatasetEntity dataset, DbQuery query,
            EntityManager entityManager) {
        aggregation.setMax(OptionalOutput.of(dataRepository.getMax(dataset, query)));
        aggregation.setMin(OptionalOutput.of(dataRepository.getMin(dataset, query)));
        aggregation.setAvg(OptionalOutput.of(dataRepository.getAverage(dataset, query)));
    }

    private boolean checkNumerical(DatasetEntity dataset) {
        return ValueType.quantity.equals(dataset.getValueType()) || ValueType.count.equals(dataset.getValueType());
    }

}
