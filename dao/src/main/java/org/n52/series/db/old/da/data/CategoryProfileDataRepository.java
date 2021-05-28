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

package org.n52.series.db.old.da.data;

import java.util.ArrayList;
import java.util.List;

import org.n52.io.response.dataset.category.CategoryValue;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.db.beans.CategoryDataEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.old.HibernateSessionStore;

public class CategoryProfileDataRepository extends ProfileDataRepository<String, String> {

    private final CategoryDataRepository categoryRepository;

    public CategoryProfileDataRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.categoryRepository = new CategoryDataRepository(sessionStore, dbQueryFactory);
    }

    @Override
    public ProfileValue<String> assembleDataValue(ProfileDataEntity observation, DatasetEntity dataset,
            DbQuery query) {
        ProfileValue<String> profile = createProfileValue(observation, query);
        List<ProfileDataItem<String>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            CategoryDataEntity categoryEntity = (CategoryDataEntity) dataEntity;
            CategoryValue valueItem = categoryRepository.createValue(categoryEntity.getValue(), categoryEntity, query);
            addParameters(categoryEntity, valueItem, query);
            if (observation.hasVerticalFrom() || observation.hasVerticalTo()) {
                dataItems.add(assembleDataItem(categoryEntity, profile, observation, query));
            } else {
                dataItems.add(assembleDataItem(categoryEntity, profile, valueItem.getParameters(), dataset, query));
            }
        }
        profile.setValue(dataItems);
        return profile;
    }

    @Override
    protected ProfileValue<String> createValue(ProfileDataEntity observation, DatasetEntity dataset, DbQuery query) {
        ProfileValue<String> value = prepareValue(observation, query);
        List<ProfileDataItem<String>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            CategoryDataEntity categoryEntity = (CategoryDataEntity) dataEntity;
            CategoryValue valueItem = categoryRepository.createValue(categoryEntity.getValue(), categoryEntity, query);
            addParameters(categoryEntity, valueItem, query);
            if (dataEntity.hasVerticalFrom() || dataEntity.hasVerticalTo()) {
                dataItems.add(assembleDataItem(categoryEntity, value, observation, query));
            } else {
                dataItems.add(assembleDataItem(categoryEntity, value, valueItem.getParameters(), dataset, query));
            }
        }
        value.setValue(dataItems);
        return value;
    }

}
