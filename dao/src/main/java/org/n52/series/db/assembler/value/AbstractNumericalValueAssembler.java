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

import java.math.BigDecimal;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DataQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;

public abstract class AbstractNumericalValueAssembler<E extends DataEntity<T>,
                                                      V extends AbstractValue<?>,
                                                      T extends Number>
        extends AbstractValueAssembler<E, V, T> {

    protected AbstractNumericalValueAssembler(DataRepository<E> dataRepository, DatasetRepository datasetRepository) {
        super(dataRepository, datasetRepository);
    }

    public V getMax(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return assembleDataValue((E) dataFilterSpec.max(dataset, getEntityManager()), dataset, query);
    }

    public V getMin(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return assembleDataValue((E) dataFilterSpec.min(dataset, getEntityManager()), dataset, query);
    }

    public BigDecimal getAverage(DatasetEntity dataset, DbQuery query) {
        DataQuerySpecifications dataFilterSpec = DataQuerySpecifications.of(query);
        return dataFilterSpec.average(dataset, getEntityManager());
    }

}
