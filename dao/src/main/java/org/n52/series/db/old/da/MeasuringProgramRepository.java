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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.MeasuringProgramDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.spi.search.MeasuringProgramSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasuringProgramRepository extends ParameterAssembler<MeasuringProgramEntity, MeasuringProgramOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasuringProgramRepository.class);

    public MeasuringProgramRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected MeasuringProgramOutput prepareEmptyParameterOutput() {
        return new MeasuringProgramOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new MeasuringProgramSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected MeasuringProgramDao createDao(Session session) {
        return new MeasuringProgramDao(session);
    }

    @Override
    protected SearchableDao<MeasuringProgramEntity> createSearchableDao(Session session) {
        return new MeasuringProgramDao(session);
    }

    @Override
    protected List<MeasuringProgramOutput> createCondensed(Collection<MeasuringProgramEntity> measuringPrograms,
            DbQuery query, Session session) {
        long start = System.currentTimeMillis();
        List<MeasuringProgramOutput> results = new LinkedList<>();
        for (MeasuringProgramEntity measuringProgram : measuringPrograms) {
            results.add(createCondensed(measuringProgram, query, session));
        }
        LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
        return results;
    }

    @Override
    protected MeasuringProgramOutput createCondensed(MeasuringProgramEntity measuringProgram, DbQuery query,
            Session session) {
        return getMapperFactory().getMeasuringProgramMapper(query).createCondensed(measuringProgram);
    }

    @Override
    protected List<MeasuringProgramOutput> createExpanded(Collection<MeasuringProgramEntity> measuringPrograms,
            DbQuery query, Session session) {
        long start = System.currentTimeMillis();
        List<MeasuringProgramOutput> results = new LinkedList<>();
        for (MeasuringProgramEntity measuringProgram : measuringPrograms) {
            results.add(createExpanded(measuringProgram, query, session));
        }
        LOGGER.debug("Processing all expanded instances takes {} ms", System.currentTimeMillis() - start);
        return results;
    }

    @Override
    protected MeasuringProgramOutput createExpanded(MeasuringProgramEntity measuringProgram, DbQuery query,
            Session session) {
        return getMapperFactory().getMeasuringProgramMapper(query).createExpanded(measuringProgram);
    }

    @Override
    protected ParameterOutputSearchResultMapper<MeasuringProgramEntity, MeasuringProgramOutput> getOutputMapper(
            DbQuery query) {
        return getMapperFactory().getMeasuringProgramMapper(query);
    }

}
