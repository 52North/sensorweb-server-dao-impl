/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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
import org.n52.io.response.OfferingOutput;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.OfferingDao;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.series.spi.search.SearchResult;

public class OfferingRepository extends HierarchicalParameterRepository<OfferingEntity, OfferingOutput> {

    @Override
    protected OfferingOutput prepareEmptyParameterOutput(OfferingEntity entity) {
        return new OfferingOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new FeatureSearchResult(id, label, baseUrl);
    }

    @Override
    protected String createHref(String hrefBase) {
        return urlHelper.getOfferingsHrefBaseUrl(hrefBase);
    }

    @Override
    protected OfferingDao createDao(Session session) {
        return new OfferingDao(session);
    }

    @Override
    protected SearchableDao<OfferingEntity> createSearchableDao(Session session) {
        return new OfferingDao(session);
    }

    @Override
    protected OfferingOutput createExpanded(OfferingEntity entity, DbQuery parameters, Session session) {
        OfferingOutput result = createCondensed(entity, parameters, session);
        if (parameters.getHrefBase() != null) {
            result.setService(getCondensedExtendedService(getServiceEntity(entity), parameters));
        } else {
            result.setService(getCondensedService(getServiceEntity(entity), parameters));
        }
        return result;
    }
}
