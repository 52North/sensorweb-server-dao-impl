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

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.dao.DbQuery;

public class PlatformMapper extends AbstractOuputMapper<PlatformOutput, PlatformEntity> {

    private DatasetMapper<AbstractValue<?>> datasetMapper;
//    private Map<Long, DatasetOutput> datasets = new LinkedHashMap<>();

    public PlatformMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params, false);
    }

    public PlatformMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        super(mapperFactory, params, subMapper);
        if (!subMapper) {
            this.datasetMapper =
                    getMapperFactory().getDatasetMapper(params.withSubSelectFilter(PlatformOutput.DATASETS), false);
        }
    }

    @Override
    public PlatformOutput createCondensed(PlatformEntity entity, DbQuery query) {
        return createCondensed(new PlatformOutput(), entity, query);
    }

    @Override
    public PlatformOutput createExpanded(PlatformEntity entity, DbQuery query, Session session) {
        try {
            PlatformOutput result = createCondensed(entity, query);
            addService(result, entity, query);
            if (query.getParameters().isSelected(PlatformOutput.DATASETS)) {
                List<DatasetOutput<AbstractValue<?>>> datasets =
                        entity.getDatasets().stream()
                                .map(d -> datasetMapper.createCondensed(d,
                                        query.withSubSelectFilter(PlatformOutput.DATASETS)))
                                .collect(Collectors.toList());
                result.setValue(PlatformOutput.DATASETS, datasets, query.getParameters(), result::setDatasets);
            }
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

}
