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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ObservationConstellationEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.i18n.I18nOfferingEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OfferingDao extends ParameterDao<OfferingEntity, I18nOfferingEntity> {

    public OfferingDao(Session session) {
        super(session);
    }

    @Override
    protected String getDatasetProperty() {
        return QueryUtils.createAssociation(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION,
                                            ObservationConstellationEntity.OFFERING);
    }

    @Override
    protected DetachedCriteria projectOnDatasetParameterId(DetachedCriteria subquery) {
        return subquery.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION)
                       .createCriteria(ObservationConstellationEntity.OFFERING)
                       .setProjection(Projections.property(DescribableEntity.PROPERTY_ID));
    }

    @Override
    protected Class<OfferingEntity> getEntityClass() {
        return OfferingEntity.class;
    }

    @Override
    protected Class<I18nOfferingEntity> getI18NEntityClass() {
        return I18nOfferingEntity.class;
    }

    public Collection<OfferingEntity> get() throws DataAccessException {
        return getAllInstances(new DbQuery(IoParameters.createDefaults()));
    }

    public Collection<OfferingEntity> get(Collection<String> identifiers) throws DataAccessException {
        Map<String, String> map = new HashMap<>();
        if (identifiers != null && !identifiers.isEmpty()) {
            map.put(IoParameters.OFFERINGS, toString(identifiers));
        }
        map.put(IoParameters.MATCH_DOMAIN_IDS, Boolean.toString(true));
        return getAllInstances(new DbQuery(IoParameters.createFromSingleValueMap(map)));
    }

    private String toString(Collection<String> identifiers) {
        StringBuilder sb = new StringBuilder();
        for (String string : identifiers) {
            sb.append(string);
            sb.append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

}
