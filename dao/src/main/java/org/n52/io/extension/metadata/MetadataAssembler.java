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
package org.n52.io.extension.metadata;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.n52.io.extension.ExtensionAssembler;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.parameter.JsonParameterEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MetadataAssembler extends ExtensionAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataAssembler.class);

    public MetadataAssembler(DatasetRepository datasetRepository, DbQueryFactory dbQueryFactory) {
        super(datasetRepository, dbQueryFactory);
    }

    protected List<String> getFieldNames(String id) {
        DatasetEntity dataset = getDatasetRepository().getOne(Long.parseLong(id));
        return dataset.hasParameters()
                ? dataset.getParameters().stream().map(p -> p.getName()).collect(Collectors.toList())
                : Collections.emptyList();
    }

    protected Map<String, Object> getExtras(ParameterOutput output, IoParameters parameters) {
        final Set<String> fields = parameters.getFields();
        DatasetEntity dataset = getDatasetRepository().getOne(Long.parseLong(output.getId()));
        return !dataset.hasParameters() ? new LinkedHashMap<>()
                : fields == null ? convertToOutputs(dataset.getParameters())
                        : convertToOutputs(dataset.getParameters().stream().filter(p -> fields.contains(p.getName()))
                                .collect(Collectors.toList()));
    }

    private Map<String, Object> convertToOutputs(Collection<ParameterEntity<?>> allInstances) {
        if (allInstances == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> outputs = new HashMap<>();
        for (ParameterEntity<?> entity : allInstances) {
            outputs.put(entity.getName(), toOutput(entity));
        }
        return outputs;
    }

    private DatabaseMetadataOutput toOutput(ParameterEntity<?> entity) {
        DatabaseMetadataOutput<Object> databaseMetadataOutput =
                DatabaseMetadataOutput.create().setLastUpdatedAt(entity.getLastUpdate());
        if (entity instanceof JsonParameterEntity) {
            try {
                databaseMetadataOutput
                        .setValue(new ObjectMapper().readTree(((JsonParameterEntity) entity).getValue()));
            } catch (IOException e) {
                LOGGER.error("Could not parse to json ({}): {}", entity.getName(), entity.getValue(), e);
            }
        } else {
            databaseMetadataOutput.setValue(entity.getValue());
        }
        return databaseMetadataOutput;
    }

}
