/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.joda.time.DateTime;
import org.n52.io.extension.ExtensionAssembler;
import org.n52.io.request.IoParameters;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResultTimeAssembler extends ExtensionAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultTimeAssembler.class);

    private EntityManager entityManager;

    public ResultTimeAssembler(EntityManager entityManager, DatasetRepository datasetRepository,
            DbQueryFactory dbQueryFactory) {
        super(datasetRepository, dbQueryFactory);
        this.entityManager = entityManager;
    }

    protected Set<String> getExtras(String datasetId, IoParameters parameters) {
        try {
            String alias = "datasets";
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Date> query = cb.createQuery(Date.class);
            Root<DataEntity> root = query.distinct(true).from(DataEntity.class);
            Join<DataEntity, DatasetEntity> join = root.join(DataEntity.PROPERTY_DATASET, JoinType.INNER);
            query.select(root.get(DataEntity.PROPERTY_RESULT_TIME));

            query.distinct(true)
                    .where(cb.notEqual(root.get(DataEntity.PROPERTY_RESULT_TIME),
                            root.get(DataEntity.PROPERTY_SAMPLING_TIME_END)),
                            cb.equal(join.get(DatasetEntity.PROPERTY_ID), Long.parseLong(datasetId)));
            List<Date> resultTimes = entityManager.createQuery(query).getResultList();
            // DataDao< ? > dao = new DataDao<>(session);
            // String datasetMember = QueryUtils.createAssociation(alias,
            // DatasetEntity.PROPERTY_ID);
            // List<Date> resultTimes =
            // dao.getDefaultCriteria(getDbQuery(parameters))
            // .add(Restrictions.neProperty(DataEntity.PROPERTY_RESULT_TIME,
            // DataEntity.PROPERTY_SAMPLING_TIME_END))
            //// .setProjection(Projections.property(DataEntity.PROPERTY_RESULT_TIME))
            //// .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            //// .createCriteria(DataEntity.PROPERTY_DATASET, alias)
            // .add(Restrictions.eq(datasetMember, Long.parseLong(datasetId)))
            // .list();
            return resultTimes.stream().map(i -> new DateTime(i).toString()).collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            LOGGER.debug("Could not convert id '{}' to long.", datasetId, e);
        }
        return Collections.emptySet();
    }

}
