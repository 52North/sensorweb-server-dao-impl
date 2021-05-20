/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.sensorweb.server.db.assembler.mapper;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.n52.io.HrefHelper;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.DescribableEntity;
import org.slf4j.Logger;

public interface OutputMapper<E extends DescribableEntity, O extends ParameterOutput> {

    default O createCondensed(E entity) {
        return createCondensed(entity, getParameterOuput());
    }

    O createCondensed(E entity, O output);

    default O createExpanded(E entity) {
        return createExpanded(entity, getParameterOuput());
    }

    default O createExpanded(E entity, O output) {
        return addExpandedValues(entity, createCondensed(entity, output));
    }

    O addExpandedValues(E entity, O output);

    O getParameterOuput();

    boolean hasSelect();

    Set<String> getSelection();

    Map<String, Set<String>> getSubSelection();

    default Set<String> getSubSelection(String sub) {
        return getSubSelection().get(sub);
    }

    default boolean isSubSelected(String sub, String selection) {
        return !hasSelect() || (getSubSelection().containsKey(sub) && checkSubSelected(sub, selection));
    }

    default boolean checkSubSelected(String sub, String selection) {
        return getSubSelection().get(sub).stream().filter(s -> s.startsWith(selection.toLowerCase(Locale.ROOT)))
                .findFirst().isPresent();
    }

    default boolean isSelected(String selection) {
        return !hasSelect() || getSelection().contains(selection) || checkSelected(selection);
    }

    default boolean checkSelected(String selection) {
        return getSelection().stream().filter(s -> s.startsWith(selection.toLowerCase(Locale.ROOT))).findFirst()
                .isPresent();
    }

    default void addAll(O result, E entity, DbQuery query, IoParameters parameters) {
        addLabel(result, entity, query, parameters);
        addDomainId(result, entity, query, parameters);
        addHref(result, entity, query, parameters);
    }

    default void addSelected(O result, E entity, DbQuery query, IoParameters parameters) {
        for (String selected : getSelection()) {
            switch (selected) {
                case ParameterOutput.LABEL:
                    addLabel(result, entity, query, parameters);
                    break;
                case ParameterOutput.DOMAIN_ID:
                    addDomainId(result, entity, query, parameters);
                    break;
                case ParameterOutput.HREF:
                    addHref(result, entity, query, parameters);
                    break;
                default:
                    break;
            }
        }
    }

    default void addLabel(O result, E entity, DbQuery query, IoParameters parameters) {
        result.setValue(ParameterOutput.LABEL, createLabel(entity, query), parameters,
                result::setLabel);
    }

    default String createLabel(E entity, DbQuery query) {
        return entity.getLabelFrom(query.getLocaleForLabel());
    }

    default void addDomainId(O result, E entity, DbQuery query, IoParameters parameters) {
        result.setValue(ParameterOutput.DOMAIN_ID, entity.getIdentifier(), parameters, result::setDomainId);
    }

    default void addHref(O result, E entity, DbQuery query, IoParameters parameters) {
        result.setValue(ParameterOutput.HREF,
                HrefHelper.constructHref(getHrefBase(), getCollectionName(result, entity)) + "/" + result.getId(),
                parameters, result::setHref);
    }

    default String getCollectionName(O result, E entity) {
        return result.getCollectionName();
    }

    String getHrefBase();

    Logger getLogger();

    default void log(DescribableEntity entity, Exception e) {
        getLogger().error("Error while processing {} with id {}! Exception: {}", entity.getClass().getSimpleName(),
                entity.getId(), e);
    }

}
