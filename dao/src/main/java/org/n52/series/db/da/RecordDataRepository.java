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

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.record.RecordValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.RecordDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.data.Data.RecordData;
import org.n52.series.db.beans.dataset.RecordDataset;
import org.n52.series.db.beans.ereporting.EReportingRecordDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class RecordDataRepository
        extends AbstractDataRepository<RecordDataset, RecordData, RecordValue> {

    @Override
    public Class<?> getDatasetEntityType(Session session) {
        return DataModelUtil.isEntitySupported(EReportingRecordDatasetEntity.class, session)
                ? EReportingRecordDatasetEntity.class
                : RecordDatasetEntity.class;
    }

    @Override
    protected Data<RecordValue> assembleData(RecordDataset seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<RecordValue> result = new Data<>();
        DataDao<RecordData> dao = new DataDao<>(session);
        List<RecordData> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (RecordData observation : observations) {
            // XXX n times same object?
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    @Override
    protected RecordValue createEmptyValue() {
        return new RecordValue();
    }

    @Override
    public RecordValue createSeriesValueFor(RecordData observation, RecordDataset series, DbQuery query) {
        if (observation == null) {
            // do not fail on empty observations
            return null;
        }

        ServiceEntity service = getServiceEntity(series);
        Map<String, Object> observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        RecordValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

}
