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

package org.n52.io.extension.parents;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.n52.io.extension.ExtensionAssembler;
import org.n52.io.request.IoParameters;
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.assembler.core.PlatformAssembler;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtrasHierarchicalParameterAssembler extends ExtensionAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtrasHierarchicalParameterAssembler.class);

    private static final String KEY_PROCEDURES = "procedures";

    private PlatformAssembler platformAssembler;

    public ExtrasHierarchicalParameterAssembler(PlatformAssembler platformAssembler,
            DatasetRepository datasetRepository, DbQueryFactory dbQueryFactory) {
        super(datasetRepository, dbQueryFactory);
        this.platformAssembler = platformAssembler;
    }

    Map<String, Set<HierarchicalParameterOutput>> getExtras(String platformId, IoParameters parameters) {
            DbQuery dbQuery = getDbQuery(parameters);
            Map<String, Set<HierarchicalParameterOutput>> extras = new HashMap<>();

            PlatformOutput platform = platformAssembler.getInstance(platformId, dbQuery);
            for (DatasetOutput<?> dataset : platform.getDatasets()) {
                String datasetId = dataset.getId();
                DatasetEntity instance = getDatasetRepository().getOne(Long.parseLong(datasetId));
                addProcedureParents(instance, dbQuery, extras);
            }

            return extras;
    }

    private void addProcedureParents(DatasetEntity instance,
                                     DbQuery dbQuery,
                                     Map<String, Set<HierarchicalParameterOutput>> extras) {
        if (!extras.containsKey(KEY_PROCEDURES)) {
            extras.put(KEY_PROCEDURES, new HashSet<>());
        }
        ProcedureEntity entity = instance.getProcedure();
        extras.get(KEY_PROCEDURES).addAll(getProcedureParents(entity, dbQuery));
    }

    private Set< ? extends HierarchicalParameterOutput> getProcedureParents(ProcedureEntity entity,
                                                                            DbQuery dbQuery) {
        return !entity.hasParents()
            ? Collections.singleton(getMapper(dbQuery).createCondensed(entity, new ProcedureOutput()))
            : new HashSet<>(entity.getParents()
                                  .stream()
                                  .map(e -> getMapper(dbQuery).createCondensed(e, new ProcedureOutput()))
                                  .collect(Collectors.toSet()));
    }

    protected ParameterOutputSearchResultMapper getMapper(final DbQuery query) {
        return new ParameterOutputSearchResultMapper(query);
    }

}
