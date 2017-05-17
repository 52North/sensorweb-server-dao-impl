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
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.I18nEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDao<T> implements GenericDao<T, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDao.class);

    private final DbQueryFactory queryFactory;

    protected Session session;

    public AbstractDao(DbQueryFactory queryFactory, Session session) {
        if (session == null) {
            throw new NullPointerException("Cannot operate on a null session.");
        }
        this.queryFactory = queryFactory;
        this.session = session;
    }

    public abstract List<T> find(DbQuery query);

    protected abstract Class<T> getEntityClass();

    protected abstract String getDatasetProperty();

    protected String getDefaultAlias() {
        return getDatasetProperty();
    }

    protected DbQuery getQueryDefaults() {
        return queryFactory.createFrom(IoParameters.createDefaults());
    }

    public boolean hasInstance(String id, DbQuery query, Class< ? extends T> clazz) throws DataAccessException {
        return getInstance(id, query) != null;
    }

    @Override
    public boolean hasInstance(Long id, DbQuery query, Class< ? extends T> clazz) {
        return session.get(clazz, id) != null;
    }

    public T getInstance(String key, DbQuery parameters) throws DataAccessException {
        if (!parameters.getParameters()
                        .isMatchDomainIds()) {
            return getInstance(Long.parseLong(key), parameters);
        }

        LOGGER.debug("get dataset type for '{}'. {}", key, parameters);
        Criteria criteria = getDefaultCriteria();
        return getEntityClass().cast(criteria.add(Restrictions.eq("domainId", key))
                                             .uniqueResult());
    }

    @Override
    public T getInstance(Long key, DbQuery parameters) throws DataAccessException {
        LOGGER.debug("get instance '{}': {}", key, parameters);
        Criteria criteria = getDefaultCriteria();
        return getEntityClass().cast(criteria.add(Restrictions.eq("pkid", key))
                                             .uniqueResult());
    }

    @Override
    public Integer getCount(DbQuery query) throws DataAccessException {
        Criteria criteria = getDefaultCriteria().setProjection(Projections.rowCount());
        return ((Long) query.addFilters(criteria, getDatasetProperty())
                            .uniqueResult()).intValue();
    }

    protected <I extends I18nEntity> Criteria i18n(Class<I> clazz, Criteria criteria, DbQuery query) {
        return hasTranslation(query, clazz)
                ? query.addLocaleTo(criteria, clazz)
                : criteria;
    }

    private <I extends I18nEntity> boolean hasTranslation(DbQuery parameters, Class<I> clazz) {
        Criteria i18nCriteria = session.createCriteria(clazz);
        return parameters.checkTranslationForLocale(i18nCriteria);
    }

//    protected Criteria getDefaultCriteria() {
//        return getDefaultCriteria(getDatasetProperty());
//    }
//
//    protected Criteria getDefaultCriteria(String alias) {
//        return alias == null || alias.isEmpty()
//                ? session.createCriteria(getEntityClass())
//                : session.createCriteria(getEntityClass(), alias);
//    }


    protected Criteria getDefaultCriteria() {
        return getDefaultCriteria((String) null);
    }

    protected Criteria getDefaultCriteria(String alias) {
        return getDefaultCriteria(alias, queryFactory.createFrom(IoParameters.createDefaults()));
    }

    protected Criteria getDefaultCriteria(String alias, DbQuery query) {
        alias = alias != null ? alias : getDefaultAlias();
//        DetachedCriteria filter = createSeriesSubQuery(alias, query);
        DetachedCriteria filter = createSeriesSubQueryViaExplicitJoin(alias, query);
        return session.createCriteria(getEntityClass(), alias)
                .add(Subqueries.propertyIn("pkid", filter));
    }

    private DetachedCriteria createSeriesSubQueryViaExplicitJoin(String alias, DbQuery query) {
        return DetachedCriteria.forClass(DatasetEntity.class)
                .add(Restrictions.eq("published", Boolean.TRUE))
                .createAlias(alias, "ref")
                .setProjection(Projections.property("ref.pkid"));
    }

    private DetachedCriteria createSeriesSubQuery(String alias, DbQuery query) {
        String filterProperty = alias != null && !alias.isEmpty()
                ? alias + ".pkid"
                : "pkid";
        return DetachedCriteria.forClass(DatasetEntity.class)
                .add(Restrictions.eq("published", Boolean.TRUE))
                // XXX NPE when filterProperty is mapped by formula
                .setProjection(Projections.property(filterProperty));
    }

}
