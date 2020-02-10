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
package org.n52.series.db.old.dao;

import java.util.Collection;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.RootEntityResultTransformer;
import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.i18n.I18nFeatureEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class FeatureDao extends ParameterDao<FeatureEntity, I18nFeatureEntity> {

    public FeatureDao(Session session) {
        super(session);
    }

    @Override
    protected String getDatasetProperty() {
        return DatasetEntity.PROPERTY_FEATURE;
    }

    @Override
    protected Class<FeatureEntity> getEntityClass() {
        return FeatureEntity.class;
    }

    @Override
    protected Class<I18nFeatureEntity> getI18NEntityClass() {
        return I18nFeatureEntity.class;
    }

    @Override
    public Collection<FeatureEntity> get(DbQuery query) {
        Criteria c = session.createCriteria(getEntityClass(), getDefaultAlias())
                .setResultTransformer(RootEntityResultTransformer.INSTANCE);
        IoParameters parameters = query.getParameters();
        if (parameters.getFeatures() != null && !parameters.getFeatures().isEmpty()) {
            c.add(query.getParameters().isMatchDomainIds() ? createDomainIdFilter(parameters.getFeatures())
                    : createIdFilter(parameters.getFeatures()));
        }
        query.addSpatialFilter(c);
        return c.list();
    }

    private Criterion createDomainIdFilter(Set<String> filterValues) {
        return filterValues.stream().map(filter -> Restrictions.ilike(FeatureEntity.PROPERTY_DOMAIN_ID, filter))
                .collect(Restrictions::disjunction, Disjunction::add,
                         (a, b) -> b.conditions().forEach(a::add));
    }

    private Criterion createIdFilter(Set<String> filterValues) {
        return Restrictions.in(FeatureEntity.PROPERTY_ID, QueryUtils.parseToIds(filterValues));
    }

}
