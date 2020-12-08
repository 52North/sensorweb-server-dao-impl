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
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.List;

import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.text.TextValue;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.series.db.beans.TextProfileDatasetEntity;
import org.n52.series.db.dao.DbQuery;

public class TextProfileDataRepository extends ProfileDataRepository<String, TextProfileDatasetEntity> {

    private final TextDataRepository textRepository;

    public TextProfileDataRepository() {
        this.textRepository = new TextDataRepository();
    }

    @Override
    public Class<TextProfileDatasetEntity> getDatasetEntityType() {
        return TextProfileDatasetEntity.class;
    }

    @Override
    protected ProfileValue<String> createValue(ProfileDataEntity observation,
                                               ProfileDatasetEntity dataset,
                                               DbQuery query) {
        ProfileValue<String> profile = createProfileValue(observation, query);
        List<ProfileDataItem<String>> dataItems = new ArrayList<>();
        for (DataEntity< ? > dataEntity : observation.getValue()) {
            TextDataEntity textEntity = (TextDataEntity) dataEntity;
            TextValue valueItem = textRepository.createValue(textEntity.getValue(), textEntity, query);
            addParameters(textEntity, valueItem, query);
            dataItems.add(assembleDataItem(textEntity, profile, valueItem.getParameters(), dataset));
        }
        profile.setValue(dataItems);
        return profile;
    }

}
