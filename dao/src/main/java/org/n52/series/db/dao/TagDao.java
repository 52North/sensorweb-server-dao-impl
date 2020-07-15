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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.n52.series.db.beans.TagEntity;
import org.n52.series.db.beans.i18n.I18nTagEntity;

public class TagDao extends ParameterDao<TagEntity, I18nTagEntity> {

    private static final String DATASET_PROPERTY = "tags";

    public TagDao(Session session) {
        super(session);
    }

    @Override
    protected String getDatasetProperty() {
        return DATASET_PROPERTY;
    }

    @Override
    protected Class<TagEntity> getEntityClass() {
        return TagEntity.class;
    }

    @Override
    protected Class<I18nTagEntity> getI18NEntityClass() {
        return I18nTagEntity.class;
    }

    @Override
    protected Criteria getDefaultCriteria(String alias, DbQuery query, Class<?> clazz) {
        Criteria criteria = session.createCriteria(clazz);
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria;
    }

}
