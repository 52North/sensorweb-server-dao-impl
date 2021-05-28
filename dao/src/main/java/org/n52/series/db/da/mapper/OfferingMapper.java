/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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
package org.n52.series.db.da.mapper;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.OfferingOutput;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.dao.DbQuery;

public class OfferingMapper extends AbstractOuputMapper<OfferingOutput, OfferingEntity> {

    public OfferingMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params, false);
    }

    public OfferingMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        super(mapperFactory, params, subMapper);
    }

    @Override
    public OfferingOutput createCondensed(OfferingEntity entity, DbQuery query) {
        return createCondensed(new OfferingOutput(), entity, query);
    }

    @Override
    public OfferingOutput createExpanded(OfferingEntity entity, DbQuery query, Session session) {
        try {
            OfferingOutput result = createCondensed(entity, query);
            addService(result, entity, query);
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

}
