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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.response.ProcedureOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.ProcedureDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.spi.search.ProcedureSearchResult;
import org.n52.series.spi.search.SearchResult;

//@Component
public class ProcedureAssembler extends HierarchicalParameterAssembler<ProcedureEntity, ProcedureOutput> {

    public ProcedureAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected ProcedureOutput prepareEmptyParameterOutput() {
        return new ProcedureOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new ProcedureSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected ProcedureDao createDao(Session session) {
        return new ProcedureDao(session);
    }

    @Override
    protected SearchableDao<ProcedureEntity> createSearchableDao(Session session) {
        return new ProcedureDao(session);
    }

    @Override
    protected ProcedureOutput createExpanded(ProcedureEntity entity, DbQuery query, Session session) {
        return getOutputMapper(query).createExpanded(entity);
    }

    protected List<ProcedureOutput> createCondensedHierarchyMembers(Set<ProcedureEntity> members, DbQuery parameters,
            Session session) {
        return members == null ? Collections.emptyList()
                : members.stream().map(e -> createCondensed(e, parameters, session)).collect(Collectors.toList());
    }

    @Override
    protected ParameterOutputSearchResultMapper<ProcedureEntity, ProcedureOutput> getOutputMapper(DbQuery query) {
        return getMapperFactory().getProcedureMapper(query);
    }

}
