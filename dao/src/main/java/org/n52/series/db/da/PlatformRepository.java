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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.PlatformDao;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.PlatformSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
public class PlatformRepository extends ParameterRepository<PlatformEntity, PlatformOutput> {

    @Autowired
    private DatasetRepository<AbstractValue<?>> datasetRepository;

    @Autowired
    private DataRepositoryTypeFactory factory;

    @Override
    protected PlatformOutput prepareEmptyParameterOutput() {
        return new PlatformOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new PlatformSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected PlatformDao createDao(Session session) {
        return new PlatformDao(session);
    }

    @Override
    protected SearchableDao<PlatformEntity> createSearchableDao(Session session) {
        return new PlatformDao(session);
    }

    @Override
    protected PlatformOutput createCondensed(PlatformEntity entity, DbQuery query, Session session) {
        return getMapperFactory().getPlatformMapper().createCondensed(entity, query);
    }

    @Override
    protected PlatformOutput createExpanded(PlatformEntity entity, DbQuery query, Session session) {
        return getMapperFactory().getPlatformMapper().createExpanded(entity, query, session);
    }

    protected List<PlatformOutput> createCondensedHierarchyMembers(Set<PlatformEntity> members, DbQuery parameters,
            Session session) {
        return members == null ? Collections.emptyList()
                : members.stream().map(e -> createCondensed(e, parameters, session)).collect(Collectors.toList());
    }
}
