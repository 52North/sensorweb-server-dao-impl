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

import java.util.List;

import org.n52.io.TvpDataCollection;
import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DataCollection;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetTypesMetadata;
import org.n52.sensorweb.server.db.ValueAssembler;
import org.n52.sensorweb.server.db.assembler.core.DatasetAssembler;
import org.n52.sensorweb.server.db.factory.DataRepositoryTypeFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.spi.srv.DataService;
import org.n52.series.spi.srv.DatasetTypesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetService<V extends AbstractValue<?>> extends AccessService<DatasetOutput<V>>
        implements DataService<Data<V>>, DatasetTypesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetService.class);

    private final DataRepositoryTypeFactory dataFactory;

    public DatasetService(DataRepositoryTypeFactory dataFactory, DatasetAssembler<V> repository,
            DbQueryFactory queryFactory) {
        super(repository, queryFactory);
        this.dataFactory = dataFactory;
    }

    @Override
    public DataCollection<Data<V>> getData(IoParameters parameters) {
        TvpDataCollection<Data<V>> dataCollection = new TvpDataCollection<>();
        List<DatasetTypesMetadata> datasetTypesMetadata =
                getRepository().getDatasetTypesMetadata(dbQueryFactory.createFrom(parameters));
        for (DatasetTypesMetadata metadata : datasetTypesMetadata) {
            Data<V> data;
            try {
                data = getDataFor(metadata, parameters);
                if (data != null) {
                    dataCollection.addNewSeries(metadata.getId(), data);
                }
            } catch (DatasetFactoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return dataCollection;
    }

    private Data<V> getDataFor(DatasetTypesMetadata metadata, IoParameters parameters) throws DatasetFactoryException {
        DbQuery dbQuery = dbQueryFactory.createFrom(parameters);
        Class<? extends DatasetEntity> entityType = DatasetEntity.class;
        ValueAssembler<?, V, ?> assembler = dataFactory.create(metadata.getDatasetType(),
                metadata.getObservationType(), metadata.getValueType(), entityType);
        return assembler.getData(metadata.getId(), dbQuery);
    }

    private DatasetAssembler<V> getRepository() {
        return (DatasetAssembler<V>) repository;
    }

    @Override
    public List<DatasetTypesMetadata> getDatasetTypesMetadata(IoParameters map) {
        DbQuery dbQuery = dbQueryFactory.createFrom(map);
        return getRepository().getDatasetTypesMetadata(dbQuery);
    }
}
