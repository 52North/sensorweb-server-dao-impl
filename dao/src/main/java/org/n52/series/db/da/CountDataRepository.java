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

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.count.CountValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.CountDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class CountDataRepository
        extends AbstractDataRepository<CountDatasetEntity, CountDataEntity, CountValue> {

    @Override
    public Class<CountDatasetEntity> getDatasetEntityType() {
        return CountDatasetEntity.class;
    }

    @Override
    protected Data<CountValue> assembleData(CountDatasetEntity seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<CountValue> result = new Data<>();
        DataDao<CountDataEntity> dao = createDataDao(session);
        List<CountDataEntity> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (CountDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    private CountValue[] expandToInterval(Integer value, CountDatasetEntity series, DbQuery query) {
        CountDataEntity referenceStart = new CountDataEntity();
        CountDataEntity referenceEnd = new CountDataEntity();
        referenceStart.setTimestamp(query.getTimespan()
                                         .getStart()
                                         .toDate());
        referenceEnd.setTimestamp(query.getTimespan()
                                       .getEnd()
                                       .toDate());
        referenceStart.setValue(value);
        referenceEnd.setValue(value);
        return new CountValue[] {
            createSeriesValueFor(referenceStart, series, query),
            createSeriesValueFor(referenceEnd, series, query),
        };

    }

    @Override
    public CountValue createSeriesValueFor(CountDataEntity observation, CountDatasetEntity series, DbQuery query) {
        if (observation == null) {
            // do not fail on empty observations
            return null;
        }

        ServiceEntity service = getServiceEntity(series);
        Integer observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        IoParameters parameters = query.getParameters();
        Date timeend = observation.getTimeend();
        Date timestart = observation.getTimestart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        CountValue value = parameters.isShowTimeIntervals()
                ? new CountValue(start, end, observationValue)
                : new CountValue(end, observationValue);

        return addMetadatasIfNeeded(observation, value, series, query);
    }

}
