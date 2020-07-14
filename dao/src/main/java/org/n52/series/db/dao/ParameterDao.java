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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.n52.io.request.Parameters;
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
    public List<T> find(DbQuery query) {
        LOGGER.debug("find instance: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        criteria.add(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, "%" + query.getSearchTerm() + "%"));
        return query.addFilters(criteria, getDatasetProperty()).list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery query) throws DataAccessException {
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        return query.addFilters(criteria, getDatasetProperty()).list();
    }

    protected DbQuery checkLevelParameterForHierarchyQuery(DbQuery query) {
        if (query.getLevel() != null) {
            if (query.getParameters().containsParameter(Parameters.FEATURES) && !(this instanceof FeatureDao)) {
                Collection<Long> ids = new FeatureDao(session).getChildrenIds(query);
                if (ids != null && !ids.isEmpty()) {
                    return new DbQuery(query.getParameters().extendWith(Parameters.FEATURES, toStringList(ids)));
                }
            } else if (query.getParameters().containsParameter(Parameters.PROCEDURES)
                    && !(this instanceof ProcedureDao)) {
                Collection<Long> ids = new ProcedureDao(session).getChildrenIds(query);
                if (ids != null && !ids.isEmpty()) {
                    return new DbQuery(query.getParameters().extendWith(Parameters.PROCEDURES, toStringList(ids)));
                }
            }
        }
        return query;
    }

    protected List<String> toStringList(Collection<Long> set) {
        return set.stream().map(s -> s.toString()).collect(Collectors.toList());
    }

}
