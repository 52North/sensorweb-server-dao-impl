/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.QueryUtils;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.SamplingGeometryEntity;

public class SamplingGeometryDao {

    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String PROPERTY_DATASET = "dataset";

    private final Session session;

    public SamplingGeometryDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<GeometryEntity> getGeometriesOrderedByTimestamp(DbQuery query) {
        Criteria criteria = session.createCriteria(SamplingGeometryEntity.class);
        String path = QueryUtils.createAssociation(DatasetEntity.PROPERTY_FEATURE, FeatureEntity.PROPERTY_ID);
        criteria.createCriteria(PROPERTY_DATASET)
                .add(Restrictions.in(path, getFeatureIds(query)));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        criteria.addOrder(Order.asc(COLUMN_TIMESTAMP));

        query.addSpatialFilter(criteria);
        return toGeometryEntities(criteria.list());
    }

    protected List<Long> getFeatureIds(DbQuery query) {
        return query.getParameters()
                    .getFeatures()
                    .stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
    }

    private List<GeometryEntity> toGeometryEntities(List<SamplingGeometryEntity> entities) {
        return entities.stream()
                       .map(e -> e.getGeometryEntity())
                       .collect(Collectors.toList());
    }

}
