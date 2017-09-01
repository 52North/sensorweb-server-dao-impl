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
import org.n52.io.response.dataset.text.TextData;
import org.n52.io.response.dataset.text.TextDatasetMetadata;
import org.n52.io.response.dataset.text.TextValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.series.db.beans.TextDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class TextDataRepository extends AbstractDataRepository<TextData, TextDatasetEntity, TextDataEntity, TextValue> {

    @Override
    public Class<TextDatasetEntity> getDatasetEntityType() {
        return TextDatasetEntity.class;
    }

    @Override
    protected TextData assembleDataWithReferenceValues(TextDatasetEntity timeseries, DbQuery dbQuery, Session session)
            throws DataAccessException {
        TextData result = assembleData(timeseries, dbQuery, session);
        Set<TextDatasetEntity> referenceValues = timeseries.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            TextDatasetMetadata metadata = new TextDatasetMetadata();
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, dbQuery, session));
            result.setMetadata(metadata);
        }
        return result;
    }

    private Map<String, TextData> assembleReferenceSeries(Set<TextDatasetEntity> referenceValues,
                                                          DbQuery query,
                                                          Session session)
            throws DataAccessException {
        Map<String, TextData> referenceSeries = new HashMap<>();
        for (TextDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                TextData referenceSeriesData = assembleData(referenceSeriesEntity, query, session);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity, query, session);
                }
                referenceSeries.put(Long.toString(referenceSeriesEntity.getPkid()), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(TextData referenceSeriesData) {
        List<TextValue> values = referenceSeriesData.getValues();
        return values.size() <= 1;
    }

    private TextData expandReferenceDataIfNecessary(TextDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        TextData result = new TextData();
        DataDao<TextDataEntity> dao = new DataDao<>(session);
        List<TextDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            TextValue lastValidValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValidValue.getValue(), seriesEntity, query));
        }

        if (hasSingleValidReferenceValue(observations)) {
            TextDataEntity entity = observations.get(0);
            result.addValues(expandToInterval(entity.getValue(), seriesEntity, query));
        }
        return result;
    }

    @Override
    protected TextData assembleData(TextDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        TextData result = new TextData();
        DataDao<TextDataEntity> dao = new DataDao<>(session);
        List<TextDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (TextDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    // XXX
    private TextValue[] expandToInterval(String value, TextDatasetEntity series, DbQuery query) {
        TextDataEntity referenceStart = new TextDataEntity();
        TextDataEntity referenceEnd = new TextDataEntity();
        referenceStart.setPhenomenonTimeEnd(query.getTimespan()
                                                 .getStart()
                                                 .toDate());
        referenceEnd.setPhenomenonTimeEnd(query.getTimespan()
                                               .getEnd()
                                               .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new TextValue[] {
            createSeriesValueFor(referenceStart, series, query),
            createSeriesValueFor(referenceEnd, series, query),
        };

    }

    @Override
    public TextValue createSeriesValueFor(TextDataEntity observation, TextDatasetEntity series, DbQuery query) {
        if (observation == null) {
            // do not fail on empty observations
            return null;
        }

        ServiceEntity service = getServiceEntity(series);
        String observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        Date timeend = observation.getPhenomenonTimeEnd();
        Date timestart = observation.getPhenomenonTimeStart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        TextValue value = parameters.isShowTimeIntervals()
                ? new TextValue(start, end, observationValue)
                : new TextValue(end, observationValue);

        return addMetadatasIfNeeded(observation, value, series, query);
    }

}
