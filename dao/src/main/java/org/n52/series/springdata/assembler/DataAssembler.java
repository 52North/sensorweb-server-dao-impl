
package org.n52.series.springdata.assembler;

import java.util.Date;

import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.AbstractValue.ValidTime;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.parameter.Parameter;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.springdata.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Geometry;

public abstract class DataAssembler<S extends DatasetEntity, E extends DataEntity< ? >, V extends AbstractValue< ? >> {

    private final DataRepository<E> dataRepository;

    /**
     * Preconfigured service entity. Alternative to {@link #serviceRepository} used for accessing service
     * entities from a database (in case there data model and mappings supports it).
     *
     * @see #assertServiceAvailable(DescribableEntity)
     */
    @Autowired(required = false)
    protected ServiceEntity serviceEntity;

    protected DataAssembler(final DataRepository<E> dataRepository) {
        this.dataRepository = dataRepository;
    }

    protected DataRepository<E> getDataRepository() {
        return dataRepository;
    }

    protected ServiceEntity getServiceEntity(final DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null
            ? entity.getService()
            : serviceEntity;
    }

    private void assertServiceAvailable(final DescribableEntity entity) throws IllegalStateException {
        if ( (serviceEntity == null) && (entity == null)) {
            throw new IllegalStateException("No service instance available");
        }
    }

    public abstract V assembleDataValue(final E observation, final S dataset, final DbQuery query);

    protected abstract V createEmptyValue();

    protected V prepareValue(final E observation, final DbQuery query) {
        final V emptyValue = createEmptyValue();
        if (observation == null) {
            return emptyValue;
        }

        final IoParameters parameters = query.getParameters();
        final Date timeend = observation.getSamplingTimeEnd();
        final Date timestart = observation.getSamplingTimeStart();
        if (parameters.isShowTimeIntervals() && (timestart != null)) {
            emptyValue.setTimestart(timestart.getTime());
        }
        emptyValue.setTimestamp(timeend.getTime());
        return emptyValue;
    }

    protected V addMetadatasIfNeeded(final E observation, final V value, final S dataset, final DbQuery query) {
        addResultTime(observation, value);

        if (query.isExpanded()) {
            addValidTime(observation, value);
            addParameters(observation, value, query);
            addGeometry(observation, value, query);
        } else {
            if (dataset.getPlatform()
                       .isMobile()) {
                addGeometry(observation, value, query);
            }
        }
        return value;
    }

    protected void addGeometry(final DataEntity< ? > dataEntity, final AbstractValue< ? > value, final DbQuery query) {
        if (dataEntity.isSetGeometryEntity()) {
            final GeometryEntity geometryEntity = dataEntity.getGeometryEntity();
            final Geometry geometry = getGeometry(geometryEntity, query);
            value.setGeometry(geometry);
        }
    }

    protected Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
        return geometryEntity != null
            ? geometryEntity.getGeometry(query.getGeometryFactory())
            : null;
    }

    protected void addValidTime(final DataEntity< ? > observation, final AbstractValue< ? > value) {
        if (observation.isSetValidStartTime() || observation.isSetValidEndTime()) {
            final Long validFrom = observation.isSetValidStartTime()
                ? observation.getValidTimeStart()
                             .getTime()
                : null;
            final Long validUntil = observation.isSetValidEndTime()
                ? observation.getValidTimeEnd()
                             .getTime()
                : null;
            value.setValidTime(new ValidTime(validFrom, validUntil));
        }
    }

    protected void addResultTime(final DataEntity< ? > observation, final AbstractValue< ? > value) {
        if (observation.getResultTime() != null) {
            value.setResultTime(observation.getResultTime()
                                           .getTime());
        }
    }

    protected void addParameters(final DataEntity< ? > observation,
                                 final AbstractValue< ? > value,
                                 final DbQuery query) {
        if (observation.hasParameters()) {
            for (final Parameter< ? > parameter : observation.getParameters()) {
                value.addParameter(parameter.toValueMap(query.getLocale()));
            }
        }
    }

}
