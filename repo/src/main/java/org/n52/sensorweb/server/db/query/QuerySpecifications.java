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
package org.n52.sensorweb.server.db.query;

import java.util.Collection;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.io.request.IoParameters;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DefaultDbQueryFactory;
import org.n52.sensorweb.server.db.old.dao.QueryUtils;
import org.n52.series.db.beans.DescribableEntity;

public abstract class QuerySpecifications {

    protected final DbQuery dbQuery;

    protected final EntityManager entityManager;

    protected QuerySpecifications(final DbQuery dbQuery, EntityManager entityManager) {
        this.dbQuery =
                dbQuery == null ? new DefaultDbQueryFactory().createFrom(IoParameters.createDefaults()) : dbQuery;
        this.entityManager = entityManager;
    }

    protected Date getTimespanStart() {
        DateTime start = getTimespan().getStart();
        return start.toDate();
    }

    protected Date getTimespanEnd() {
        DateTime end = getTimespan().getEnd();
        return end.toDate();
    }

    private Interval getTimespan() {
        return dbQuery.getTimespan();
    }

    protected Predicate getIdPredicate(Join<?, ?> join, final Collection<String> ids) {
        return dbQuery.isMatchDomainIds() ? join.get(DescribableEntity.PROPERTY_IDENTIFIER).in(ids)
                : join.get(DescribableEntity.PROPERTY_ID).in(QueryUtils.parseToIds(ids));
    }

}
