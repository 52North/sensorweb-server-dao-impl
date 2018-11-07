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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.transform.DistinctResultTransformer;
import org.n52.io.request.FilterResolver;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.IdEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.i18n.I18nPlatformEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlatformDao extends ParameterDao<PlatformEntity, I18nPlatformEntity> {

    public PlatformDao(Session session) {
        super(session);
    }

    @Override
    public Integer getCount(DbQuery query) throws DataAccessException {
        DetachedCriteria mobile = QueryUtils.projectionOn(DatasetEntity.PROPERTY_PROCEDURE, createMobileFilter(true))
                                            .setResultTransformer(DistinctResultTransformer.INSTANCE);
        DetachedCriteria stationary = QueryUtils.projectionOn(DatasetEntity.PROPERTY_FEATURE, createMobileFilter(false))
                                                .setResultTransformer(DistinctResultTransformer.INSTANCE);

        DatasetDao<DatasetEntity> datasetDao = new DatasetDao<>(session);
        return (int) Long.sum(count(stationary, datasetDao, query),
                              count(mobile, datasetDao, query));
    }

    private Long count(DetachedCriteria subquery, AbstractDao< ? > dao, DbQuery query) {
        Criteria criteria = dao.getDefaultCriteria(query);
        Criteria elements = criteria.add(Subqueries.propertyIn(IdEntity.PROPERTY_ID, subquery));
        return (Long) elements.setProjection(Projections.rowCount())
                              .uniqueResult();
    }

    private DetachedCriteria createMobileFilter(boolean mobile) {
        DetachedCriteria criteria = DetachedCriteria.forClass(DatasetEntity.class);
        criteria.add(Restrictions.eq(DatasetEntity.PROPERTY_MOBILE, mobile));
        return criteria;
    }

    @Override
    protected String getDatasetProperty() {
        return DatasetEntity.PROPERTY_PROCEDURE;
    }

    @Override
    protected Class<PlatformEntity> getEntityClass() {
        return PlatformEntity.class;
    }

    @Override
    protected Class<I18nPlatformEntity> getI18NEntityClass() {
        return I18nPlatformEntity.class;
    }

    @Override
    protected DetachedCriteria addSpatialFilter(DbQuery query, DetachedCriteria criteria) {
        /*
         * Filters for the last known location (max result time) of a mobile platform
         *
         * Here we do have to consider mobile variants only (mobile=true has been set beforehand) as
         * repository decides already which DAO is used to query stationary (--> FeatureDao) and mobile
         * platforms
         */
        FilterResolver filterResolver = query.getFilterResolver();
        if (filterResolver.isSetMobileFilter()) {

            // values for oldest result time
            String rtAlias = "rtAlias";
            // String rtColumn = QueryUtils.createAssociation(rtAlias, column);
            String rtDatasetId = QueryUtils.createAssociation(rtAlias, IdEntity.PROPERTY_ID);
            String rtResultTime = QueryUtils.createAssociation(rtAlias, DataEntity.PROPERTY_RESULT_TIME);

            DetachedCriteria maxResultTimeByDatasetId = DetachedCriteria.forClass(DataEntity.class, rtAlias);
            maxResultTimeByDatasetId.setProjection(Projections.projectionList()
                                                              // .add(Projections.groupProperty(rtColumn))
                                                              .add(Projections.groupProperty(rtDatasetId))
                                                              .add(Projections.max(rtResultTime)));

            String[] matchProperties = new String[] {
                                                     IdEntity.PROPERTY_ID,
                                                     // DataEntity.PROPERTY_SERIES_PKID,
                                                     DataEntity.PROPERTY_RESULT_TIME
            };
            DetachedCriteria observationCriteria = query.addSpatialFilter(DetachedCriteria.forClass(DataEntity.class))
                                                        .add(Subqueries.propertiesIn(matchProperties,
                                                                                     maxResultTimeByDatasetId))
                                                        .createCriteria(DataEntity.PROPERTY_DATASET)
                                                        .setProjection(Projections.property(DataEntity.PROPERTY_ID));

            criteria.add(Subqueries.propertyIn(IdEntity.PROPERTY_ID, observationCriteria));
        }

        return criteria;
    }

}
