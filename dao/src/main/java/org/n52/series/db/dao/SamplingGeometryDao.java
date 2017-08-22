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
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.SamplingGeometryEntity;

public class SamplingGeometryDao {
    
    private static final String COLUMN_PKID = "pkid";

    private static final String COLUMN_SERIES_PKID = "seriesPkid";

    private static final String COLUMN_TIMESTAMP = "timestamp";

    private final Session session;

    public SamplingGeometryDao(Session session) {
        this.session = session;
    }

    @SuppressWarnings("unchecked")
    public List<GeometryEntity> getGeometriesOrderedByTimestamp(DbQuery query) {
        Criteria criteria = session.createCriteria(SamplingGeometryEntity.class);
        criteria.add(Restrictions.in(COLUMN_SERIES_PKID, getDatasetIds(query)));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        criteria.addOrder(Order.asc(COLUMN_TIMESTAMP));
        query.addSpatialFilterTo(criteria);
        return toGeometryEntities(criteria.list());
    }
    
    @SuppressWarnings("unchecked")
    private List<Long> getDatasetIds(DbQuery query) {
        Criteria criteria = session.createCriteria(DatasetEntity.class);
        criteria.setProjection(Projections.property(DescribableEntity.PROPERTY_PKID));
        query.addDetachedFilters("", criteria);
        return criteria.list();
    }


    private List<GeometryEntity> toGeometryEntities(List<SamplingGeometryEntity> entities) {
        return entities.stream()
                       .map(e -> e.getGeometryEntity())
                       .collect(Collectors.toList());
    }

}
