/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
import java.util.Collections;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.i18n.I18nProcedureEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ProcedureDao extends HierarchicalDao<ProcedureEntity, I18nProcedureEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcedureDao.class);

    private static final String COLUMN_REFERENCE = "reference";

    public ProcedureDao(Session session) {
        super(session);
    }

    @Override
    public ProcedureEntity getInstance(Long key, DbQuery query) {
        LOGGER.debug("get instance '{}': {}", key, query);
        Criteria criteria = getDefaultCriteria(query);
        return getEntityClass().cast(criteria.add(Restrictions.eq(DescribableEntity.PROPERTY_ID, key))
                                             .uniqueResult());
    }

    @Override
    public Set<Long> getChildrenIds(DbQuery query) {
        Set<String> procedures = query.getParameters().getProcedures();
        if (procedures != null && !procedures.isEmpty()) {
           return getChildrenIds(query, procedures, query.getLevel());
        }
        return Collections.emptySet();
    }

    @Override
    public Criteria getDefaultCriteria(DbQuery query) {
        return getDefaultCriteria(query, true);
    }

    private Criteria getDefaultCriteria(DbQuery query, boolean ignoreReferenceProcedures) {
        return ignoreReferenceProcedures
                ? super.getDefaultCriteria(query).add(Restrictions.eq(COLUMN_REFERENCE, Boolean.FALSE))
                : super.getDefaultCriteria(query);
    }

    @Override
    protected String getDatasetProperty() {
        return DatasetEntity.PROPERTY_PROCEDURE;
    }

    @Override
    protected Class<ProcedureEntity> getEntityClass() {
        return ProcedureEntity.class;
    }

    @Override
    protected Class<I18nProcedureEntity> getI18NEntityClass() {
        return I18nProcedureEntity.class;
    }

    @Override
    protected Set<String> getParameter(DbQuery query) {
        return query.getParameters().getProcedures();
    }

    @Override
    protected IoParameters replaceParameter(DbQuery query, Collection<String> entities) {
        return query.getParameters().replaceWith(Parameters.PROCEDURES, entities);
    }

}
