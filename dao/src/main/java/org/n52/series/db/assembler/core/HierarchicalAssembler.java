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
package org.n52.series.db.assembler.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.repositories.ParameterDataRepository;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.assembler.ParameterOutputAssembler;
import org.n52.series.db.beans.HierarchicalEntity;
import org.n52.series.spi.search.SearchResult;

public abstract class HierarchicalAssembler<E extends HierarchicalEntity<E>,
                                            O extends HierarchicalParameterOutput<O>,
                                            S extends SearchResult>
        extends ParameterOutputAssembler<E, O, S> {

    public HierarchicalAssembler(ParameterDataRepository<E> parameterRepository, DatasetRepository datasetRepository) {
        super(parameterRepository, datasetRepository);
    }

    @Override
    protected O createExpanded(E entity, DbQuery query) {
        return createExpanded(entity, query, false, false, query.getLevel());
    }

    protected O createExpanded(E entity, DbQuery query, boolean isParent, boolean isChild, Integer level) {
        O result = super.createExpanded(entity, query);
        if (!isParent && !isChild && entity.hasParents()) {
            List<O> parents = getMemberList(entity.getParents(), query, level, true, false);
            result.setValue(HierarchicalParameterOutput.PARENTS, parents, query.getParameters(), result::setParents);
        }
        if (level != null && level > 0) {
            if (((!isParent && !isChild) || (!isParent && isChild)) && entity.hasChildren()) {
                List<O> children = getMemberList(entity.getChildren(), query, level - 1, false, true);
                result.setValue(HierarchicalParameterOutput.CHILDREN, children, query.getParameters(),
                        result::setChildren);
            }
        }
        return result;
    }

    private List<O> getMemberList(Set<E> entities, DbQuery query, Integer level, boolean isNotParent,
            boolean isNotChild) {
        List<O> list = new LinkedList<>();
        for (E e : entities) {
            list.add(createExpanded(e, query, isNotParent, isNotChild, level));
        }
        return list;
    }
}
