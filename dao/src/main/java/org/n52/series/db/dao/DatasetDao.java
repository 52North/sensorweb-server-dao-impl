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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.I18nFeatureEntity;
import org.n52.series.db.beans.I18nOfferingEntity;
import org.n52.series.db.beans.I18nPhenomenonEntity;
import org.n52.series.db.beans.I18nProcedureEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SuppressWarnings("rawtypes")
public class DatasetDao<T extends DatasetEntity> extends AbstractDao<T> implements SearchableDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetDao.class);

    private static final String COLUMN_PKID = "pkid";

    private static final String FEATURE_PATH_ALIAS = "dsFeature";

    private static final String PROCEDURE_PATH_ALIAS = "dsProcedure";

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

        List<T> series = new ArrayList<>();
        String searchTerm = "%" + query.getSearchTerm() + "%";

        /*
         * Timeseries labels are constructed from labels of related feature and phenomenon. Therefore we have
         * to join both tables and search for given pattern on any of the stored labels.
         */
        Criteria criteria = getDefaultCriteria("s", query);

//        Criteria featureCriteria = criteria.createCriteria(DatasetEntity.PROPERTY_FEATURE, JoinType.LEFT_OUTER_JOIN);
        Criteria featureCriteria = criteria;
        featureCriteria = i18n(I18nFeatureEntity.class, featureCriteria, query);
        String featureName = QueryUtils.createAssociation(FEATURE_PATH_ALIAS, FeatureEntity.PROPERTY_NAME);
        featureCriteria.add(Restrictions.ilike(featureName, searchTerm));
        series.addAll(featureCriteria.list());

//        Criteria procedureCriteria = criteria.createCriteria(DatasetEntity.PROPERTY_PROCEDURE,
//                                                             JoinType.LEFT_OUTER_JOIN);
        Criteria procedureCriteria = criteria;
        procedureCriteria = i18n(I18nProcedureEntity.class, procedureCriteria, query);
        String procedureName = QueryUtils.createAssociation(PROCEDURE_PATH_ALIAS, ProcedureEntity.PROPERTY_NAME);
        procedureCriteria.add(Restrictions.ilike(procedureName, searchTerm));
        series.addAll(procedureCriteria.list());

        Criteria offeringCriteria = criteria.createCriteria(DatasetEntity.PROPERTY_OFFERING,
                                                            JoinType.LEFT_OUTER_JOIN);
        offeringCriteria = i18n(I18nOfferingEntity.class, offeringCriteria, query);
        offeringCriteria.add(Restrictions.ilike(OfferingEntity.PROPERTY_NAME, searchTerm));
        series.addAll(offeringCriteria.list());

        Criteria phenomenonCriteria = criteria.createCriteria(DatasetEntity.PROPERTY_PHENOMENON,
                                                              JoinType.LEFT_OUTER_JOIN);
        phenomenonCriteria = i18n(I18nPhenomenonEntity.class, phenomenonCriteria, query);
        phenomenonCriteria.add(Restrictions.ilike(PhenomenonEntity.PROPERTY_NAME, searchTerm));
        series.addAll(phenomenonCriteria.list());

        return series;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getInstance(Long key, DbQuery query) throws DataAccessException {
        Criteria criteria = getDefaultCriteria(getDefaultAlias(), false, query);
        return (T) criteria.add(Restrictions.eq(COLUMN_PKID, key))
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
        Criteria criteria = query.addFilters(getDefaultCriteria(query), getDatasetProperty());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(toSQLString(criteria));
        }
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<T> getInstancesWith(FeatureEntity feature, DbQuery query) {
        LOGGER.debug("get instance for feature '{}'", feature);
        Criteria criteria = getDefaultCriteria(query);
        String idColumn = QueryUtils.createAssociation(FEATURE_PATH_ALIAS, COLUMN_PKID);
        return criteria.add(Restrictions.eq(idColumn, feature.getPkid()))
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
    protected Criteria getDefaultCriteria(String alias, DbQuery query, Class< ? > clazz) {
        // declare explicit alias here
        return getDefaultCriteria(alias, true, query, clazz);
    }

    private Criteria getDefaultCriteria(String alias, boolean ignoreReferenceSeries, DbQuery query) {
        return getDefaultCriteria(alias, ignoreReferenceSeries, query, getEntityClass());
    }

    private Criteria getDefaultCriteria(String alias, boolean ignoreReferenceSeries, DbQuery query, Class< ? > clazz) {
        Criteria criteria = super.getDefaultCriteria(alias, query, clazz);

        if (ignoreReferenceSeries) {
            criteria.createCriteria(DatasetEntity.PROPERTY_PROCEDURE, PROCEDURE_PATH_ALIAS, JoinType.LEFT_OUTER_JOIN)
                    .add(Restrictions.eq(ProcedureEntity.PROPERTY_DELETED, Boolean.FALSE))
                    .add(Restrictions.eq(ProcedureEntity.PROPERTY_REFERENCE, Boolean.FALSE));
        }

        query.addOdataFilterForDataset(criteria);

        return criteria;
    }

    @Override
    protected Criteria addDatasetFilters(DbQuery query, Criteria criteria) {
        // on dataset itself there is no explicit join neccessary
        Criteria filter = criteria.add(createPublishedDatasetFilter());
        query.addDatasetTimespan(criteria);
        query.addSpatialFilter(filter.createCriteria(DatasetEntity.PROPERTY_FEATURE,
                                                     FEATURE_PATH_ALIAS,
                                                     JoinType.LEFT_OUTER_JOIN));
        return criteria;
    }

}
