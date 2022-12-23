/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.old.HibernateSessionStore;

//@ValueAssemblerComponent(value = "quantity-profile", datasetEntityType = DatasetEntity.class)
public class QuantityProfileDataRepository extends ProfileDataRepository<BigDecimal, BigDecimal> {

    private final QuantityDataRepository dataAssembler;

    public QuantityProfileDataRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.dataAssembler = new QuantityDataRepository(sessionStore, dbQueryFactory);
    }

    @Override
    public ProfileValue<BigDecimal> assembleDataValue(ProfileDataEntity observation, DatasetEntity datasetEntity,
            DbQuery query) {
        ProfileValue<BigDecimal> profile = createProfileValue(observation, query);
        List<ProfileDataItem<BigDecimal>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            QuantityDataEntity quantityEntity = (QuantityDataEntity) dataEntity;
            QuantityValue valueItem = dataAssembler.createValue(quantityEntity.getValue(), quantityEntity, query);
            addParameters(quantityEntity, valueItem, query);
            if (dataEntity.hasVerticalFrom() || dataEntity.hasVerticalTo()) {
                dataItems.add(assembleDataItem(quantityEntity, profile, observation, query));
            } else {
                dataItems.add(
                        assembleDataItem(quantityEntity, profile, valueItem.getParameters(), datasetEntity, query));
            }
        }
        profile.setValue(dataItems);
        return profile;
    }

    @Override
    protected ProfileValue<BigDecimal> createValue(ProfileDataEntity observation, DatasetEntity dataset,
            DbQuery query) {
        ProfileValue<BigDecimal> value = prepareValue(observation, query);
        List<ProfileDataItem<BigDecimal>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            QuantityDataEntity quantityEntity = (QuantityDataEntity) dataEntity;
            QuantityValue valueItem = dataAssembler.createValue(quantityEntity.getValue(), quantityEntity, query);
            addParameters(quantityEntity, valueItem, query);
            if (dataEntity.hasVerticalFrom() || dataEntity.hasVerticalTo()) {
                dataItems.add(assembleDataItem(quantityEntity, value, observation, query));
            } else {
                dataItems.add(assembleDataItem(quantityEntity, value, valueItem.getParameters(), dataset, query));
            }
        }
        value.setValue(dataItems);
        return value;
    }

    @Override
    public ProfileValue<BigDecimal> getFirstValue(DatasetEntity entity, DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProfileValue<BigDecimal> getLastValue(DatasetEntity entity, DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

}
