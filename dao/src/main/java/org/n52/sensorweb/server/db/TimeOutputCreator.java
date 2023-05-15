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
package org.n52.sensorweb.server.db;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.io.request.IoParameters;
import org.n52.io.response.TimeOutput;

public interface TimeOutputCreator {

    String OFFSET_REGEX = "([+-](?:2[0-3]|[01][0-9]):[0-5][0-9])";

    default TimeOutput createTimeOutput(Date date, IoParameters parameters) {
        if (date != null) {
            return new TimeOutput(new DateTime(date), parameters.formatToUnixTime());
        }
        return null;
    }

    default TimeOutput createTimeOutput(Date date, String originTimezone, IoParameters parameters) {
        if (date != null) {
            DateTimeZone zone = getOriginTimeZone(originTimezone);
            return new TimeOutput(new DateTime(date).withZone(zone), parameters.formatToUnixTime());
        }
        return null;
    }

    default DateTimeZone getOriginTimeZone(String originTimezone) {
        if (originTimezone != null && !originTimezone.isEmpty()) {
            if (originTimezone.matches(OFFSET_REGEX)) {
                return DateTimeZone.forTimeZone(TimeZone.getTimeZone(ZoneOffset.of(originTimezone).normalized()));
            } else {
                return DateTimeZone.forID(originTimezone.trim());
            }
        }
        return DateTimeZone.UTC;
    }
}
