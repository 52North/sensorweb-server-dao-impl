/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.da;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.bool.BooleanValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.BooleanDataEntity;
import org.n52.series.db.beans.BooleanDatasetEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class BooleanDataRepository
        extends AbstractDataRepository<BooleanDatasetEntity, BooleanDataEntity, BooleanValue> {

    @Override
    public Class<BooleanDatasetEntity> getDatasetEntityType() {
        return BooleanDatasetEntity.class;
    }

    @Override
    protected Data<BooleanValue> assembleDataWithReferenceValues(BooleanDatasetEntity timeseries,
                                                          DbQuery dbQuery,
                                                          Session session)
            throws DataAccessException {
        Data<BooleanValue> result = assembleData(timeseries, dbQuery, session);
        Set<DatasetEntity> referenceValues = timeseries.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            DatasetMetadata<Data<BooleanValue>> metadata = new DatasetMetadata<>();
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, dbQuery, session));
            result.setMetadata(metadata);
        }
        return result;
    }

    private Map<String, Data<BooleanValue>> assembleReferenceSeries(Set<DatasetEntity> referenceValues,
                                                             DbQuery query,
                                                             Session session)
            throws DataAccessException {
        Map<String, Data<BooleanValue>> referenceSeries = new HashMap<>();
        for (DatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity instanceof BooleanDatasetEntity) {
                if (referenceSeriesEntity.isPublished()) {
                    Data<BooleanValue> referenceSeriesData = assembleData((BooleanDatasetEntity) referenceSeriesEntity, query, session);
                    if (haveToExpandReferenceData(referenceSeriesData)) {
                        referenceSeriesData = expandReferenceDataIfNecessary((BooleanDatasetEntity) referenceSeriesEntity, query, session);
                    }
                    referenceSeries.put(referenceSeriesEntity.getId()
                                                             .toString(),
                                        referenceSeriesData);
                }
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(Data<BooleanValue> referenceSeriesData) {
        return referenceSeriesData.getValues()
                                  .size() <= 1;
    }

    private Data<BooleanValue> expandReferenceDataIfNecessary(BooleanDatasetEntity seriesEntity,
                                                       DbQuery query,
                                                       Session session)
            throws DataAccessException {
        Data<BooleanValue> result = new Data<>();
        DataDao<BooleanDataEntity> dao = createDataDao(session);
        List<BooleanDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            BooleanValue lastValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValue.getValue(), seriesEntity, query));
        }
        if (hasSingleValidReferenceValue(observations)) {
            BooleanDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), seriesEntity, query));
        }
        return result;
    }

    @Override
    protected Data<BooleanValue> assembleData(BooleanDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<BooleanValue> result = new Data<>();
        DataDao<BooleanDataEntity> dao = createDataDao(session);
        List<BooleanDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (BooleanDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    private BooleanValue[] expandToInterval(Boolean value, BooleanDatasetEntity series, DbQuery query) {
        BooleanDataEntity referenceStart = new BooleanDataEntity();
        BooleanDataEntity referenceEnd = new BooleanDataEntity();
        referenceStart.setSamplingTimeEnd(query.getTimespan()
                                                 .getStart()
                                                 .toDate());
        referenceEnd.setSamplingTimeEnd(query.getTimespan()
                                               .getEnd()
                                               .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new BooleanValue[] {
            createSeriesValueFor(referenceStart, series, query),
            createSeriesValueFor(referenceEnd, series, query),
        };

    }

    @Override
    protected BooleanValue createEmptyValue() {
        return new BooleanValue();
    }

    @Override
    public BooleanValue createSeriesValueFor(BooleanDataEntity observation,
                                             BooleanDatasetEntity series,
                                             DbQuery query) {
        ServiceEntity service = getServiceEntity(series);
        Boolean observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        BooleanValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

}