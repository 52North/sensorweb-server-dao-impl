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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.AbstractValue.ValidTime;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.ValueType;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.parameter.Parameter;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DatasetDao;
import org.n52.series.db.dao.DbQuery;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDataRepository<S extends DatasetEntity,
                                             E extends DataEntity< ? >,
                                             V extends AbstractValue< ? >>
        extends SessionAwareRepository implements DataRepository<S, V> {

    @Autowired
    private PlatformRepository platformRepository;

    @Override
    public Data< ? extends AbstractValue< ? >> getData(String datasetId, DbQuery dbQuery) throws DataAccessException {
        Session session = getSession();
        try {
            String id = ValueType.extractId(datasetId);
            DatasetDao<S> seriesDao = getSeriesDao(session);
            IoParameters parameters = dbQuery.getParameters();
            // remove spatial filter on metadata
            S series = seriesDao.getInstance(id, getDbQuery(parameters.removeAllOf(Parameters.BBOX)
                                                                      .removeAllOf(Parameters.NEAR)
                                                                      .removeAllOf(Parameters.ODATA_FILTER)));
            if (series.getService() == null) {
                series.setService(getServiceEntity());
            }
            return dbQuery.isExpanded()
                    ? assembleDataWithReferenceValues(series, dbQuery, session)
                    : assembleData(series, dbQuery, session);
        } finally {
            returnSession(session);
        }
    }

    private PlatformEntity getCondensedPlatform(DatasetEntity dataset, DbQuery query, Session session)
            throws DataAccessException {
        // platform has to be handled dynamically (see #309)
        return platformRepository.getEntity(getPlatformId(dataset), query, session);
    }

    @Override
    public V getFirstValue(S entity, Session session, DbQuery query) {
//        DataDao<E> dao = createDataDao(session);
//        E valueEntity = dao.getDataValueViaTimestart(entity, query);
        E valueEntity = (E) entity.getFirstObservation();
        return valueEntity != null
                ? createSeriesValueFor(valueEntity, entity, query)
                : null;
    }

    @Override
    public V getLastValue(S entity, Session session, DbQuery query) {
//        DataDao<E> dao = createDataDao(session);
//        E valueEntity = dao.getDataValueViaTimeend(entity, query);
        E valueEntity = (E) entity.getLastObservation();
        return valueEntity != null
                ? createSeriesValueFor(valueEntity, entity, query)
                : null;
    }

    @Override
    public GeometryEntity getLastKnownGeometry(DatasetEntity entity, Session session, DbQuery query) {
//        DataDao<E> dao = createDataDao(session);
//        return dao.getValueGeometryViaTimeend(entity, query);
        org.n52.series.db.beans.data.Data<?> lastObservation = entity.getLastObservation();
        return lastObservation != null
                ? lastObservation.getGeometryEntity()
                : null;
    }

    protected DatasetDao<S> getSeriesDao(Session session) {
        return new DatasetDao<>(session);
    }

    protected DataDao<E> createDataDao(Session session) {
        return new DataDao<>(session);
    }

    protected abstract V createEmptyValue();

    protected V prepareValue(E observation, DbQuery query) {
        V emptyValue = createEmptyValue();
        if (observation == null) {
            return emptyValue;
        }

        IoParameters parameters = query.getParameters();
        Date timeend = observation.getSamplingTimeEnd();
        Date timestart = observation.getSamplingTimeStart();
        if (parameters.isShowTimeIntervals() && (timestart != null)) {
            emptyValue.setTimestart(timestart.getTime());
        }
        emptyValue.setTimestamp(timeend.getTime());
        return emptyValue;
    }

    @Override
    public List<ReferenceValueOutput<V>> createReferenceValueOutputs(S datasetEntity, DbQuery query) {
        return new ArrayList<>();
    }

    protected abstract V createSeriesValueFor(E valueEntity, S datasetEntity, DbQuery query);

    protected abstract Data<V> assembleData(S datasetEntity, DbQuery query, Session session) throws DataAccessException;

    protected Data<V> assembleDataWithReferenceValues(S datasetEntity, DbQuery dbQuery, Session session)
            throws DataAccessException {
        return assembleData(datasetEntity, dbQuery, session);
    }

    protected boolean hasValidEntriesWithinRequestedTimespan(List< ? > observations) {
        return observations.size() > 0;
    }

    protected boolean hasSingleValidReferenceValue(List< ? > observations) {
        return observations.size() == 1;
    }

    protected V addMetadatasIfNeeded(E observation, V value, S dataset, DbQuery query) {
        // TODO move to appropriate location
        addResultTime(observation, value);

        if (query.isExpanded()) {
            addValidTime(observation, value);
            addParameters(observation, value, query);
            addGeometry(observation, value, query);
        } else {
            if (dataset.isMobile()) {
                addGeometry(observation, value, query);
            }
        }
        return value;
    }

    protected void addGeometry(DataEntity< ? > dataEntity, AbstractValue< ? > value, DbQuery query) {
        if (dataEntity.isSetGeometryEntity()) {
            GeometryEntity geometryEntity = dataEntity.getGeometryEntity();
            Geometry geometry = getGeometry(geometryEntity, query);
            value.setGeometry(geometry);
        }
    }

    protected void addValidTime(DataEntity< ? > observation, AbstractValue< ? > value) {
        if (observation.isSetValidStartTime() || observation.isSetValidEndTime()) {
            Long validFrom = observation.isSetValidStartTime()
                    ? observation.getValidTimeStart()
                                 .getTime()
                    : null;
            Long validUntil = observation.isSetValidEndTime()
                    ? observation.getValidTimeEnd()
                                 .getTime()
                    : null;
            value.setValidTime(new ValidTime(validFrom, validUntil));
        }
    }

    protected void addResultTime(DataEntity< ? > observation, AbstractValue< ? > value) {
        if (observation.getResultTime() != null) {
            value.setResultTime(observation.getResultTime()
                                           .getTime());
        }
    }

    protected void addParameters(DataEntity< ? > observation, AbstractValue< ? > value, DbQuery query) {
        if (observation.hasParameters()) {
            for (Parameter< ? > parameter : observation.getParameters()) {
                value.addParameter(parameter.toValueMap(query.getLocale()));
            }
        }
    }

}
