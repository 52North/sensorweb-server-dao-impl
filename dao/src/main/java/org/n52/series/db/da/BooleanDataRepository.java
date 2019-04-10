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

import java.util.List;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.bool.BooleanValue;
import org.n52.series.db.beans.BooleanDataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class BooleanDataRepository
        extends AbstractDataRepository<DatasetEntity, BooleanDataEntity, BooleanValue, Boolean> {

    @Override
    protected BooleanValue createEmptyValue() {
        return new BooleanValue();
    }

    @Override
    protected Data<BooleanValue> assembleData(Long dataset, DbQuery query, Session session) {
        Data<BooleanValue> result = new Data<>();
        DataDao<BooleanDataEntity> dao = createDataDao(session);
        List<BooleanDataEntity> observations = dao.getAllInstancesFor(dataset, query);
        for (BooleanDataEntity observation : observations) {
            if (observation != null) {
                result.addNewValue(assembleDataValue(observation, observation.getDataset(), query));
            }
        }
        return result;
    }

    @Override
    public BooleanValue assembleDataValue(BooleanDataEntity observation,
                                            DatasetEntity series,
                                             DbQuery query) {
        ServiceEntity service = getServiceEntity(series);
        Boolean observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        BooleanValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

}
