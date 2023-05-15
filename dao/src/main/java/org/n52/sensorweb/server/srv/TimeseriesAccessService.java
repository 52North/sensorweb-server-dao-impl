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
package org.n52.sensorweb.server.srv;

import java.math.BigDecimal;

import org.n52.io.TvpDataCollection;
import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DataCollection;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.sensorweb.server.db.ValueAssembler;
import org.n52.sensorweb.server.db.assembler.core.DatasetAssembler;
import org.n52.sensorweb.server.db.factory.DataRepositoryTypeFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.dataset.ObservationType;
import org.n52.series.spi.srv.DataService;
import org.springframework.stereotype.Component;

@Component
public class TimeseriesAccessService extends AccessService<TimeseriesMetadataOutput>
        implements
        DataService<Data<QuantityValue>> {

    private final DataRepositoryTypeFactory factory;

    public TimeseriesAccessService(DataRepositoryTypeFactory factory,
                                   DatasetAssembler repository,
                                   DbQueryFactory queryFactory) {
        super(repository, queryFactory);
        this.factory = factory;
    }

    @Override
    public DataCollection<Data<QuantityValue>> getData(IoParameters parameters) {
        TvpDataCollection<Data<QuantityValue>> dataCollection = new TvpDataCollection<>();
        for (String timeseriesId : parameters.getDatasets()) {
            Data<QuantityValue> data = getDataFor(timeseriesId, parameters);
            if (data != null) {
                dataCollection.addNewSeries(timeseriesId, data);
            }
        }
        return dataCollection;
    }

    private Data<QuantityValue> getDataFor(String timeseriesId, IoParameters parameters) {
        DbQuery dbQuery = dbQueryFactory.createFrom(parameters);
        return createRepository().getData(timeseriesId, dbQuery);
    }

    private ValueAssembler<QuantityDataEntity, QuantityValue, BigDecimal> createRepository() {
        try {
            return factory.create(DatasetType.timeseries.name(), ObservationType.simple.name(), QuantityValue.TYPE,
                    DatasetEntity.class);
        } catch (DatasetFactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
