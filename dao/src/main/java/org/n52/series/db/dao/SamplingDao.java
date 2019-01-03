/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.i18n.I18nSamplingEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamplingDao extends AbstractDao<SamplingEntity> implements SearchableDao<SamplingEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamplingDao.class);

    public SamplingDao(Session session) {
        super(session);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SamplingEntity> find(DbQuery query) {
        LOGGER.debug("find instance: {}", query);
//        Criteria criteria = getDefaultCriteria(query);
        Criteria criteria = session.createCriteria(getEntityClass());
        criteria = i18n(getI18NEntityClass(), criteria, query);
        criteria.add(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, "%" + query.getSearchTerm() + "%"));
        return criteria.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SamplingEntity> getAllInstances(DbQuery query) throws DataAccessException {
        LOGGER.debug("get all instances: {}", query);
//        Criteria criteria = getDefaultCriteria(query);
        Criteria criteria = session.createCriteria(getEntityClass());
        criteria = i18n(getI18NEntityClass(), criteria, query);
        return criteria.list();
    }

    @Override
    protected SamplingEntity getInstance(String key, DbQuery query, Class<SamplingEntity> clazz) {
        LOGGER.debug("get instance for '{}'. {}", key, query);
        Criteria criteria = session.createCriteria(clazz);
        return getInstance(key, query, clazz, criteria);
    }

    protected Class<I18nSamplingEntity> getI18NEntityClass() {
        return I18nSamplingEntity.class;
    }

    @Override
    protected Class<SamplingEntity> getEntityClass() {
        return SamplingEntity.class;
    }

    @Override
    protected String getDatasetProperty() {
        // TODO Auto-generated method stub
        return "";
    }

}
