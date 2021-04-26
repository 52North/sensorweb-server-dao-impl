/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.i18n.I18nEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParameterDao<T extends DescribableEntity, I extends I18nEntity<T>> extends AbstractDao<T>
        implements SearchableDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterDao.class);

    public ParameterDao(Session session) {
        super(session);
    }

    protected abstract Class<I> getI18NEntityClass();

    @Override
    @SuppressWarnings("unchecked")
    public List<T> find(DbQuery q) {
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        LOGGER.debug("find instance: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        addFetchModes(criteria, query);
        if (query.getParameters().getLocale() != null || !query.getParameters().getLocale().isEmpty()) {
            criteria = i18n(getI18NEntityClass(), criteria, query);
        }
        criteria.add(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, "%" + query.getSearchTerm() + "%"));
        criteria = query.addFilters(criteria, getDatasetProperty(), session);
        long start = System.currentTimeMillis();
        try {
            return criteria.list();
        } finally {
            logProcessingTime(start);
        }
    }

    private void logProcessingTime(long start) {
        LOGGER.debug("Querying all instances takes {} ms", System.currentTimeMillis() - start);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery q) throws DataAccessException {
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        addFetchModes(criteria, query);
        if (query.getParameters().getLocale() != null && !query.getParameters().getLocale().isEmpty()) {
            criteria = i18n(getI18NEntityClass(), criteria, query);
        }
        criteria = query.addFilters(criteria, getDatasetProperty(), session);
        long start = System.currentTimeMillis();
        try {
            return criteria.list();
        } finally {
            logProcessingTime(start);
        }
    }

    @Override
    protected Criteria addFetchModes(Criteria criteria, DbQuery query) {
        if (query.getParameters().getLocale() != null && !query.getParameters().getLocale().isEmpty()) {
            criteria.setFetchMode(TRANSLATIONS_ALIAS, FetchMode.JOIN);
        }
        return criteria;
    }

}
