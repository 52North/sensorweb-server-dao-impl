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
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.response.AbstractOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.HierarchicalEntity;
import org.n52.series.db.dao.DbQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HierarchicalParameterRepository<E extends HierarchicalEntity<E>, O extends AbstractOutput>
        extends ParameterRepository<E, O> implements OutputAssembler<O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalParameterRepository.class);

    @Override
    protected List<O> createExpanded(Collection<E> entities, DbQuery query, Session session)
            throws DataAccessException {
        if (entities != null) {
            long start = System.currentTimeMillis();
            LOGGER.debug("Expandend entities raw: " + entities.size());
            List<O> result = entities.parallelStream().map(e -> createExpanded(e, query, session))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            LOGGER.debug("Expandend entities processed: " + result.size());
            LOGGER.debug("Processing all expanded instances takes {} ms", System.currentTimeMillis() - start);
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    protected List<O> createCondensed(Collection<E> entities, DbQuery query, Session session) {
        long start = System.currentTimeMillis();
        if (entities != null) {
            LOGGER.debug("Condensed entities raw: " + entities.size());
            List<O> result = entities.parallelStream().map(entity -> createCondensed(entity, query, session))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            LOGGER.debug("Condensed entities processed: " + result.size());
            LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
            return result;
        }
        return new ArrayList<>();
    }

}
