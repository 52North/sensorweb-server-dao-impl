/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.joda.time.DateTime;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetMetadata;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class QuantityDataRepository
        extends AbstractDataRepository<QuantityDatasetEntity, QuantityDataEntity, QuantityValue> {

    private static final String BLOCK_SEPARATOR = "@";
    private static final String TOKEN_SEPARATOR = "#";

    @Override
    public Class<QuantityDatasetEntity> getDatasetEntityType() {
        return QuantityDatasetEntity.class;
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> createReferenceValueOutputs(QuantityDatasetEntity datasetEntity,
            DbQuery query) {
        List<QuantityDatasetEntity> referenceValues = datasetEntity.getReferenceValues();
        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
            ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
            refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
            refenceValueOutput.setReferenceValueId(referenceSeriesEntity.getPkid().toString());

            QuantityDataEntity lastValue = referenceSeriesEntity.getLastValue();
            refenceValueOutput.setLastValue(createSeriesValueFor(lastValue, referenceSeriesEntity, query));
            outputs.add(refenceValueOutput);
        }
        return outputs;
    }

    @Override
    protected Data<QuantityValue> assembleDataWithReferenceValues(QuantityDatasetEntity timeseries, DbQuery dbQuery,
            Session session) throws DataAccessException {
        Data<QuantityValue> result = assembleData(timeseries, dbQuery, session);
        List<QuantityDatasetEntity> referenceValues = timeseries.getReferenceValues();
        if (referenceValues != null && !referenceValues.isEmpty()) {
            DatasetMetadata<Data<QuantityValue>> metadata = new DatasetMetadata<>();
            metadata.setReferenceValues(assembleReferenceSeries(referenceValues, dbQuery, session));
            result.setMetadata(metadata);
        }
        return result;
    }

    private Map<String, Data<QuantityValue>> assembleReferenceSeries(List<QuantityDatasetEntity> referenceValues,
            DbQuery query, Session session) throws DataAccessException {
        Map<String, Data<QuantityValue>> referenceSeries = new HashMap<>();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                Data<QuantityValue> referenceSeriesData = assembleData(referenceSeriesEntity, query, session);
                if (haveToExpandReferenceData(referenceSeriesData)) {
                    referenceSeriesData = expandReferenceDataIfNecessary(referenceSeriesEntity, query, session);
                }
                referenceSeries.put(referenceSeriesEntity.getPkid().toString(), referenceSeriesData);
            }
        }
        return referenceSeries;
    }

    private boolean haveToExpandReferenceData(Data<QuantityValue> referenceSeriesData) {
        return referenceSeriesData.getValues().size() <= 1;
    }

    private Data<QuantityValue> expandReferenceDataIfNecessary(QuantityDatasetEntity seriesEntity, DbQuery query,
            Session session) throws DataAccessException {
        Data<QuantityValue> result = new Data<>();
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        List<QuantityDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        if (!hasValidEntriesWithinRequestedTimespan(observations)) {
            QuantityValue lastValue = getLastValue(seriesEntity, session, query);
            result.addValues(expandToInterval(lastValue.getValue().toPlainString(), seriesEntity, query));
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
                result.addValues(createSeriesValuesFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    private QuantityValue[] expandToInterval(String value, QuantityDatasetEntity series, DbQuery query) {
        QuantityDataEntity referenceStart = new QuantityDataEntity();
        QuantityDataEntity referenceEnd = new QuantityDataEntity();
        referenceStart.setTimestamp(query.getTimespan().getStart().toDate());
        referenceEnd.setTimestamp(query.getTimespan().getEnd().toDate());
        referenceStart.setValue(value.toString());
        referenceEnd.setValue(value.toString());
        return new QuantityValue[] { createSeriesValueFor(referenceStart, series, query),
                createSeriesValueFor(referenceEnd, series, query), };

    }

    @Override
    public QuantityValue createSeriesValueFor(QuantityDataEntity observation, QuantityDatasetEntity dataset,
            DbQuery query) {
        return createSeriesValuesFor(observation, dataset, query)[0];
    }

    public QuantityValue[] createSeriesValuesFor(QuantityDataEntity observation, QuantityDatasetEntity dataset,
            DbQuery query) {
        List<QuantityValue> list = new LinkedList<>();
        String[] blocks = observation.getValue().split(BLOCK_SEPARATOR);
        for (int j = 0; j < blocks.length; j++) {
            String[] split = blocks[j].split(TOKEN_SEPARATOR);
            for (int i = 0; i < split.length; i = i + 2) {
                DateTime time = new DateTime(split[i]);
                BigDecimal v = new BigDecimal(split[i + 1]);
                QuantityValue value = createValue(observation, v, time, dataset, query);
                list.add(addMetadatasIfNeeded(observation, value, dataset, query));
            }
        }

        return list.toArray(new QuantityValue[0]);
    }

    private QuantityValue createValue(QuantityDataEntity observation, BigDecimal value, DateTime time,
            QuantityDatasetEntity dataset, DbQuery query) {
        ServiceEntity service = getServiceEntity(dataset);
        BigDecimal observationValue = !service.isNoDataValue(observation) ? format(value, dataset) : null;
        long end = time.getMillis();
        long start = time.getMillis();
        IoParameters parameters = query.getParameters();
        return parameters.isShowTimeIntervals() ? new QuantityValue(start, end, observationValue)
                : new QuantityValue(end, observationValue);
    }

    QuantityValue createValue(BigDecimal observationValue, QuantityDataEntity observation, DbQuery query) {
        Date timeend = observation.getTimeend();
        Date timestart = observation.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        return parameters.isShowTimeIntervals() ? new QuantityValue(start, end, observationValue)
                : new QuantityValue(end, observationValue);
    }

    private BigDecimal format(BigDecimal value, QuantityDatasetEntity series) {
        if (value == null) {
            return value;
        }
        int scale = series.getNumberOfDecimals();
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    @Override
    public QuantityValue getFirstValue(QuantityDatasetEntity entity, Session session, DbQuery query) {
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        QuantityDataEntity valueEntity = dao.getDataValueViaTimestart(entity, query);
        if (valueEntity != null) {
            QuantityValue[] values = createSeriesValuesFor(valueEntity, entity, query);
            if (values != null && values.length > 0) {
                return values[0];
            }
        }
        return null;
    }

    @Override
    public QuantityValue getLastValue(QuantityDatasetEntity entity, Session session, DbQuery query) {
        DataDao<QuantityDataEntity> dao = createDataDao(session);
        QuantityDataEntity valueEntity = dao.getDataValueViaTimeend(entity, query);
        if (valueEntity != null) {
            QuantityValue[] values = createSeriesValuesFor(valueEntity, entity, query);
            if (values != null && values.length > 0) {
                return values[values.length - 1];
            }
        }
        return null;
    }

}
