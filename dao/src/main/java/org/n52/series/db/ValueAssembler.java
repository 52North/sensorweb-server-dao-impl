
package org.n52.series.db;

import java.util.Collections;
import java.util.List;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.old.dao.DbQuery;

public interface ValueAssembler<S extends DatasetEntity, E extends DataEntity<T>, V extends AbstractValue< ? >, T> {

    /**
     * Assembles observation values as {@link Data} output.
     *
     * @param id
     *        the dataset id
     * @param query
     *        the query
     * @return the assembled data
     */
    Data<V> getData(String id, DbQuery query);

    /**
     * Assembles a list of reference values.
     *
     * @param datasetEntity
     *        the dataset
     * @param query
     *        the query
     * @return a list of reference values
     */
    default List<ReferenceValueOutput<V>> getReferenceValues(S datasetEntity, DbQuery query) {
        return Collections.emptyList();
    }

    /**
     * Assembles an output for a data entity containing all metadata (geometry, parameters, valid time, etc.)
     * for a given query.
     *
     * @param dataEntity
     *        the single data entity to assemble
     * @param datasetEntity
     *        the dataset the data entity belongs to
     * @param query
     *        the query
     * @return the assembled output
     */
    V assembleDataValueWithMetadata(E dataEntity, S datasetEntity, DbQuery query);

    /**
     * Assembles an output for a data entity for a given query.
     *
     * @param dataEntity
     *        the single data entity to assemble
     * @param datasetEntity
     *        the dataset the data entity belongs to
     * @param query
     *        the query
     * @return the assembled output
     */
    V assembleDataValue(E dataEntity, S datasetEntity, DbQuery query);

    // /**
    // * Assembles plain output containing date/time and the as-is value for a given query.
    // *
    // * @param value
    // * the value
    // * @param data
    // * the data entity
    // * @param query
    // * the query
    // * @return the assembled output
    // */
    // V assembleDataValue(T value, E data, DbQuery query);

    /**
     * @param entity
     *        the dataset entity
     * @param query
     *        the query
     * @return the first value for the given dataset
     */
    V getFirstValue(S entity, DbQuery query);

    /**
     * @param entity
     *        the dataset entity
     * @param query
     *        the query
     * @return the last value for the given dataset
     */
    V getLastValue(S entity, DbQuery query);

    /**
     * Finds the closest value before a given timespan.
     *
     * @param dataset
     *        the dataset
     * @param query
     *        the query containing the timespan
     * @return the closest value before a given timespan
     */
    E getClosestValueBeforeStart(S dataset, DbQuery query);

    /**
     * Finds the closest value after a given timespan.
     *
     * @param dataset
     *        the dataset
     * @param query
     *        the query containing the timespan
     * @return the closest value after a given timespan
     */
    E getClosestValueAfterEnd(S dataset, DbQuery query);

}
