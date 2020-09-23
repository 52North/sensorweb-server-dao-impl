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
package org.n52.series.db.assembler;

import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.assembler.mapper.DatasetOutputMapper;
import org.n52.series.db.assembler.mapper.FeatureOutputMapper;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;

public interface ParameterDatasetOutputAssembler {

    default DatasetOutput<AbstractValue<?>> getDataset(DatasetEntity datasetEntity, DbQuery query) {
        return new DatasetOutputMapper(query).createCondensed(datasetEntity, new DatasetOutput());
    }

    default FeatureOutput getFeature(DatasetEntity datasetEntity, DbQuery query) {
        return new FeatureOutputMapper(query).createCondensed(datasetEntity.getFeature(), new FeatureOutput());
    }

    default OfferingOutput getOffering(DatasetEntity datasetEntity, DbQuery query) {
        return getDefaultMapper(query).createCondensed(datasetEntity.getOffering(),
                new OfferingOutput());
    }

    default PhenomenonOutput getPhenomenon(DatasetEntity datasetEntity, DbQuery query) {
        return getDefaultMapper(query).createCondensed(datasetEntity.getPhenomenon(),
                new PhenomenonOutput());
    }

    default CategoryOutput getCategory(DatasetEntity datasetEntity, DbQuery query) {
        return getDefaultMapper(query).createCondensed(datasetEntity.getCategory(),
                new CategoryOutput());
    }

    default ProcedureOutput getProcedure(DatasetEntity datasetEntity, DbQuery query) {
        return getDefaultMapper(query).createCondensed(datasetEntity.getProcedure(),
                new ProcedureOutput());
    }

    default PlatformOutput getPlatform(DatasetEntity datasetEntity, DbQuery query) {
        return getDefaultMapper(query).createCondensed(datasetEntity.getPlatform(),
                new PlatformOutput());
    }

    default ServiceOutput getService(ServiceEntity service, DbQuery query) {
        return getDefaultMapper(query).createCondensed(service, new ServiceOutput());
    }

    default ParameterOutputSearchResultMapper getDefaultMapper(DbQuery query) {
        return new ParameterOutputSearchResultMapper(query);
    }

}
