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
package org.n52.series.db.old.da;

import org.hibernate.Session;
import org.n52.io.response.PhenomenonOutput;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.AbstractDao;
import org.n52.series.db.old.dao.PhenomenonDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.spi.search.PhenomenonSearchResult;
import org.n52.series.spi.search.SearchResult;

//@Component
public class PhenomenonAssembler extends HierarchicalParameterAssembler<PhenomenonEntity, PhenomenonOutput> {

    public PhenomenonAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected PhenomenonOutput prepareEmptyParameterOutput() {
        return new PhenomenonOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new PhenomenonSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected AbstractDao<PhenomenonEntity> createDao(Session session) {
        return new PhenomenonDao(session);
    }

    @Override
    protected SearchableDao<PhenomenonEntity> createSearchableDao(Session session) {
        return new PhenomenonDao(session);
    }

    @Override
    protected PhenomenonOutput createCondensed(PhenomenonEntity entity, DbQuery query, Session session) {
        return getMapperFactory().getPhenomenonMapper(query).createCondensed(entity);
    }

    @Override
    protected PhenomenonOutput createExpanded(PhenomenonEntity entity, DbQuery query, Session session) {
        return getOutputMapper(query).createExpanded(entity);
    }

    @Override
    protected ParameterOutputSearchResultMapper<PhenomenonEntity, PhenomenonOutput> getOutputMapper(DbQuery query) {
        return getMapperFactory().getPhenomenonMapper(query);
    }

}
