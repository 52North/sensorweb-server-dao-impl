package org.n52.series.db.dao;

import java.util.Optional;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MoreRestrictions;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.shetland.ogc.filter.ComparisonFilter;
import org.n52.shetland.ogc.filter.Filter;
import org.n52.shetland.ogc.filter.SpatialFilter;

/**
 * Class to create a {@linkplain Criterion criterion} for {@linkplain DatasetEntity datasets} from an FES
 * {@linkplain Filter filter}.
 *
 * @author Christian Autermann
 */
public class DatasetFESCriterionGenerator extends FESCriterionGenerator {

    /**
     * Creates a new {@code DatasetFESCriterionGenerator}.
     *
     * @param criteria          the criteria
     * @param unsupportedIsTrue if the generator encounters a filter expression it could not translate it may generate a
     *                          criterion that is always {@code true} or always {@code false} depending on this flag
     * @param matchDomainIds    if filter on observation parameters like feature, offering or procedure should match on
     *                          their respective domain identifiers or on the primary keys in the database
     * @param complexParent     if the queries should result in the parent observation and hide the child observations
     */
    public DatasetFESCriterionGenerator(Criteria criteria,
                                        boolean unsupportedIsTrue,
                                        boolean matchDomainIds,
                                        boolean complexParent) {
        super(criteria, unsupportedIsTrue, matchDomainIds, complexParent);
    }

    @Override
    protected Criterion createDataCriterion(Criterion criterion) {
        DetachedCriteria subquery = DetachedCriteria.forClass(DataEntity.class)
                .setProjection(Projections.property(DataEntity.PROPERTY_SERIES_PKID))
                .add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE))
                .add(criterion);
        return Subqueries.propertyIn(DatasetEntity.PROPERTY_PKID, subquery);
    }

    @Override
    protected Criterion createDatasetCriterion(String property, ComparisonFilter filter) {
        Object value;
        if (isMatchDomainIds()) {
            filter.setValueReference(DescribableEntity.PROPERTY_DOMAIN_ID);
            value = filter.getValue();
        } else {
            filter.setValueReference(DescribableEntity.PROPERTY_PKID);
            Optional<Long> id = parseLong(filter.getValue());
            if (!id.isPresent()) {
                return unsupported(filter);
            }
            value = id.get();
        }
        String alias = addAlias(property);
        filter.setValueReference(QueryUtils.createAssociation(alias, filter.getValueReference()));
        return createComparison(filter, value);
    }

    @Override
    protected Criterion createDatasetCriterion(String property, SpatialFilter filter) {
        String alias = addAlias(property);
        filter.setValueReference(QueryUtils.createAssociation(alias, filter.getValueReference()));
        return createSpatialFilterCriterion(filter);
    }

    @Override
    protected Criterion createResultCriterion(ComparisonFilter filter) {
        return getResultSubqueries(filter)
                // just get the dataset PKID from the data entities
                .map(q -> q.setProjection(Projections.property(DataEntity.PROPERTY_SERIES_PKID)))
                // create a property IN expression for each query
                .map(q -> Subqueries.propertyIn(DatasetEntity.PROPERTY_PKID, q))
                // and wrap everything into a disjunction
                .collect(MoreRestrictions.toDisjunction());
    }



}
