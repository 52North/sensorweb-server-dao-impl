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
package org.n52.sensorweb.server.db.assembler.value;

import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.sensorweb.server.db.ValueAssemblerComponent;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.core.DataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.math.BigDecimal;
import java.util.List;

@ValueAssemblerComponent(value = "quantity-trajectory", datasetEntityType = DatasetEntity.class)
public class QuantityTrajectoryValueAssembler
    extends TrajectoryValueAssembler<QuantityDataEntity, QuantityValue, BigDecimal> {

    private final QuantityValueAssembler assembler;

    @SuppressFBWarnings({ "EI_EXPOSE_REP2" })
    public QuantityTrajectoryValueAssembler(DataRepository<QuantityDataEntity> trajectoryDataRepository,
                                            DatasetRepository datasetRepository,
                                            QuantityValueAssembler assembler) {
        super(trajectoryDataRepository, datasetRepository);
        this.assembler = assembler;
    }

    @Override
    public List<ReferenceValueOutput<QuantityValue>> getReferenceValues(DatasetEntity datasetEntity, DbQuery query) {
        return assembler.getReferenceValues(datasetEntity, query);
    }

    @Override
    public QuantityValue assembleDataValue(QuantityDataEntity dataEntity, DatasetEntity datasetEntity, DbQuery query) {
        QuantityValue quantityValue = assembler.assembleDataValue(dataEntity, datasetEntity, query);
        if (dataEntity.isSetGeometryEntity()) {
            quantityValue.setGeometry(dataEntity.getGeometryEntity().getGeometry());
        }
        return quantityValue;
    }

    @Override public boolean hasConnector(DatasetEntity entity) {
        return assembler.hasConnector(entity);
    }

    @Override public ValueConnector getConnector(DatasetEntity entity) {
        return assembler.getConnector(entity);
    }
}
