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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;

import org.n52.io.request.IoParameters;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.springframework.data.jpa.domain.Specification;

public class ParameterQuerySpecifications extends QuerySpecifications {

    protected ParameterQuerySpecifications(final DbQuery dbQuery, EntityManager entityManager) {
        super(dbQuery, entityManager);
    }

    public static ParameterQuerySpecifications of(final DbQuery dbQuery, EntityManager entityManager) {
        return new ParameterQuerySpecifications(dbQuery, entityManager);
    }


    public <T extends DescribableEntity> Specification<T> matchServices(ServiceEntity service) {
        return matchServices(service.getId().toString());
    }

    /**
     * Matches datasets having service with given ids.
     *
     * @return a boolean expression
     * @see #matchServices(Collection)
     */
    public <T extends DescribableEntity> Specification<T> matchServices() {
        final IoParameters parameters = dbQuery.getParameters();
        return matchServices(parameters.getServices());
    }

    /**
     * Matches datasets having service with given ids.
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression
     * @see #matchServices(Collection)
     */
    public <T extends DescribableEntity> Specification<T> matchServices(final String... ids) {
        return ids != null ? matchServices(Arrays.asList(ids)) : matchServices(Collections.emptyList());
    }

    /**
     * Matches datasets having service with given ids. For example:
     *
     * <pre>
     *  where service.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where service.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *            the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public <T extends DescribableEntity> Specification<T> matchServices(final Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return (root, query, builder) -> {
            final Join<DatasetEntity, ServiceEntity> join = root.join(DatasetEntity.PROPERTY_SERVICE, JoinType.INNER);
            return getIdPredicate(join, ids);
        };
    }

}
