/*
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
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
import org.joda.time.DateTime;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.v1.ext.DatasetType;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.SessionAwareRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DataParameter;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.ObservationDao;
import org.n52.series.db.dao.SeriesDao;

public abstract class AbstractDataRepository<D extends Data<?>, DSE extends DatasetEntity<?>, DE extends DataEntity<?>, V extends AbstractValue<?>>
        extends SessionAwareRepository<DbQuery> implements DataRepository<DSE, V> {

    @Override
    public Data<?> getData(String seriesId, DbQuery dbQuery) throws DataAccessException {
        Session session = getSession();
        try {
            SeriesDao<DSE> seriesDao = getSeriesDao(session);
            String id = DatasetType.extractId(seriesId);
            DSE series = seriesDao.getInstance(parseId(id), dbQuery);
            return dbQuery.isExpanded()
                ? assembleDataWithReferenceValues(series, dbQuery, session)
                : assembleData(series, dbQuery, session);
        }
        finally {
            returnSession(session);
        }
    }

    @Override
    public V getFirstValue(DSE entity, Session session) {
        return getValueAt(entity.getFirstValueAt(), entity, session);
    }

    @Override
    public V getLastValue(DSE entity, Session session) {
        return getValueAt(entity.getLastValueAt(), entity, session);
    }

    private V getValueAt(Date valueAt, DSE datasetEntity, Session session) {
        DateTime timestamp = new DateTime(valueAt);
        ObservationDao<DE> dao = createDataDao(session);
        DE valueEntity = dao.getDataValueAt(timestamp, datasetEntity);
        return createSeriesValueFor(valueEntity, datasetEntity);
    }

    protected SeriesDao<DSE> getSeriesDao(Session session) {
        return new SeriesDao<>(session);
    }

    protected ObservationDao<DE> createDataDao(Session session) {
        return new ObservationDao<>(session);
    }

//    protected abstract SeriesDao<DSE> getSeriesDao(Session session);
//
//    protected abstract ObservationDao<DE> createDataDao(Session session);

    protected abstract V createSeriesValueFor(DE valueEntity, DSE datasetEntity);

    protected abstract D assembleData(DSE datasetEntity, DbQuery query, Session session) throws DataAccessException;

    protected abstract D assembleDataWithReferenceValues(DSE datasetEntity, DbQuery dbQuery, Session session) throws DataAccessException;

    protected boolean hasValidEntriesWithinRequestedTimespan(List<?> observations) {
        return observations.size() > 0;
    }

    protected boolean hasSingleValidReferenceValue(List<?> observations) {
        return observations.size() == 1;
    }

    protected void addGeometry(DataEntity<?> dataEntity, AbstractValue<?> value) {
        if (dataEntity.isSetGeometry()) {
            GeometryEntity geometry = dataEntity.getGeometry();
            value.setGeometry(geometry.getGeometry(getDatabaseSrid()));
        }
    }

    protected void addValidTime(DataEntity<?> observation, AbstractValue<?> value) {
        // TODO add validTime to value
        if (observation.isSetValidStartTime()) {
            observation.getValidTimeStart().getTime();
        }
        if (observation.isSetValidEndTime()) {
            observation.getValidTimeEnd().getTime();
        }
    }

    protected void addParameter(DataEntity<?> observation, AbstractValue<?> value) {
        if (observation.hasParameters()) {
            for (DataParameter<?> parameter : observation.getParameters()) {
                // TODO add parameters to value
                parameter.getName();
                parameter.getValue();
            }
        }
    }

}
