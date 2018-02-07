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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.i18n.I18nFeatureEntity;
import org.n52.series.db.beans.i18n.I18nOfferingEntity;
import org.n52.series.db.beans.i18n.I18nPhenomenonEntity;
import org.n52.series.db.beans.i18n.I18nProcedureEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DatasetDao<T extends DatasetEntity> extends AbstractDao<T> implements SearchableDao<T> {

    public static final String FEATURE_PATH_ALIAS = "dsFeature";

    public static final String PROCEDURE_PATH_ALIAS = "dsProcedure";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetDao.class);

    private final Class<T> entityType;

    @SuppressWarnings("unchecked")
    public DatasetDao(Session session) {
        this(session, (Class<T>) DatasetEntity.class);
    }

    public DatasetDao(Session session, Class<T> clazz) {
        super(session);
        this.entityType = clazz;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> find(DbQuery query) {
        LOGGER.debug("find entities: {}", query);

        String searchTerm = "%" + query.getSearchTerm() + "%";

        /*
         * Timeseries labels are constructed from labels of related feature and phenomenon. Therefore we have
         * to join tables and search for given pattern on any of the stored labels.
         */
        Criteria criteria = getDefaultCriteria(query);
        // default criteria performs join on procedure table

        criteria.add(Restrictions.or(Restrictions.ilike(PhenomenonEntity.PROPERTY_NAME, searchTerm),
                                     Restrictions.ilike(ProcedureEntity.PROPERTY_NAME, searchTerm),
                                     Restrictions.ilike(OfferingEntity.PROPERTY_NAME, searchTerm),
                                     Restrictions.ilike(FeatureEntity.PROPERTY_NAME, searchTerm)));

        i18n(I18nOfferingEntity.class, criteria, query);
        i18n(I18nPhenomenonEntity.class, criteria, query);
        i18n(I18nProcedureEntity.class, criteria, query);
        i18n(I18nFeatureEntity.class, criteria, query);
        return criteria.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getInstance(Long key, DbQuery query) {
        Criteria criteria = getDefaultCriteria(getDefaultAlias(), false, query);
        return (T) criteria.add(Restrictions.eq(DescribableEntity.PROPERTY_ID, key))
                           .uniqueResult();
    }

    @Override
    protected T getInstance(String key, DbQuery query, Class<T> clazz) {
        return super.getInstance(key, query, clazz, getDefaultCriteria(null, false, query, clazz));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery query) throws DataAccessException {
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        return query.addFilters(criteria, getDatasetProperty())
                    .list();
    }

    @SuppressWarnings("unchecked")
    public List<T> getInstancesWith(FeatureEntity feature, DbQuery query) {
        LOGGER.debug("get instance for feature '{}'", feature);
        Criteria criteria = getDefaultCriteria(query);
        String path = QueryUtils.createAssociation(DatasetEntity.PROPERTY_FEATURE, DatasetEntity.PROPERTY_ID);
        return criteria.add(Restrictions.eq(path, feature.getId()))
                       .list();
    }

    @Override
    protected Class<T> getEntityClass() {
        return entityType;
    }

    @Override
    protected String getDatasetProperty() {
        // self has no property
        return "";
    }

    @Override
    protected String getDefaultAlias() {
        return DatasetEntity.ENTITY_ALIAS;
    }

    @Override
    public Criteria getDefaultCriteria(String alias, DbQuery query, Class< ? > clazz) {
        return getDefaultCriteria(alias, true, query, clazz);
    }

    private Criteria getDefaultCriteria(String alias, boolean ignoreReferenceSeries, DbQuery query) {
        return getDefaultCriteria(alias, ignoreReferenceSeries, query, getEntityClass());
    }

    private Criteria getDefaultCriteria(String alias, boolean ignoreReferenceSeries, DbQuery query, Class< ? > clazz) {
        Criteria criteria = super.getDefaultCriteria(alias, query, clazz);

        Criteria procCriteria = criteria.createCriteria(DatasetEntity.PROPERTY_PROCEDURE, PROCEDURE_PATH_ALIAS);
        if (ignoreReferenceSeries) {
            procCriteria.add(Restrictions.eq(ProcedureEntity.PROPERTY_REFERENCE, Boolean.FALSE));
        }

        return criteria;
    }

    @Override
    protected Criteria addDatasetFilters(DbQuery query, Criteria criteria) {
        // on dataset itself there is no explicit join neccessary
        Criteria filter = criteria.add(createPublishedDatasetFilter());
        query.addSpatialFilter(filter.createCriteria(DatasetEntity.PROPERTY_FEATURE,
                                                     FEATURE_PATH_ALIAS,
                                                     JoinType.LEFT_OUTER_JOIN));
        return criteria;
    }

}
