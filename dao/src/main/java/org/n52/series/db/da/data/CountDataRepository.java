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
package org.n52.series.db.da.data;

import java.util.List;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.count.CountValue;
import org.n52.series.db.HibernateSessionStore;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.CountDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;

@DataAssembler("count")
public class CountDataRepository
        extends AbstractDataRepository<CountDatasetEntity, CountDataEntity, CountValue> {

    public CountDataRepository(HibernateSessionStore sessionStore,
                               DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    public Class<CountDatasetEntity> getDatasetEntityType() {
        return CountDatasetEntity.class;
    }

    @Override
    protected Data<CountValue> assembleData(CountDatasetEntity seriesEntity, DbQuery query, Session session) {
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

    @Override
    protected CountValue createEmptyValue() {
        return new CountValue();
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

        CountValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

}
