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
package org.n52.series.db.beans;

import java.util.Date;
import java.util.Set;

import org.n52.series.db.DataModelUtil;

import com.vividsolutions.jts.geom.Geometry;

public class OfferingEntity extends HierarchicalEntity<OfferingEntity> {

    private Geometry envelope;

    private Set<String> observationTypes;

    private Set<String> featureTypes;

    private Date phenomenonTimeStart;

    private Date phenomenonTimeEnd;

    private Date resultTimeStart;

    private Date resultTimeEnd;

    /**
     * @return the envelope
     */
    public Geometry getEnvelope() {
        return envelope;
    }

    /**
     * @param envelope
     *        the envelope to set
     */
    public void setEnvelope(Geometry envelope) {
        this.envelope = envelope;
    }

    public boolean hasEnvelope() {
        return getEnvelope() != null && !getEnvelope().isEmpty();
    }

    /**
     * @return the observationTypes
     */
    public Set<String> getObservationTypes() {
        return observationTypes;
    }

    /**
     * @param observationTypes
     *        the observationTypes to set
     */
    public void setObservationTypes(Set<String> observationTypes) {
        this.observationTypes = observationTypes;
    }

    public boolean hasObservationTypes() {
        return getObservationTypes() != null && !getObservationTypes().isEmpty();
    }

    /**
     * @return the featureTypes
     */
    public Set<String> getFeatureTypes() {
        return featureTypes;
    }

    /**
     * @param featureTypes
     *        the featureTypes to set
     */
    public void setFeatureTypes(Set<String> featureTypes) {
        this.featureTypes = featureTypes;
    }

    public boolean hasFeatureTypes() {
        return getFeatureTypes() != null && !getFeatureTypes().isEmpty();
    }

    /**
     * @return the phenomenonTimeStart
     */
    public Date getPhenomenonTimeStart() {
        return DataModelUtil.createUnmutableTimestamp(phenomenonTimeStart);
    }

    /**
     * @param phenomenonTimeStart
     *        the phenomenonTimeStart to set
     */
    public void setPhenomenonTimeStart(Date phenomenonTimeStart) {
        this.phenomenonTimeStart = DataModelUtil.createUnmutableTimestamp(phenomenonTimeStart);
    }

    /**
     * @return the phenomenonTimeEnd
     */
    public Date getPhenomenonTimeEnd() {
        return DataModelUtil.createUnmutableTimestamp(phenomenonTimeEnd);
    }

    /**
     * @param phenomenonTimeEnd
     *        the phenomenonTimeEnd to set
     */
    public void setPhenomenonTimeEnd(Date phenomenonTimeEnd) {
        this.phenomenonTimeEnd = DataModelUtil.createUnmutableTimestamp(phenomenonTimeEnd);
    }

    /**
     * @return the resultTimeStart
     */
    public Date getResultTimeStart() {
        return DataModelUtil.createUnmutableTimestamp(resultTimeStart);
    }

    /**
     * @param resultTimeStart
     *        the resultTimeStart to set
     */
    public void setResultTimeStart(Date resultTimeStart) {
        this.resultTimeStart = DataModelUtil.createUnmutableTimestamp(resultTimeStart);
    }

    /**
     * @return the resultTimeEnd
     */
    public Date getResultTimeEnd() {
        return DataModelUtil.createUnmutableTimestamp(resultTimeEnd);
    }

    /**
     * @param resultTimeEnd
     *        the resultTimeEnd to set
     */
    public void setResultTimeEnd(Date resultTimeEnd) {
        this.resultTimeEnd = DataModelUtil.createUnmutableTimestamp(resultTimeEnd);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName())
          .append(" [");
        sb.append(" Domain id: ")
          .append(getDomainId());
        return sb.append(" ]")
                 .toString();
    }

}
