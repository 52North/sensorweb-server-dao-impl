/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.old.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.response.AbstractOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.HierarchicalEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.srv.OutputAssembler;

public abstract class HierarchicalParameterAssembler<E extends HierarchicalEntity<E>, O extends AbstractOutput>
        extends ParameterAssembler<E, O>
        implements OutputAssembler<O> {

    public HierarchicalParameterAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected List<O> createExpanded(Collection<E> entities, DbQuery query, Session session) {
        Set<O> results = new HashSet<>();
        if (entities != null) {
            for (E entity : entities) {
                O result = createExpanded(entity, query, session);
                results.add(result);
            }
        }
        return new ArrayList<>(results);
    }

    @Override
    protected List<O> createCondensed(Collection<E> entities, DbQuery query, Session session) {
        return entities == null
                ? Collections.emptyList()
                : entities.stream()
                          .map(entity -> createCondensed(entity, query, session))
                          .collect(Collectors.toList());
    }

}
