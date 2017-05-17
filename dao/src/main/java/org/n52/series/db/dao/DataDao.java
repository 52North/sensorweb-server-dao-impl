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

package org.n52.series.db.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.criterion.Subqueries;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 * @param <T>
 *        the data entity type
 */
@Transactional
@SuppressWarnings("rawtypes")
public class DataDao<T extends DataEntity> extends AbstractDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDao.class);

    private static final String COLUMN_SERIES_PKID = "seriesPkid";

    private static final String COLUMN_DELETED = "deleted";

    private static final String COLUMN_RESULTTIME = "resultTime";

    private static final String COLUMN_TIMESTART = "timestart";

    private static final String COLUMN_TIMEEND = "timeend";

    private static final String COLUMN_PARENT = "parent";

    private final Class<T> entityType;

    @SuppressWarnings("unchecked")
    public DataDao(Session session) {
        this(session, (Class<T>) DataEntity.class);
    }

    public DataDao(Session session, Class<T> clazz) {
        super(session);
        this.entityType = clazz;
    }

    @Override
    public T getInstance(Long key, DbQuery parameters) throws DataAccessException {
        LOGGER.debug("get instance '{}': {}", key, parameters);
        return entityType.cast(session.get(entityType, key));
    }

    /**
     * <p>
     * Retrieves all available observation instances.
     * </p>
     *
     * @param parameters
     *        query parameters.
     * @return all instances matching the given query parameters.
     * @throws DataAccessException
     *         if accessing database fails.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery parameters) throws DataAccessException {
        LOGGER.debug("get all instances: {}", parameters);
        Criteria criteria = getDefaultCriteria(parameters);
        parameters.addTimespanTo(criteria);
        return criteria.list();
    }

    /**
     * Retrieves all available observation instances belonging to a particular series.
     *
     * @param series
     *        the series the observations belongs to.
     * @param query
     *        some query parameters to restrict result.
     * @return all observation entities belonging to the given series which match the given query.
     * @throws DataAccessException
     *         if accessing database fails.
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllInstancesFor(DatasetEntity series, DbQuery query) throws DataAccessException {
        final Long pkid = series.getPkid();
        LOGGER.debug("get all instances for series '{}': {}", pkid, query);
        final SimpleExpression equalsPkid = Restrictions.eq(COLUMN_SERIES_PKID, pkid);
        Criteria criteria = getDefaultCriteria(query).add(equalsPkid);
        query.addTimespanTo(criteria);
        return criteria.list();
    }

    @Override
    protected String getDatasetProperty() {
        // there's no series property for observation
        return "";
    }

    @Override
    protected Criteria getDefaultCriteria(DbQuery parameters) {
        Criteria criteria = session.createCriteria(entityType)
                                   // TODO check ordering when `showtimeintervals=true`
                                   .addOrder(Order.asc(COLUMN_TIMEEND))
                                   .add(Restrictions.eq(COLUMN_DELETED, Boolean.FALSE));

        if (parameters != null && parameters.getResultTime() != null) {
            criteria = criteria.add(Restrictions.eq(COLUMN_RESULTTIME, parameters.getResultTime()));
        }

        criteria = parameters != null && parameters.isComplexParent()
                ? criteria.add(Restrictions.eq(COLUMN_PARENT, true))
                : criteria.add(Restrictions.eq(COLUMN_PARENT, false));

        return criteria;
    }

    @Override
    protected Class<T> getEntityClass() {
        return entityType;
    }

    public T getDataValueViaTimeend(DatasetEntity series, DbQuery query) {
        Date timeend = series.getLastValueAt();
        return getDataValueAt(timeend, COLUMN_TIMEEND, series, query);
    }

    public T getDataValueViaTimestart(DatasetEntity series, DbQuery query) {
        Date timestart = series.getFirstValueAt();
        return getDataValueAt(timestart, COLUMN_TIMESTART, series, query);
    }

    @SuppressWarnings("unchecked")
    private T getDataValueAt(Date timestamp, String column, DatasetEntity series, DbQuery query) {
        LOGGER.debug("get instances @{} for '{}'", new DateTime(timestamp.getTime()), series.getPkid());
        Criteria criteria = getDefaultCriteria(query).add(Restrictions.eq(COLUMN_SERIES_PKID, series.getPkid()))
                                                .add(Restrictions.eq(column, timestamp));

        DetachedCriteria filter = DetachedCriteria.forClass(DatasetEntity.class)
                                                  .setProjection(Projections.projectionList()
                                                                            .add(Projections.property("pkid")));
        criteria.add(Subqueries.propertyIn(COLUMN_SERIES_PKID, filter));

        IoParameters parameters = query.getParameters();
        if (parameters.containsParameter(Parameters.RESULTTIME)) {
            Instant resultTime = parameters.getResultTime();
            criteria.add(Restrictions.eq(COLUMN_RESULTTIME, resultTime.toDate()));
            return (T) criteria.uniqueResult();
        } else {
            List<T> list = criteria.list();
            return getLastValueWhenMultipleResultTimesAvailable(list);
        }
    }

    private T getLastValueWhenMultipleResultTimesAvailable(List<T> values) {
        T lastValue = null;
        for (T value : values) {
            lastValue = lastValue != null
                    ? lastValue
                    : value;
            Date lastResultTime = lastValue.getResultTime();
            Date resultTime = value.getResultTime();
            if (new Instant(resultTime).isAfter(new Instant(lastResultTime))) {
                lastValue = value;
            }
        }
        return lastValue;
    }

}
