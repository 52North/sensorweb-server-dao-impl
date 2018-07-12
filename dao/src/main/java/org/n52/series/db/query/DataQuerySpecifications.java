
package org.n52.series.db.query;

import static org.n52.series.db.old.dao.QueryUtils.parseToIds;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.n52.io.request.IoParameters;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDataEntity;
import org.n52.series.db.beans.QGeometryEntity;
import org.n52.series.db.old.dao.DbQuery;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.jpa.impl.JPAQuery;
import com.vividsolutions.jts.geom.Geometry;

public class DataQuerySpecifications extends QuerySpecifications {

    public static DataQuerySpecifications of(final DbQuery query) {
        return new DataQuerySpecifications(query);
    }

    private DataQuerySpecifications(final DbQuery query) {
        super(query);
    }

    /**
     * Matches data entities belonging to a given dataset and applying query via {@link #matchFilters()}
     *
     * @param dataset
     *        the dataset
     * @return a boolean expression
     * @see #matchFilters()
     */
    public BooleanExpression matchFilters(DatasetEntity dataset) {
        QDataEntity dataentity = QDataEntity.dataEntity;
        return dataentity.dataset.id.eq(dataset.getId()).and(matchFilters());
    }

    /**
     * Aggregates following filters in an {@literal AND} expression:
     * <ul>
     * <li>{@link #matchDatasets()}</li>
     * <li>{@link #matchTimespan()}</li>
     * <li>{@link #matchParents()}</li>
     * <li>{@link #matchesSpatially()}</li>
     * </ul>
     *
     * @return a boolean expression matching all filter criteria
     */
    public BooleanExpression matchFilters() {
        return matchDatasets().and(matchTimespan())
                              .and(matchParents())
                              .and(matchesSpatially());
    }

    /**
     * Matches the timespan.
     *
     * @return a boolean expression
     */
    private BooleanExpression matchTimespan() {
        QDataEntity dataentity = QDataEntity.dataEntity;
        Date requestedStart = getTimespanStart();
        Date requestedEnd = getTimespanEnd();

        DateTimePath<Date> timestart = dataentity.samplingTimeStart;
        DateTimePath<Date> timeend = dataentity.samplingTimeEnd;
        BooleanExpression afterStart = timestart.after(requestedStart);
        BooleanExpression beforeEnd = timeend.before(requestedEnd);
        return afterStart.and(beforeEnd);
    }

    /**
     * Matches entities so that {@link DataEntity#isParent()} is {@literal true}.
     *
     * @return a boolean expression
     */
    public BooleanExpression matchParents() {
        QDataEntity dataentity = QDataEntity.dataEntity;
        return dataentity.parent.eq(query.isComplexParent());
    }

    /**
     * Matches data of datasets with given ids.
     *
     * @return a boolean expression
     * @see #matchDatasets(Collection)
     */
    public BooleanExpression matchDatasets() {
        final IoParameters parameters = query.getParameters();
        return matchDatasets(parameters.getDatasets());
    }

    /**
     * Matches data of datasets with given ids.
     *
     * @param ids
     *        the ids to match
     * @return a boolean expression
     * @see #matchDatasets(Collection)
     */
    public BooleanExpression matchDatasets(final String... ids) {
        return ids != null
            ? matchDatasets(Arrays.asList(ids))
            : matchDatasets(Collections.emptyList());
    }

    /**
     * Matches data of datasets with given ids. For example:
     *
     * <pre>
     *  where dataset.id in (&lt;ids&gt;)
     * </pre>
     *
     * In case of {@link DbQuery#isMatchDomainIds()} returns {@literal true} the following query path will be
     * used:
     *
     * <pre>
     *  where dataset.identifier in (&lt;ids&gt;)
     * </pre>
     *
     * @param ids
     *        the ids to match
     * @return a boolean expression or {@literal null} when given ids are {@literal null} or empty
     */
    public BooleanExpression matchDatasets(final Collection<String> ids) {
        if ((ids == null) || ids.isEmpty()) {
            return null;
        }
        final QDataEntity data = QDataEntity.dataEntity;
        return query.isMatchDomainIds()
            ? data.dataset.identifier.in(ids)
            : data.dataset.id.in(parseToIds(ids));
    }

    public Optional<DataEntity< ? >> matchClosestBeforeStart(DatasetEntity dataset, EntityManager entityManager) {
        QDataEntity dataentity = QDataEntity.dataEntity;
        OrderSpecifier<Date> order = new OrderSpecifier<>(Order.DESC, dataentity.samplingTimeEnd);
        BooleanExpression beforeSamplingTime = dataentity.samplingTimeStart.before(getTimespanStart());
        BooleanExpression matchesDatasetId = matchDatasets(dataset.getId().toString());
        BooleanExpression predicate = matchesDatasetId.and(beforeSamplingTime);
        JPAQuery<DataEntity< ? >> query = new JPAQuery<>(entityManager);
        return query.from(dataentity)
                    .where(predicate)
                    .orderBy(order)
                    .limit(1)
                    .fetch()
                    .stream()
                    .findFirst();
    }

    public Optional<DataEntity< ? >> matchClosestAfterEnd(DatasetEntity dataset, EntityManager entityManager) {
        QDataEntity dataentity = QDataEntity.dataEntity;
        OrderSpecifier<Date> order = new OrderSpecifier<>(Order.ASC, dataentity.samplingTimeEnd);
        BooleanExpression afterSamplingTime = dataentity.samplingTimeEnd.after(getTimespanEnd());
        BooleanExpression matchesDatasetId = matchDatasets(dataset.getId().toString());
        BooleanExpression predicate = matchesDatasetId.and(afterSamplingTime);
        JPAQuery<DataEntity< ? >> query = new JPAQuery<>(entityManager);
        return query.from(dataentity)
                    .where(predicate)
                    .orderBy(order)
                    .limit(1)
                    .fetch()
                    .stream()
                    .findFirst();
    }

    /**
     * Matches datasets which have a feature laying within the given bbox using an intersects query. For
     * example:
     *
     * <pre>
     *   where ST_INTERSECTS(feature.geom, &lt;filter_geometry&gt;)=1
     * </pre>
     *
     * @return a boolean expression or {@literal null} when given spatial filter is {@literal null} or empty
     */
    public BooleanExpression matchesSpatially() {
        final Geometry geometry = query.getSpatialFilter();
        if ((geometry == null) || geometry.isEmpty()) {
            return null;
        }
        final QDataEntity dataentity = QDataEntity.dataEntity;
        final QGeometryEntity geometryEntity = dataentity.geometryEntity;
        return geometryEntity.geometry.intersects(geometry);

    }

}
