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
package org.n52.series.db.da.sos;

import javax.inject.Inject;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.series.db.HibernateSessionStore;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.sos.ds.hibernate.HibernateSessionHolder;
import org.n52.sos.ds.hibernate.SessionFactoryProvider;

public class SOSHibernateSessionHolder implements HibernateSessionStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(SOSHibernateSessionHolder.class);
    private SessionFactoryProvider provider;
    private HibernateSessionHolder sessionHolder;

    @Override
    public void returnSession(Session session) {
        this.sessionHolder.returnSession(session);
    }

    @Override
    public Session getSession() {
        try {
            return this.sessionHolder.getSession();
        } catch (OwsExceptionReport e) {
            throw new IllegalStateException("Could not get hibernate session.", e);
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("shutdown '{}'", getClass().getSimpleName());
        if (this.provider != null) {
            this.provider.destroy();
        }
    }

    @Inject
    public void setProvider(SessionFactoryProvider provider) {
        this.provider = provider;
        this.sessionHolder = new HibernateSessionHolder(provider);
    }
}
