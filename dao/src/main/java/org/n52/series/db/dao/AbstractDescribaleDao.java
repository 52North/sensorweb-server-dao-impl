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
package org.n52.series.db.dao;

import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.Session;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.shetland.ogc.OGCConstants;
import org.n52.shetland.ogc.gml.AbstractGML;
import org.n52.shetland.ogc.gml.CodeType;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;

public abstract class AbstractDescribaleDao<T>
        extends
        AbstractDao<T> {

    public AbstractDescribaleDao(Session session) {
        super(session);
    }

    public void addDomainIdNameDescription(AbstractGML abstractFeature, DescribableEntity entity) {
        addDomainId(abstractFeature, entity);
        addName(abstractFeature, entity);
        addDescription(abstractFeature, entity);
    }

    public void addDomainId(AbstractGML abstractFeature, DescribableEntity entity) {
        addDomainId(entity, abstractFeature.getIdentifierCodeWithAuthority());
    }

    public void addDomainId(DescribableEntity entity, CodeWithAuthority identifier) {
        String value = identifier != null && identifier.isSetValue() ? identifier.getValue() : null;
        String codespace =
                identifier != null && identifier.isSetCodeSpace() ? identifier.getCodeSpace() : OGCConstants.UNKNOWN;
        entity.setIdentifier(value);
        entity.setCodespace(new CodespaceDao(session).getOrInsert(codespace));
    }

    public void addName(AbstractGML abstractFeature, DescribableEntity entity) {
        addName(entity, abstractFeature.getFirstName());
    }

    public void addName(DescribableEntity entity, CodeType name) {
        String value = name != null && name.isSetValue() ? name.getValue() : null;
        String codespace =
                name != null && name.isSetCodeSpace() ? name.getCodeSpace().toString() : OGCConstants.UNKNOWN;
        entity.setName(value);
        entity.setCodespaceName(new CodespaceDao(session).getOrInsert(codespace));
    }

    public void addDescription(AbstractGML abstractFeature, DescribableEntity entity) {
        addDescription(entity, abstractFeature.getDescription());
    }

    public void addDescription(DescribableEntity entity, String description) {
        if (description != null && !description.isEmpty()) {
            entity.setDescription(description);
        }
    }

    public void getAndAddIdentifierNameDescription(AbstractGML abstractFeature, DescribableEntity entity)
            throws OwsExceptionReport {
        abstractFeature.setIdentifier(getIdentifier(entity));
        abstractFeature.addName(getName(entity));
        abstractFeature.setDescription(getDescription(entity));
    }

    public CodeWithAuthority getIdentifier(DescribableEntity entity) {
        CodeWithAuthority identifier = new CodeWithAuthority(entity.getIdentifier());
        if (entity.isSetCodespace()) {
            identifier.setCodeSpace(entity.getCodespace().getCodespace());
        }
        return identifier;
    }

    public CodeType getName(DescribableEntity entity)
            throws OwsExceptionReport {
        if (entity.isSetName()) {
            CodeType name = new CodeType(entity.getName());
            if (entity.isSetCodespaceName()) {
                try {
                    name.setCodeSpace(new URI(entity.getCodespaceName().getCodespace()));
                } catch (URISyntaxException e) {
                    throw new NoApplicableCodeException().causedBy(e).withMessage("Error while creating URI from '{}'",
                            entity.getCodespaceName().getCodespace());
                }
            }
            return name;
        }
        return null;
    }

    public String getDescription(DescribableEntity entity) {
        if (entity.isSetDescription()) {
            return entity.getDescription();
        }
        return null;
    }

}
