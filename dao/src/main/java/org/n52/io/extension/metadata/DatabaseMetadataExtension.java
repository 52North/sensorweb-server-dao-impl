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
package org.n52.io.extension.metadata;

import java.util.Collection;
import java.util.Map;

import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.extension.MetadataExtension;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMetadataExtension extends MetadataExtension<ParameterOutput> {

    private static final String EXTENSION_NAME = "databaseMetadata";

    private final MetadataAssembler repository;

    public DatabaseMetadataExtension(MetadataAssembler repository) {
        this.repository = repository;
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public Map<String, Object> getExtras(ParameterOutput output, IoParameters parameters) {
        return repository.getExtras(output, parameters);
    }

    @Override
    public Collection<String> getExtraMetadataFieldNames(ParameterOutput output) {
        return repository.getFieldNames(output.getId());
    }

}
