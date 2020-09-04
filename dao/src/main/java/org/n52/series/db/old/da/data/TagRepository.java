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
package org.n52.series.db.da;

import org.hibernate.Session;
import org.n52.io.response.AbstractOutput;
import org.n52.io.response.TagOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.beans.TagEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.TagDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.TagSearchResult;
import org.n52.series.spi.search.SearchResult;

public class TagRepository extends ParameterRepository<TagEntity, TagOutput> {

    @Override
    protected TagOutput prepareEmptyParameterOutput() {
        return new TagOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new TagSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected AbstractDao<TagEntity> createDao(Session session) {
        return new TagDao(session);
    }

    @Override
    protected SearchableDao<TagEntity> createSearchableDao(Session session) {
        return new TagDao(session);
    }

    @Override
    protected TagOutput createExpanded(TagEntity entity, DbQuery query, Session session) {
        TagOutput result = createCondensed(entity, query, session);
        ServiceOutput service = (query.getHrefBase() != null)
                ? getCondensedExtendedService(getServiceEntity(entity), query.withoutFieldsFilter())
                : getCondensedService(getServiceEntity(entity), query.withoutFieldsFilter());
        result.setValue(AbstractOutput.SERVICE, service, query.getParameters(), result::setService);
        result.setValue(TagOutput.DATASETS, getCondensedDataset(entity, query, session), query.getParameters(),
                result::setDatasets);
        return result;
    }

}
