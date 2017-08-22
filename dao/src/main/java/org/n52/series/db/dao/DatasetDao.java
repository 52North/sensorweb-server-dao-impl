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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.ObservationConstellationEntity;
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

    public static final String PROCEDURE_ALIAS = "proc";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetDao.class);

    private static final String COLUMN_PKID = "pkid";

    private static final String OFFERING_ALIAS = "off";

    private static final String FEATURE_ALIAS = "feat";

    private static final String PHENOMENON_ALIAS = "phen";

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
        constellationJoin(ObservationConstellationEntity.OFFERING, OFFERING_ALIAS, criteria);
        constellationJoin(ObservationConstellationEntity.OBSERVABLE_PROPERTY, PHENOMENON_ALIAS, criteria);
        criteria.createCriteria(DatasetEntity.PROPERTY_FEATURE, FEATURE_ALIAS, JoinType.LEFT_OUTER_JOIN);

        String phenomenonName = QueryUtils.createAssociation(PHENOMENON_ALIAS, PhenomenonEntity.PROPERTY_NAME);
        String procedureName = QueryUtils.createAssociation(PROCEDURE_ALIAS, ProcedureEntity.PROPERTY_NAME);
        String offeringName = QueryUtils.createAssociation(OFFERING_ALIAS, OfferingEntity.PROPERTY_NAME);
        String featureName = QueryUtils.createAssociation(FEATURE_ALIAS, FeatureEntity.PROPERTY_NAME);
        criteria.add(Restrictions.or(Restrictions.ilike(procedureName, searchTerm),
                                     Restrictions.ilike(offeringName, searchTerm),
                                     Restrictions.ilike(phenomenonName, searchTerm),
                                     Restrictions.ilike(featureName, searchTerm)));

        i18n(I18nOfferingEntity.class, criteria, query);
        i18n(I18nPhenomenonEntity.class, criteria, query);
        i18n(I18nProcedureEntity.class, criteria, query);
        i18n(I18nFeatureEntity.class, criteria, query);
        return criteria.list();
    }

    private Criteria constellationJoin(String accosiationPath, String targetAlias, Criteria criteria) {
        String member = QueryUtils.createAssociation(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION, accosiationPath);
        return criteria.createCriteria(member, targetAlias, JoinType.LEFT_OUTER_JOIN);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getInstance(Long key, DbQuery query) throws DataAccessException {
        Criteria criteria = getDefaultCriteria(getDefaultAlias(), false, query);
        return (T) criteria.add(Restrictions.eq(COLUMN_PKID, key))
                           .uniqueResult();
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
        return criteria.createCriteria(DatasetEntity.PROPERTY_FEATURE, JoinType.LEFT_OUTER_JOIN)
                       .add(Restrictions.eq(COLUMN_PKID, feature.getPkid()))
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
        Criteria criteria = session.createCriteria(clazz)
                                   .add(createPublishedDatasetFilter());
        criteria.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION)
                .createCriteria(ObservationConstellationEntity.PROCEDURE, PROCEDURE_ALIAS);

        query.addValueTypeFilter("", criteria);
        query.addPlatformTypeFilter("", criteria);
        String refMember = QueryUtils.createAssociation(PROCEDURE_ALIAS, ProcedureEntity.PROPERTY_REFERENCE);
        return ignoreReferenceSeries
                ? criteria.add(Restrictions.eq(refMember, Boolean.FALSE))
                : criteria;
    }

}
