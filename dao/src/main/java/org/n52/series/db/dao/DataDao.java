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

package org.n52.series.db.dao;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.criterion.Subqueries;
import org.joda.time.DateTime;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
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

    private static final Order DEFAULT_ORDER = Order.asc(DataEntity.PROPERTY_SAMPLING_TIME_END);

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
     * Retrieves all available observation instances.
     *
     * @param parameters query parameters.
     *
     * @return all instances matching the given query parameters.
     *
     * @throws DataAccessException if accessing database fails.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery parameters) throws DataAccessException {
        LOGGER.debug("get all instances: {}", parameters);
        Criteria criteria = getDefaultCriteria(parameters);
        parameters.addTimespanTo(criteria);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(toSQLString(criteria));
        }
        return criteria.list();
    }

    /**
     * Retrieves all available observation instances belonging to a particular series.
     *
     * @param dataset
     *        the dataset the observations belongs to.
     * @param query
     *        some query parameters to restrict result.
     * @return all observation entities belonging to the given series which match the given query.
     * @throws DataAccessException
     *         if accessing database fails.
     */
    @SuppressWarnings("unchecked")
    public List<T> getAllInstancesFor(DatasetEntity dataset, DbQuery query) throws DataAccessException {
        final Long id = dataset.getId();
        LOGGER.debug("get all instances for series '{}': {}", id, query);

        // TODO do we really want to allow mapping from observatin to dataset? PERFORMANCE LEAK
        final SimpleExpression equalsPkid = Restrictions.eq(DataEntity.PROPERTY_DATASET, id);
        Criteria criteria = getDefaultCriteria(query).add(equalsPkid);
        query.addTimespanTo(criteria);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public T getClosestOuterPreviousValue(final DatasetEntity dataset, final DateTime lowerBound, final DbQuery query) {
        final String column = DataEntity.PROPERTY_SAMPLING_TIME_START;
        final Order order = Order.desc(column);
        final Criteria criteria = createDataCriteria(column, dataset, query, order);
        return (T) criteria.add(Restrictions.lt(column, lowerBound.toDate()))
                           .setMaxResults(1)
                           .uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T getClosestOuterNextValue(final DatasetEntity dataset, final DateTime upperBound, final DbQuery query) {
        final String column = DataEntity.PROPERTY_SAMPLING_TIME_END;
        final Order order = Order.asc(column);
        final Criteria criteria = createDataCriteria(column, dataset, query, order);
        return (T) criteria.add(Restrictions.gt(column, upperBound.toDate()))
                           .setMaxResults(1)
                           .uniqueResult();
    }

    @Override
    protected Class<T> getEntityClass() {
        return entityType;
    }

    @Override
    protected String getDatasetProperty() {
        // there's no series property for observation
        return "";
    }

    @Override
    public Criteria getDefaultCriteria(final DbQuery query) {
        return getDefaultCriteria(query, DEFAULT_ORDER);
    }

    private Criteria getDefaultCriteria(final DbQuery query, Order order) {
        Criteria criteria = session.createCriteria(entityType)
                                   .add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE))
                                   // TODO check ordering when `showtimeintervals=true`
                                   .addOrder(order);
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        query.addSpatialFilter(criteria);
        query.addResultTimeFilter(criteria);
        query.addOdataFilterForData(criteria);

//        criteria = query.isComplexParent()
//                ? criteria.add(Restrictions.isNull(DataEntity.PROPERTY_PARENT))
//                : criteria.add(Restrictions.isNotNull(DataEntity.PROPERTY_PARENT));

        criteria.add(Restrictions.isNull(DataEntity.PROPERTY_PARENT));
        return criteria;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public T getDataValueViaTimeend(DatasetEntity series, DbQuery query) {
        Date timeend = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(timeend, DataEntity.PROPERTY_SAMPLING_TIME_END, series, query);
        return (T) criteria.uniqueResult();
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public T getDataValueViaTimestart(DatasetEntity series, DbQuery query) {
        Date timestart = series.getFirstValueAt();
        Criteria criteria = createDataAtCriteria(timestart, DataEntity.PROPERTY_SAMPLING_TIME_START, series, query);
        return (T) criteria.uniqueResult();
    }

    @Deprecated
    public GeometryEntity getValueGeometryViaTimeend(DatasetEntity series, DbQuery query) {
        Date lastValueAt = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(lastValueAt, DataEntity.PROPERTY_SAMPLING_TIME_END, series, query);
        criteria.setProjection(Projections.property(DataEntity.PROPERTY_GEOMETRY_ENTITY));
        return (GeometryEntity) criteria.uniqueResult();
    }

    private Criteria createDataAtCriteria(final Date timestamp,
                                          final String column,
                                          final DatasetEntity dataset,
                                          final DbQuery query) {
        LOGGER.debug("get data @{} for '{}'", new DateTime(timestamp.getTime()), dataset.getId());
        return createDataCriteria(column, dataset, query).add(Restrictions.eq(column, timestamp));
    }

    private Criteria createDataCriteria(String column, DatasetEntity dataset, DbQuery query) {
        return createDataCriteria(column, dataset, query, DEFAULT_ORDER);
    }

    private Criteria createDataCriteria(String column, DatasetEntity dataset, DbQuery query, Order order) {
        Criteria criteria = getDefaultCriteria(query);
        criteria.add(Restrictions.eq(DataEntity.PROPERTY_DATASET, dataset.getId()));

        IoParameters parameters = query.getParameters();
        if (parameters.isAllResultTimes()) {
            // no filter needed
            return criteria;
        } else if (!parameters.getResultTimes()
                              .isEmpty()) {
            // filter based on given result times
            return query.addResultTimeFilter(criteria);
        } else {
            // values for oldest result time
            String rtAlias = "rtAlias";
            String rtColumn = QueryUtils.createAssociation(rtAlias, column);
            String rtDatasetId = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_DATASET);
            String rtResultTime = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_RESULT_TIME);
            DetachedCriteria maxResultTimeQuery = DetachedCriteria.forClass(getEntityClass(), rtAlias);
            maxResultTimeQuery.add(Restrictions.eq(DataEntity.PROPERTY_DATASET, dataset.getId()))
                              .setProjection(Projections.projectionList()
                                                        .add(Projections.groupProperty(rtColumn))
                                                        .add(Projections.groupProperty(rtDatasetId))
                                                        .add(Projections.max(rtResultTime)));
            criteria.add(Subqueries.propertiesIn(new String[] {
                column,
                DataEntity.PROPERTY_DATASET,
                DataEntity.PROPERTY_RESULT_TIME
            }, maxResultTimeQuery));

        }
        return criteria;
    }

}
