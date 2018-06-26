/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
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

import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.FeatureDao;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.series.spi.search.SearchResult;

public class FeatureRepository extends HierarchicalParameterRepository<FeatureEntity, FeatureOutput> {

    @Override
    protected FeatureOutput prepareEmptyParameterOutput() {
        return new FeatureOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new FeatureSearchResult(id, label, baseUrl);
    }

    @Override
    protected String createHref(String hrefBase) {
        return urlHelper.getFeaturesHrefBaseUrl(hrefBase);
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
    protected FeatureOutput createExpanded(FeatureEntity entity, DbQuery query, Session session) {
        FeatureOutput result = createCondensed(entity, query, session);
        ServiceOutput service = (query.getHrefBase() != null)
                ? getCondensedExtendedService(getServiceEntity(entity), query.withoutFieldsFilter())
                : getCondensedService(getServiceEntity(entity), query.withoutFieldsFilter());
        Set<Map<String, Object>> parameters = entity.getMappedParameters(query.getLocale());
        result.setValue(FeatureOutput.SERVICE, service, query.getParameters(), result::setService);
        result.setValue(FeatureOutput.PARAMETERS, parameters, query.getParameters(), result::setParameters);
        return result;
    }

}
