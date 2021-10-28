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
package org.n52.series.srv;

import java.util.List;

import org.n52.io.TvpDataCollection;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DataCollection;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetTypesMetadata;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.da.DataRepository;
import org.n52.series.db.da.DatasetRepository;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.srv.DataService;
import org.n52.series.spi.srv.DatasetTypesService;
import org.n52.web.exception.InternalServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
public class DatasetAccessService<V extends AbstractValue<?>> extends AccessService<DatasetOutput<V>>
        implements DataService<Data<V>>, DatasetTypesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetAccessService.class);

    @Autowired
    private DataRepositoryTypeFactory dataFactory;

    public DatasetAccessService(DatasetRepository<V> repository) {
        super(repository);
    }

    @Override
    public DataCollection<Data<V>> getData(IoParameters parameters) {
        try {
            TvpDataCollection<Data<V>> dataCollection = new TvpDataCollection<>();
            List<DatasetTypesMetadata> datasetTypesMetadata = getRepository().getDatasetTypesMetadata(parameters);
            for (DatasetTypesMetadata metadata : datasetTypesMetadata) {
                Data<V> data = getDataFor(metadata, parameters);
                if (data != null) {
                    dataCollection.addNewSeries(metadata.getId(), data);
                }
            }
            return dataCollection;
        } catch (DataAccessException e) {
            throw new InternalServerException("Could not get series data from database.", e);
        }
    }

    private Data<V> getDataFor(DatasetTypesMetadata metadata, IoParameters parameters)
            throws DataAccessException {
        DbQuery dbQuery = dbQueryFactory.createFrom(parameters);
        Class<? extends DatasetEntity> entityType = DatasetEntity.class;
        DataRepository<? extends DatasetEntity, ?, V, ?> assembler =
                dataFactory.create(metadata.getObservationType(), metadata.getValueType(), entityType);
        return assembler.getData(metadata.getId(), dbQuery);
    }

    private DatasetRepository<V> getRepository() {
        return (DatasetRepository<V>) repository;
    }

    @Override
    public List<DatasetTypesMetadata> getDatasetTypesMetadata(IoParameters map) {
        return getRepository().getDatasetTypesMetadata(map);
    }

}
