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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.category.CategoryValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.CategoryDataEntity;
import org.n52.series.db.beans.CategoryDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class CategoryDataRepository
        extends AbstractDataRepository<CategoryDatasetEntity, CategoryDataEntity, CategoryValue> {

    @Override
    public Class<CategoryDatasetEntity> getDatasetEntityType() {
        return CategoryDatasetEntity.class;
    }

    @Override
    protected Data<CategoryValue> assembleDataWithReferenceValues(CategoryDatasetEntity timeseries,
                                                                  DbQuery dbQuery,
                                                                  Session session)
            throws DataAccessException {
        Data<CategoryValue> result = assembleData(timeseries, dbQuery, session);
        Set<CategoryDatasetEntity> referenceValues = timeseries.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            DatasetMetadata<Data<CategoryValue>> metadata = new DatasetMetadata<>();
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, dbQuery, session));
            result.setMetadata(metadata);
        }
        return result;
    }

    private Map<String, Data<CategoryValue>> assembleReferenceSeries(Set<CategoryDatasetEntity> referenceValues,
                                                                     DbQuery query,
                                                                     Session session)
            throws DataAccessException {
        Map<String, Data<CategoryValue>> referenceSeries = new HashMap<>();
        for (CategoryDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                Data<CategoryValue> referenceSeriesData = assembleData(referenceSeriesEntity, query, session);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity, query, session);
                }
                referenceSeries.put(Long.toString(referenceSeriesEntity.getPkid()), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(Data<CategoryValue> referenceSeriesData) {
        List<CategoryValue> values = referenceSeriesData.getValues();
        return values.size() <= 1;
    }

    private Data<CategoryValue> expandReferenceDataIfNecessary(CategoryDatasetEntity seriesEntity,
                                                               DbQuery query,
                                                               Session session)
            throws DataAccessException {
        Data<CategoryValue> result = new Data<>();
        DataDao<CategoryDataEntity> dao = new DataDao<>(session);
        List<CategoryDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            CategoryValue lastValidValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValidValue.getValue(), seriesEntity, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            CategoryDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), seriesEntity, query));
        }
        return result;
    }

    @Override
    protected Data<CategoryValue> assembleData(CategoryDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<CategoryValue> result = new Data<>();
        DataDao<CategoryDataEntity> dao = new DataDao<>(session);
        List<CategoryDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (CategoryDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    // XXX
    private CategoryValue[] expandToInterval(String value, CategoryDatasetEntity series, DbQuery query) {
        CategoryDataEntity referenceStart = new CategoryDataEntity();
        CategoryDataEntity referenceEnd = new CategoryDataEntity();
        referenceStart.setTimestamp(query.getTimespan()
                                         .getStart()
                                         .toDate());
        referenceEnd.setTimestamp(query.getTimespan()
                                       .getEnd()
                                       .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new CategoryValue[] {
            createSeriesValueFor(referenceStart, series, query),
            createSeriesValueFor(referenceEnd, series, query),
        };

    }

    @Override
    public CategoryValue createSeriesValueFor(CategoryDataEntity observation,
                                              CategoryDatasetEntity series,
                                              DbQuery query) {
        ServiceEntity service = getServiceEntity(series);
        String observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        CategoryValue value = createValue(observation, series, query, observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

    private CategoryValue createValue(CategoryDataEntity observation,
                                      CategoryDatasetEntity series,
                                      DbQuery query,
                                      String observationValue) {
        ServiceEntity service = getServiceEntity(series);
        String textValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;
        return createValue(textValue, observation, query);
    }

    CategoryValue createValue(String observationValue,
                              CategoryDataEntity observation,
                              DbQuery query) {
        Date timeend = observation.getTimeend();
        Date timestart = observation.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        return parameters.isShowTimeIntervals()
                ? new CategoryValue(start, end, observationValue)
                : new CategoryValue(end, observationValue);
    }

}
