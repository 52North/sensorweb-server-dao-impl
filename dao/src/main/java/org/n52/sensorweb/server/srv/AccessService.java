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
package org.n52.sensorweb.server.srv;

import java.util.ArrayList;
import java.util.List;

import org.n52.io.request.IoParameters;
import org.n52.io.response.OutputCollection;
import org.n52.io.response.ParameterOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.old.dao.DefaultDbQueryFactory;
import org.n52.series.db.old.DataAccessException;
import org.n52.series.spi.srv.ParameterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public abstract class AccessService<T extends ParameterOutput> extends ParameterService<T> {

    protected final OutputAssembler<T> repository;

    protected final DbQueryFactory dbQueryFactory;

    public AccessService(OutputAssembler<T> repository, DbQueryFactory queryFactory) {
        this.repository = repository;
        this.dbQueryFactory = queryFactory == null
                ? new DefaultDbQueryFactory()
                : queryFactory;
    }

    @Override
    public OutputCollection<T> getExpandedParameters(IoParameters query) {
        try {
            DbQuery dbQuery = dbQueryFactory.createFrom(query);
            List<T> results = repository.getAllExpanded(dbQuery);
            return new OutputCollection<>(results);
        } catch (Exception e) {
            throw new DataAccessException("Could not get expanded data.", e);
        }
    }

    @Override
    public OutputCollection<T> getCondensedParameters(IoParameters query) {
        try {
            DbQuery dbQuery = dbQueryFactory.createFrom(query);
            List<T> results = repository.getAllCondensed(dbQuery);
            return new OutputCollection<>(results);
        } catch (Exception e) {
            throw new DataAccessException("Could not get condensed data.", e);
        }
    }

    @Override
    public OutputCollection<T> getParameters(String[] ids, IoParameters query) {
        try {
            DbQuery dbQuery = dbQueryFactory.createFrom(query);
            List<T> results = new ArrayList<>();
            for (String id : ids) {
                results.add(repository.getInstance(id, dbQuery));
            }
            return new OutputCollection<>(results);
        } catch (Exception e) {
            throw new DataAccessException("Could not get multiple instance data.", e);
        }
    }

    @Override
    public T getParameter(String id, IoParameters query) {
        try {
            DbQuery dbQuery = dbQueryFactory.createFrom(query);
            return repository.getInstance(id, dbQuery);
        } catch (Exception e) {
            throw new DataAccessException("Could not get instance data.", e);
        }
    }

    @Override
    public boolean exists(String id, IoParameters parameters) {
        return repository.exists(id, dbQueryFactory.createFrom(parameters));
    }

}
