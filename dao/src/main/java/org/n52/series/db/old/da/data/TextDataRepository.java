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
package org.n52.series.db.old.da.data;

import java.util.List;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.text.TextValue;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DataDao;

//@ValueAssemblerComponent(value = "text", datasetEntityType = DatasetEntity.class)
public class TextDataRepository extends AbstractDataRepository<TextDataEntity, TextValue, String> {

    public TextDataRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected TextValue createEmptyValue() {
        return new TextValue();
    }

    @Override
    protected Data<TextValue> assembleData(Long dataset, DbQuery query, Session session) {
        Data<TextValue> result = new Data<>();
        DataDao<TextDataEntity> dao = new DataDao<>(session);
        List<TextDataEntity> observations = dao.getAllInstancesFor(dataset, query);
        for (TextDataEntity observation : observations) {
            if (observation != null) {
                result.addNewValue(assembleDataValue(observation, observation.getDataset(), query));
            }
        }
        return result;
    }

    @Override
    public TextValue assembleDataValue(TextDataEntity observation, DatasetEntity series, DbQuery query) {
        ServiceEntity service = getServiceEntity(series);
        String observationValue = !service.isNoDataValue(observation) ? observation.getValue() : null;

        TextValue value = createValue(observation, series, query, observationValue);
        return value;
    }

    private TextValue createValue(TextDataEntity observation, DatasetEntity series, DbQuery query,
            String observationValue) {
        ServiceEntity service = getServiceEntity(series);
        String textValue = !service.isNoDataValue(observation) ? observation.getValue() : null;
        return createValue(textValue, observation, query);
    }

    TextValue createValue(String observationValue, TextDataEntity observation, DbQuery query) {
        TextValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return value;
    }

}
