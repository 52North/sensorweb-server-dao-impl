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
package org.n52.sensorweb.server.db.query;

import java.util.Collection;

import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.QueryUtils;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.springframework.data.jpa.domain.Specification;

public final class ServiceQuerySpecifications extends ParameterQuerySpecifications {

    private ServiceQuerySpecifications(DbQuery dbQuery) {
        super(dbQuery, null);
    }

    public static ServiceQuerySpecifications of(DbQuery dbQuery) {
        return new ServiceQuerySpecifications(dbQuery);
    }

    /**
     * Matches service included in a result of a given filter, i.e.
     *
     * <pre>
     *   where id in (select fk_service_id from dataset where &lt;filter&gt;)
     * </pre>
     *
     * @param filter
     *            the query
     * @return a boolean expression
     */
    public Specification<ServiceEntity> selectFrom(final Specification<DatasetEntity> filter) {
        return (root, query, builder) -> {
            Subquery<Long> sq = query.subquery(Long.class);
            Root<DatasetEntity> dataset = sq.from(DatasetEntity.class);
            sq.select(dataset.get(DatasetEntity.PROPERTY_SERVICE).get(DescribableEntity.PROPERTY_ID))
                    .where(filter.toPredicate(dataset, query, builder));
            return builder.in(root.get(DescribableEntity.PROPERTY_ID)).value(sq);
        };
    }

    @Override
    public <T extends DescribableEntity> Specification<T> matchServices(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            return dbQuery.isMatchDomainIds() ? root.get(DescribableEntity.PROPERTY_IDENTIFIER).in(ids)
                    : root.get(DescribableEntity.PROPERTY_ID).in(QueryUtils.parseToIds(ids));
        };
    }

}
