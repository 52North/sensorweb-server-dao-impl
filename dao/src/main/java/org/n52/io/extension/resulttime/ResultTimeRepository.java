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
package org.n52.io.extension.resulttime;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.da.SessionAwareRepository;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResultTimeRepository extends SessionAwareRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultTimeRepository.class);

    @SuppressWarnings("unchecked")
    Set<String> getExtras(String datasetId, IoParameters parameters) {
        Session session = getSession();
        try {
            String alias = "datasets";
            DataDao< ? > dao = new DataDao<>(session);
            String datasetMember = QueryUtils.createAssociation(alias, DatasetEntity.PROPERTY_ID);
            List<Date> resultTimes = dao.getDefaultCriteria(getDbQuery(parameters))
                                        .add(Restrictions.neProperty(DataEntity.PROPERTY_RESULT_TIME,
                                                                     DataEntity.PROPERTY_SAMPLING_TIME_END))
                                        .setProjection(Projections.property(DataEntity.PROPERTY_RESULT_TIME))
                                        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                                        .createCriteria(DataEntity.PROPERTY_DATASET, alias)
                                        .add(Restrictions.eq(datasetMember, Long.parseLong(datasetId)))
                                        .list();
            return resultTimes.stream()
                              .map(i -> new DateTime(i).toString())
                              .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            LOGGER.debug("Could not convert id '{}' to long.", datasetId, e);
        } finally {
            returnSession(session);
        }
        return Collections.emptySet();
    }

}
