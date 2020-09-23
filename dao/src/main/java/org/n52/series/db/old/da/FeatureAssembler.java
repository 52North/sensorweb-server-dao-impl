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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.AbstractOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.StationOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DatasetDao;
import org.n52.series.db.old.dao.FeatureDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.series.spi.search.SearchResult;

//@Component
public class FeatureAssembler extends HierarchicalParameterAssembler<FeatureEntity, FeatureOutput> {

    public FeatureAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected FeatureOutput prepareEmptyParameterOutput() {
        return new FeatureOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new FeatureSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected FeatureDao createDao(Session session) {
        return new FeatureDao(session);
    }

    @Override
    protected SearchableDao<FeatureEntity> createSearchableDao(Session session) {
        return new FeatureDao(session);
    }

    @Override
    protected FeatureOutput createCondensed(FeatureEntity entity, DbQuery query, Session session) {
        return getCondensedFeature(entity, query);
    }

    @Override
    protected FeatureOutput createExpanded(FeatureEntity entity, DbQuery query, Session session) {
        return createExpanded(entity, query, false, false, query.getLevel(), session);
    }

    protected FeatureOutput createExpanded(FeatureEntity entity, DbQuery query, boolean isParent, boolean isChild,
            Integer level, Session session) {
        FeatureOutput result = createCondensed(entity, query, session);
        ServiceOutput service = (query.getHrefBase() != null)
                ? getCondensedExtendedService(getServiceEntity(entity), query.withoutFieldsFilter())
                : getCondensedService(getServiceEntity(entity), query.withoutFieldsFilter());
        result.setValue(AbstractOutput.SERVICE, service, query.getParameters(), result::setService);
        if (!isParent && !isChild) {
            Class<DatasetEntity> clazz = DatasetEntity.class;
            DatasetDao<DatasetEntity> seriesDao = new DatasetDao<>(session, clazz);
            List<DatasetEntity> series = seriesDao.getInstancesWith(entity, query);
            Map<String, DatasetParameters> timeseriesList = createTimeseriesList(series, query);
            result.setValue(StationOutput.PROPERTIES, timeseriesList, query.getParameters(), result::setDatasets);
        }
        if (!isParent && !isChild && entity.hasParents()) {
            List<FeatureOutput> parents = getMemberList(entity.getParents(), query, level, true, false, session);
            result.setValue(HierarchicalParameterOutput.PARENTS, parents, query.getParameters(), result::setParents);
        }
        if (level != null && level > 0) {
            if (((!isParent && !isChild) || (!isParent && isChild)) && entity.hasChildren()) {
                List<FeatureOutput> children =
                        getMemberList(entity.getChildren(), query, level - 1, false, true, session);
                result.setValue(HierarchicalParameterOutput.CHILDREN, children, query.getParameters(),
                        result::setChildren);
            }

        }
        return result;
    }

    private List<FeatureOutput> getMemberList(Set<FeatureEntity> entities, DbQuery query, Integer level,
            boolean isNotParent, boolean isNotChild, Session session) {
        List<FeatureOutput> list = new LinkedList<>();
        for (FeatureEntity e : entities) {
            list.add(createExpanded(e, query, isNotParent, isNotChild, level, session));
        }
        return list;
    }

    private boolean hasFilterParameter(DbQuery query) {
        IoParameters parameters = query.getParameters();
        return parameters.containsParameter(Parameters.DATASETS) || parameters.containsParameter(Parameters.CATEGORIES)
                || parameters.containsParameter(Parameters.OFFERINGS)
                || parameters.containsParameter(Parameters.PHENOMENA)
                || parameters.containsParameter(Parameters.PLATFORMS)
                || parameters.containsParameter(Parameters.PROCEDURES);
    }

}
