/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class QuantityDataRepository extends
        AbstractDataRepository<QuantityDatasetEntity, QuantityDataEntity, QuantityValue> {

    @Override
    public Class<QuantityDatasetEntity> getDatasetEntityType() {
        return QuantityDatasetEntity.class;
    }

    @Override
    protected Data<QuantityValue> assembleDataWithReferenceValues(QuantityDatasetEntity timeseries,
                                                           DbQuery dbQuery,
                                                           Session session)
            throws DataAccessException {
        Data<QuantityValue> result = assembleData(timeseries, dbQuery, session);
        Set<QuantityDatasetEntity> referenceValues = timeseries.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            DatasetMetadata<Data<QuantityValue>> metadata = new DatasetMetadata<>();
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, dbQuery, session));
            result.setMetadata(metadata);
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(Set<QuantityDatasetEntity> referenceValues,
                                                              DbQuery query,
                                                              Session session)
            throws DataAccessException {
        Map<String, Data<QuantityValue>> referenceSeries = new HashMap<>();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                Data<QuantityValue> referenceSeriesData = assembleData(referenceSeriesEntity, query, session);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity, query, session);
                }
                referenceSeries.put(referenceSeriesEntity.getPkid()
                                                         .toString(),
                                    referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referenceSeriesData) {
        return referenceSeriesData.getValues()
                                  .size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(QuantityDatasetEntity seriesEntity,
                                                        DbQuery query,
                                                        Session session)
            throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        List<QuantityDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            QuantityValue lastValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValue.getValue(), seriesEntity, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            QuantityDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), seriesEntity, query));
        }
        return result;
    }

    @Override
    protected Data<QuantityValue> assembleData(QuantityDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        List<QuantityDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (QuantityDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    private QuantityValue[] expandToInterval(BigDecimal value, QuantityDatasetEntity series, DbQuery query) {
        QuantityDataEntity referenceStart = new QuantityDataEntity();
        QuantityDataEntity referenceEnd = new QuantityDataEntity();
        referenceStart.setTimestamp(query.getTimespan()
                                         .getStart()
                                         .toDate());
        referenceEnd.setTimestamp(query.getTimespan()
                                       .getEnd()
                                       .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new QuantityValue[] {
            createSeriesValueFor(referenceStart, series, query),
            createSeriesValueFor(referenceEnd, series, query),
        };

    }

    @Override
    public QuantityValue createSeriesValueFor(QuantityDataEntity observation,
                                              QuantityDatasetEntity dataset,
                                              DbQuery query) {
        QuantityValue value = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, value, dataset, query);
    }

    private QuantityValue createValue(QuantityDataEntity observation, QuantityDatasetEntity series, DbQuery query) {
        ServiceEntity service = getServiceEntity(series);
        BigDecimal observationValue = !service.isNoDataValue(observation)
                ? format(observation, series)
                : null;
        return createValue(observationValue, observation, query);
    }

    QuantityValue createValue(BigDecimal observationValue, QuantityDataEntity observation, DbQuery query) {
        Date timeend = observation.getTimeend();
        Date timestart = observation.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        return parameters.isShowTimeIntervals()
                ? new QuantityValue(start, end, observationValue)
                : new QuantityValue(end, observationValue);
    }

    private BigDecimal format(QuantityDataEntity observation, QuantityDatasetEntity series) {
        if (observation.getValue() == null) {
            return observation.getValue();
        }
        int scale = series.getNumberOfDecimals();
        return observation.getValue().setScale(scale, RoundingMode.HALF_UP);
    }

}
