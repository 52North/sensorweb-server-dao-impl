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
        Criteria criteria = query.addTimespanTo(getDefaultCriteria(query));
        return criteria.createCriteria(DataEntity.PROPERTY_DATASETS)
                       .add(Restrictions.eq(DataEntity.PROPERTY_PKID, pkid))
                       .list();
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
    public Criteria getDefaultCriteria(DbQuery query) {
        Criteria criteria = session.createCriteria(entityType)
                                   // TODO check ordering when `showtimeintervals=true`
                                   .addOrder(Order.asc(DataEntity.PROPERTY_PHENOMENON_TIME_END))
                                   .add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE));
        
        query.addSpatialFilter(criteria);
        query.addResultTimeFilter(criteria);

        criteria = query.isComplexParent()
                ? criteria.add(Restrictions.eq(DataEntity.PROPERTY_PARENT, true))
                : criteria.add(Restrictions.eq(DataEntity.PROPERTY_PARENT, false));

        return criteria;
    }

    @SuppressWarnings("unchecked")
    public T getDataValueViaTimeend(DatasetEntity series, DbQuery query) {
        Date timeend = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(timeend, DataEntity.PROPERTY_PHENOMENON_TIME_END, series, query);
        return (T) criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T getDataValueViaTimestart(DatasetEntity series, DbQuery query) {
        Date timestart = series.getFirstValueAt();
        Criteria criteria = createDataAtCriteria(timestart, DataEntity.PROPERTY_PHENOMENON_TIME_START, series, query);
        return (T) criteria.uniqueResult();
    }

    public GeometryEntity getValueGeometryViaTimeend(DatasetEntity series, DbQuery query) {
        Date lastValueAt = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(lastValueAt, DataEntity.PROPERTY_PHENOMENON_TIME_END, series, query);
        criteria.setProjection(Projections.property(DataEntity.PROPERTY_GEOMETRY_ENTITY));
        return (GeometryEntity) criteria.uniqueResult();
    }

    private Criteria createDataAtCriteria(Date timestamp, String column, DatasetEntity dataset, DbQuery query) {
        LOGGER.debug("get data @{} for '{}'", new DateTime(timestamp.getTime()), dataset.getPkid());
        String dsAlias = DatasetEntity.ENTITY_ALIAS;
        String dsId = QueryUtils.createAssociation(dsAlias, DatasetEntity.PROPERTY_PKID);
        Criteria criteria = getDefaultCriteria(query).add(Restrictions.eq(column, timestamp));
        criteria.createCriteria(DataEntity.PROPERTY_DATASETS, dsAlias)
                .add(Restrictions.eq(dsId, dataset.getPkid()));

        IoParameters parameters = query.getParameters();
        if (parameters.isAllResultTimes()) {
            // no filter needed
            return criteria;
        } else if (!parameters.getResultTimes()
                              .isEmpty()) {
            // filter based on given result times
            return query.addResultTimeFilter(criteria);
        } else {
            // project on oldest result time
            String rtAlias = "rt";
            String rtDatasetAlias = "rtDataset";
            String rtColumn = QueryUtils.createAssociation(rtAlias, column);
            String rtResultTime = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_RESULT_TIME);
            String rtDatasetId = QueryUtils.createAssociation(rtDatasetAlias, DatasetEntity.PROPERTY_PKID);
            DetachedCriteria resultTimeQuery = DetachedCriteria.forClass(getEntityClass(), rtAlias);
            resultTimeQuery.createCriteria(DataEntity.PROPERTY_DATASETS, rtDatasetAlias)
                           .add(Restrictions.eq(rtDatasetId, dataset.getPkid()))
                           .setProjection(Projections.projectionList()
                                                     .add(Projections.groupProperty(rtColumn))
                                                     .add(Projections.groupProperty(rtDatasetId))
                                                     .add(Projections.max(rtResultTime)));
            criteria.add(Subqueries.propertiesIn(new String[] {
                column,
                dsId,
                DataEntity.PROPERTY_RESULT_TIME
            }, resultTimeQuery));
        }
        return criteria;
    }

}
