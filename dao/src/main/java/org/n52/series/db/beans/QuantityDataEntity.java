/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.beans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
public class QuantityDataEntity extends DataEntity<BigDecimal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuantityDataEntity.class);

    private static final BigDecimal DOUBLE_THRESHOLD = new BigDecimal(0.0001d);

    @Override
    public boolean isNoDataValue(Collection<String> noDataValues) {
        BigDecimal value = getValue();
        return value == null
                || containsValue(noDataValues, value);
    }

    private boolean containsValue(Collection<String> collection, BigDecimal key) {
        if (collection == null) {
            return false;
        }
        for (BigDecimal noDataValue : convert(collection)) {
            if (noDataValue.subtract(key)
                           .abs()
                           .compareTo(DOUBLE_THRESHOLD) < 0) {
                return true;
            }
        }
        return false;
    }

    private Collection<BigDecimal> convert(Collection<String> collection) {
        List<BigDecimal> validatedValues = new ArrayList<>();
        for (String value : collection) {
            String trimmed = value.trim();
            try {
                validatedValues.add(new BigDecimal(trimmed));
            } catch (NumberFormatException e) {
                LOGGER.trace("Ignoring NO_DATA value {} (not a big decimal value).", trimmed);
            }
        }
        return validatedValues;
    }

}
