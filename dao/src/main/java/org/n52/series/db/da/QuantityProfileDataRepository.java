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
package org.n52.series.db.da;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.series.db.DataRepositoryComponent;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.dao.DbQuery;

@DataRepositoryComponent(value = "quantity-profile", datasetEntityType = DatasetEntity.class)
public class QuantityProfileDataRepository extends ProfileDataRepository<DatasetEntity, BigDecimal, BigDecimal> {

    private final QuantityDataRepository quantityRepository;

    public QuantityProfileDataRepository() {
        this.quantityRepository = new QuantityDataRepository();
    }

    @Override
    protected ProfileValue<BigDecimal> createValue(ProfileDataEntity observation, DatasetEntity dataset,
            DbQuery query) {
        Locale locale = LocaleHelper.decode(query.getLocale());
        NumberFormat formatter = NumberFormat.getInstance(locale);

        ProfileValue<BigDecimal> profile = createProfileValue(observation, query);
        List<ProfileDataItem<BigDecimal>> dataItems = new ArrayList<>();
        for (DataEntity<?> dataEntity : observation.getValue()) {
            QuantityDataEntity quantity = (QuantityDataEntity) dataEntity;
            QuantityValue valueItem = quantityRepository.createValue(quantity.getValue(), quantity, query);
            addParameters(quantity, valueItem, query);
            if (dataEntity.hasVerticalFrom() || dataEntity.hasVerticalTo()) {
                ProfileDataItem<BigDecimal> item = assembleDataItem(quantity, profile, observation, query);
                item.setValueFormatter(formatter::format);
                dataItems.add(item);
            } else {
                Set<Map<String, Object>> parameters = valueItem.getParameters();
                ProfileDataItem<BigDecimal> item = assembleDataItem(quantity, profile, parameters, dataset, query);
                item.setValueFormatter(formatter::format);
                dataItems.add(item);
            }
        }
        profile.setValue(dataItems);
        return profile;
    }

}
