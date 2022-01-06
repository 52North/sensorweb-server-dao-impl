/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.i18n.I18nFeatureEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class FeatureDao extends HierarchicalDao<FeatureEntity, I18nFeatureEntity> {

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
        return getCriteria(query).list();
    }

    @Override
    public Set<Long> getChildrenIds(DbQuery query) {
        Set<String> features = query.getParameters().getFeatures();
        if (features != null && !features.isEmpty()) {
            return getChildrenIds(query, features, query.getLevel());
        }
        return Collections.emptySet();
    }

    @Override
    protected Criteria getCriteria(DbQuery query) throws DataAccessException {
        Criteria c = getDefaultCriteria();
        IoParameters parameters = query.getParameters();
        if (parameters.getFeatures() != null && !parameters.getFeatures().isEmpty()) {
            c.add(query.getParameters().isMatchDomainIds() ? createDomainIdFilter(parameters.getFeatures())
                    : createIdFilter(parameters.getFeatures()));
        }
        query.addSpatialFilter(c);
        return c;
    }

    @Override
    protected Set<String> getParameter(DbQuery query) {
        return query.getParameters().getFeatures();
    }

    @Override
    protected IoParameters replaceParameter(DbQuery query, Collection<String> entites) {
        return query.getParameters().replaceWith(Parameters.FEATURES, entites);
    }

    @Override
    protected Criteria addFetchModes(Criteria criteria, DbQuery q, boolean instance) {
        super.addFetchModes(criteria, q, instance);
        if (q.isExpanded() || instance) {
            if (q.getParameters().isSelected(AbstractFeatureEntity.PROPERTY_PARENTS)) {
                criteria.setFetchMode(AbstractFeatureEntity.PROPERTY_PARENTS, FetchMode.JOIN);
            }
            if (q.getParameters().isSelected(AbstractFeatureEntity.PROPERTY_CHILDREN)) {
                criteria.setFetchMode(AbstractFeatureEntity.PROPERTY_CHILDREN, FetchMode.JOIN);
            }
            if (q.getParameters().isSelected(AbstractFeatureEntity.PROPERTY_DATASETS)) {
                criteria.setFetchMode(AbstractFeatureEntity.PROPERTY_DATASETS, FetchMode.JOIN);
                criteria.setFetchMode(
                        getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_PHENOMENON),
                        FetchMode.JOIN);
                criteria.setFetchMode(
                        getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_PROCEDURE),
                        FetchMode.JOIN);
                criteria.setFetchMode(
                        getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_OFFERING),
                        FetchMode.JOIN);
                criteria.setFetchMode(
                        getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_PLATFORM),
                        FetchMode.JOIN);
                criteria.setFetchMode(
                        getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_CATEGORY),
                        FetchMode.JOIN);
                criteria.setFetchMode(
                        getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS, DatasetEntity.PROPERTY_UNIT),
                        FetchMode.JOIN);
                if (!q.isDefaultLocal()) {
                    criteria.setFetchMode(getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS,
                            DatasetEntity.PROPERTY_PHENOMENON, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                    criteria.setFetchMode(getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS,
                            DatasetEntity.PROPERTY_PROCEDURE, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                    criteria.setFetchMode(getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS,
                            DatasetEntity.PROPERTY_OFFERING, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                    criteria.setFetchMode(getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS,
                            DatasetEntity.PROPERTY_PLATFORM, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                    criteria.setFetchMode(getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS,
                            DatasetEntity.PROPERTY_CATEGORY, TRANSLATIONS_ALIAS), FetchMode.JOIN);
                    criteria.setFetchMode(getFetchPath(AbstractFeatureEntity.PROPERTY_DATASETS,
                            DatasetEntity.PROPERTY_UNIT, TRANSLATIONS_ALIAS), FetchMode.JOIN);

                }
            }
        }
        return criteria;
    }
}
