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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.request.Parameters;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.AbstractDao;
import org.n52.series.db.old.dao.PlatformDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.series.spi.search.SearchResult;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
// @Component
public class PlatformAssembler extends ParameterAssembler<PlatformEntity, PlatformOutput> {

    private final DatasetAssembler<AbstractValue<?>> datasetAssembler;

    @SuppressFBWarnings({ "EI_EXPOSE_REP2" })
    public PlatformAssembler(DatasetAssembler<AbstractValue<?>> datasetAssembler, HibernateSessionStore sessionStore,
            DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.datasetAssembler = datasetAssembler;
    }

    @Override
    protected PlatformOutput prepareEmptyParameterOutput() {
        return new PlatformOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new FeatureSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected AbstractDao<PlatformEntity> createDao(Session session) {
        return createPlatformDao(session);
    }

    private PlatformDao createPlatformDao(Session session) {
        return new PlatformDao(session);
    }

    @Override
    protected SearchableDao<PlatformEntity> createSearchableDao(Session session) {
        return new PlatformDao(session);
    }

    @Override
    protected PlatformOutput createCondensed(PlatformEntity entity, DbQuery query, Session session) {
        return getCondensedPlatform(entity, query);
    }

    @Override
    protected PlatformOutput createExpanded(PlatformEntity entity, DbQuery query, Session session) {
        PlatformOutput result = createCondensed(entity, query, session);
        ServiceOutput service = getCondensedService(getServiceEntity(entity), query.withoutFieldsFilter());
        result.setValue(PlatformOutput.SERVICE, service, query.getParameters(), result::setService);

        DbQuery platformQuery = getDbQuery(query.getParameters().extendWith(Parameters.PLATFORMS, result.getId())
                .removeAllOf(Parameters.FILTER_FIELDS));
        DbQuery datasetQuery = getDbQuery(platformQuery.getParameters().removeAllOf(Parameters.ODATA_FILTER)
                .removeAllOf(Parameters.FILTER_FIELDS));

        List<DatasetOutput<AbstractValue<?>>> datasets = datasetAssembler.getAllCondensed(datasetQuery);
        // Set<Map<String, Object>> parameters =
        // entity.getMappedParameters(query.getLocale());

        result.setValue(PlatformOutput.DATASETS, datasets, query.getParameters(), result::setDatasets);

        return result;
    }

    protected List<PlatformOutput> createCondensedHierarchyMembers(Set<PlatformEntity> members, DbQuery parameters,
            Session session) {
        return members == null ? Collections.emptyList()
                : members.stream().map(e -> createCondensed(e, parameters, session)).collect(Collectors.toList());
    }

    public PlatformOutput createCondensedPlatform(PlatformEntity platform, DbQuery query, Session session) {
        return getCondensedPlatform(platform, query);
    }

    @Override
    protected ParameterOutputSearchResultMapper<PlatformEntity, PlatformOutput> getOutputMapper(DbQuery query) {
        return getMapperFactory().getPlatformMapper(query);
    }

}
