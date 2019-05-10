/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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

import org.hibernate.Session;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.PhenomenonDao;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.CategorySearchResult;
import org.n52.series.spi.search.SearchResult;

public class PhenomenonRepository extends HierarchicalParameterRepository<PhenomenonEntity, PhenomenonOutput> {

    @Override
    protected PhenomenonOutput prepareEmptyParameterOutput(PhenomenonEntity entity) {
        return new PhenomenonOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new CategorySearchResult(id, label, baseUrl);
    }

    @Override
    protected String createHref(String hrefBase) {
        return urlHelper.getPhenomenaHrefBaseUrl(hrefBase);
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
    protected PhenomenonOutput createExpanded(PhenomenonEntity entity, DbQuery query, Session session) {
        PhenomenonOutput result = createCondensed(entity, query, session);
        ServiceOutput service = (query.getHrefBase() != null)
                ? getCondensedExtendedService(getServiceEntity(entity), query)
                : getCondensedService(getServiceEntity(entity), query);
        result.setValue(PhenomenonOutput.SERVICE, service, query.getParameters(), result::setService);
        return result;
    }

}
