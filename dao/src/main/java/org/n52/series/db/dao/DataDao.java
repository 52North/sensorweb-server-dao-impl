/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.criterion.Subqueries;
import org.joda.time.DateTime;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.CategoryDataEntity;
import org.n52.series.db.beans.CategoryDatasetEntity;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.CountDatasetEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.RecordDataEntity;
import org.n52.series.db.beans.RecordDatasetEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.series.db.beans.TextDatasetEntity;
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
        final SimpleExpression equalsPkid = Restrictions.eq(DataEntity.PROPERTY_SERIES_PKID, pkid);
        Criteria criteria = getDefaultCriteria(query, series).add(equalsPkid);
        query.addTimespanTo(criteria);
        if (query.isExpanded() && (!query.getParameters().containsParameter(Parameters.FORMAT)
                || !("highcharts".equalsIgnoreCase(query.getParameters().getFormat())
                        || "flotcharts".equalsIgnoreCase(query.getParameters().getFormat())
                        || "flot".equalsIgnoreCase(query.getParameters().getFormat())))) {
            // force joining of parameters for expanded query to avoid x separate queries
            criteria.setFetchMode("parameters", FetchMode.JOIN);
        }
        return criteria.list();
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

    private Criteria getDefaultCriteria(DbQuery query, DatasetEntity series) {
        Class<?> specific = entityType;
        if (series instanceof QuantityDatasetEntity) {
            specific = QuantityDataEntity.class;
        } else if (series instanceof CategoryDatasetEntity) {
            specific = CategoryDataEntity.class;
        } else if (series instanceof CountDatasetEntity) {
            specific = CountDataEntity.class;
        } else if (series instanceof TextDatasetEntity) {
            specific = TextDataEntity.class;
        } else if (series instanceof ProfileDatasetEntity) {
            specific = ProfileDataEntity.class;
        } else if (series instanceof RecordDatasetEntity) {
            specific = RecordDataEntity.class;
        }
        return addRestrictions(session.createCriteria(specific), query);
    }

    @Override
    public Criteria getDefaultCriteria(DbQuery query) {
        return addRestrictions(session.createCriteria(entityType), query);
    }

    private Criteria addRestrictions(Criteria criteria, DbQuery query) {
        criteria.addOrder(Order.asc(DataEntity.PROPERTY_TIMEEND))
                .add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        query.addSpatialFilter(criteria);
        query.addResultTimeFilter(criteria);
        query.addOdataFilterForData(criteria);

        return query.isComplexParent() ? criteria.add(Restrictions.eq(DataEntity.PROPERTY_PARENT, true))
                : criteria.add(Restrictions.eq(DataEntity.PROPERTY_PARENT, false));
    }

    @SuppressWarnings("unchecked")
    public T getDataValueViaTimeend(DatasetEntity series, DbQuery query) {
        Date timeend = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(timeend, DataEntity.PROPERTY_TIMEEND, series, query);
        return (T) criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T getDataValueViaTimestart(DatasetEntity series, DbQuery query) {
        Date timestart = series.getFirstValueAt();
        Criteria criteria = createDataAtCriteria(timestart, DataEntity.PROPERTY_TIMESTART, series, query);
        return (T) criteria.uniqueResult();
    }

    public GeometryEntity getValueGeometryViaTimeend(DatasetEntity series, DbQuery query) {
        Date lastValueAt = series.getLastValueAt();
        Criteria criteria = createDataAtCriteria(lastValueAt, DataEntity.PROPERTY_TIMEEND, series, query);
        criteria.setProjection(Projections.property(DataEntity.PROPERTY_GEOMETRY_ENTITY));
        return (GeometryEntity) criteria.uniqueResult();
    }

    private Criteria createDataAtCriteria(Date timestamp, String column, DatasetEntity dataset, DbQuery query) {
        LOGGER.debug("get data @{} for '{}'", new DateTime(timestamp.getTime()), dataset.getPkid());
        Criteria criteria = getDefaultCriteria(query).add(Restrictions.eq(column, timestamp))
                                                     .add(Restrictions.eq(DataEntity.PROPERTY_SERIES_PKID,
                                                                          dataset.getPkid()));

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
            String rtDatasetId = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_SERIES_PKID);
            String rtResultTime = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_RESULTTIME);
            DetachedCriteria maxResultTimeQuery = DetachedCriteria.forClass(getEntityClass(), rtAlias);
            maxResultTimeQuery.add(Restrictions.eq(DataEntity.PROPERTY_SERIES_PKID, dataset.getPkid()))
                              .setProjection(Projections.projectionList()
                                                        .add(Projections.groupProperty(rtColumn))
                                                        .add(Projections.groupProperty(rtDatasetId))
                                                        .add(Projections.max(rtResultTime)));
            criteria.add(Subqueries.propertiesIn(new String[] {
                column,
                DataEntity.PROPERTY_SERIES_PKID,
                DataEntity.PROPERTY_RESULTTIME
            }, maxResultTimeQuery));

        }
        return criteria;
    }

}
