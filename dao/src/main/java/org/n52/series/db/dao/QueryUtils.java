/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.dao;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.n52.series.db.beans.DescribableEntity;

public class QueryUtils {

    public static String createAssociation(String alias, String property) {
        return alias != null && !alias.isEmpty()
            ? alias + "." + property
            : property;
    }

    public static DetachedCriteria projectionOnPkid(DetachedCriteria criteria) {
        return projectionOnPkid(null, criteria);
    }

    public static DetachedCriteria projectionOnPkid(String member, DetachedCriteria criteria) {
        return projectionOnPkid(null, member, criteria);
    }

    public static DetachedCriteria projectionOnPkid(String alias, String member, DetachedCriteria criteria) {
        return criteria.setProjection(projectionOnPkid(alias, member));
    }

    public static PropertyProjection projectionOnPkid() {
        return projectionOnPkid((String) null);
    }

    public static PropertyProjection projectionOnPkid(String member) {
        return projectionOnPkid(null, member);
    }

    public static PropertyProjection projectionOnPkid(String alias, String member) {
        return projectionOn(alias, member, DescribableEntity.PROPERTY_ID);
    }

    public static DetachedCriteria projectionOn(String property, DetachedCriteria criteria) {
        return projectionOn(null, property, criteria);
    }

    public static DetachedCriteria projectionOn(String member, String property, DetachedCriteria criteria) {
        return projectionOn(null, member, property, criteria);
    }

    public static DetachedCriteria projectionOn(String alias,
                                                String member,
                                                String property,
                                                DetachedCriteria criteria) {
        return criteria.setProjection(projectionOn(alias, member, property));
    }

    public static PropertyProjection projectionOn(String property) {
        return projectionOn(null, property);
    }

    public static PropertyProjection projectionOn(String member, String property) {
        return projectionOn(null, member, property);
    }

    public static PropertyProjection projectionOn(String alias, String member, String property) {
        String qMember = QueryUtils.createAssociation(alias, member);
        String association = QueryUtils.createAssociation(qMember, property);
        return Projections.property(association);
    }

    public static void setFilterProjectionOn(String parameter, DetachedCriteria c) {
        // String[] associationPathElements = parameter.split("\\.", 2);
        // if (associationPathElements.length == 2) {
        // // other observationconstellation members
        // String member = associationPathElements[1];
        // projectionOnPkid(alias, member, c);
        // } else {
        if (!parameter.isEmpty()) {
            // feature case only
            projectionOn(parameter, c);
        } else {
            // dataset case
            projectionOnPkid(c);
        }
        // }
    }

    public static Set<Long> parseToIds(Collection<String> ids) {
        return ids.stream()
                  .map(e -> parseToId(e))
                  .collect(Collectors.toSet());
    }

    public static SimpleExpression matchesPkid(String id) {
        return Restrictions.eq(DescribableEntity.PROPERTY_ID, QueryUtils.parseToId(id));
    }

    /**
     * parsed the given string to the raw database id. strips prefixes ending with a "<tt>_</tt>", e.g.
     * <tt>platform_track_8</tt> will return <tt>8</tt>.
     *
     * @param id
     *        the id string to parse.
     * @return the long value of given string or {@link Long#MIN_VALUE} if string could not be parsed to type
     *         long.
     */
    public static Long parseToId(String id) {
        try {
            String rawId = id.substring(id.lastIndexOf("_") + 1);
            return Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

}
