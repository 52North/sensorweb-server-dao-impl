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
package org.n52.sensorweb.server.db.assembler;

import org.n52.series.db.beans.DescribableEntity;
import org.springframework.transaction.annotation.Transactional;

public interface InsertAssembler<E extends DescribableEntity> extends TransactionalAssembler<E> {

    E refresh(E entity);

    @Transactional
    default E getOrInsertInstance(E entity) {
        E instance = getParameterRepository().getInstance(entity);
        if (instance != null) {
            return instance;
        }
        checkParameter(entity);
        checkReferencedEntities(entity);
        return refresh(getParameterRepository().saveAndFlush(entity));
    }

    default E getOrUpdateInstance(E instance, E entity) {
        if (entity.getId() == null) {
            entity.setId(instance.getId());
            entity.setStaIdentifier(instance.getStaIdentifier());
            checkParameterUpdate(entity, instance);
            checkReferencedEntities(entity);
            return refresh(getParameterRepository().saveAndFlush(entity));
        } else {
            return instance;
        }
    }

    @Transactional
    default E updateInstance(E entity) {
        checkParameter(entity);
        return refresh(getParameterRepository().saveAndFlush(entity));
    }

    default E checkParameterUpdate(E entity, E instance) {
        return entity;
    }

    default E checkParameter(E entity) {
        return entity;
    }

    default E checkReferencedEntities(E entity) {
        return entity;
    }

}
