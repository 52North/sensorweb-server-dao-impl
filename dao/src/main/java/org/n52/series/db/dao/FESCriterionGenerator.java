package org.n52.series.db.dao;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.shetland.ogc.filter.BinaryLogicFilter;
import org.n52.shetland.ogc.filter.ComparisonFilter;
import org.n52.shetland.ogc.filter.Filter;
import org.n52.shetland.ogc.filter.FilterConstants.ComparisonOperator;
import org.n52.shetland.ogc.filter.FilterConstants.TimeOperator;
import org.n52.shetland.ogc.filter.Filters;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.filter.UnaryLogicFilter;
import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.gml.time.TimePeriod;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Class to create a {@linkplain Criterion criterion} from an FES {@linkplain Filter filter}.
 *
 * @author Christian Autermann
 */
public class FESCriterionGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(FESCriterionGenerator.class);
    private static final String VR_PROCEDURE = "om:procedure";
    private static final String VR_FEATURE = "om:featureOfInterest";
    private static final String VR_OFFERING = "sos:offering";
    private static final String VR_PHENOMENON = "om:observedProperty";
    private static final String VR_VALID_TIME = "om:validTime";
    private static final String VR_IDENTIFIER = "gml:identifier";
    private static final String VR_FEATURE_SHAPE = "om:featureOfInterest/*/sams:shape";
    private static final String VR_SAMPLING_GEOMETRY = "http://www.opengis.net/req/omxml/2.0/data/samplingGeometry";
    private static final String VR_PHENOMENON_TIME = "om:phenomenonTime";
    private static final String VR_RESULT_TIME = "om:resultTime";
    private static final String VR_RESULT = "om:result";
    private final boolean unsupportedIsTrue;
    private final boolean matchDomainIds;

    /**
     * Creates a new {@code FESCriterionGenerator}.
     *
     * @param unsupportedIsTrue if the generator encounters a filter expression it could not translate it may generate a
     *                          criterion that is always {@code true} or always {@code false} depending on this flag
     * @param matchDomainIds    if filter on observation parameters like feature, offering or procedure should match on
     *                          their respective domain identifiers or on the primary keys in the database
     */
    public FESCriterionGenerator(boolean unsupportedIsTrue, boolean matchDomainIds) {
        this.unsupportedIsTrue = unsupportedIsTrue;
        this.matchDomainIds = matchDomainIds;
    }

    /**
     * Create a {@code Criterion} for the supplied {@code Filter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    public Criterion create(Filter<?> filter) {

        Filter<?> f = simplify(filter);

        if (f instanceof ComparisonFilter) {
            return create((ComparisonFilter) f);
        } else if (f instanceof BinaryLogicFilter) {
            return create((BinaryLogicFilter) f);
        } else if (f instanceof UnaryLogicFilter) {
            return create((UnaryLogicFilter) f);
        } else if (f instanceof SpatialFilter) {
            return create((SpatialFilter) f);
        } else if (f instanceof TemporalFilter) {
            return create((TemporalFilter) f);
        } else if (f == null) {
            return alwaysTrue();
        } else {
            return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for the supplied {@code UnaryLogicFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion create(UnaryLogicFilter filter) {
        switch (filter.getOperator()) {
            case Not:
                Criterion criterion = create(filter.getFilterPredicate());
                return Restrictions.not(criterion);
            default:
                return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for the supplied {@code BinaryLogicFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion create(BinaryLogicFilter filter) {
        Stream<Criterion> predicates = filter.getFilterPredicates().stream().map(this::create);
        switch (filter.getOperator()) {
            case And:
                return predicates.collect(toConjunction());
            case Or:
                return predicates.collect(toDisjunction());
            default:
                return unsupported(filter);
        }
    }

    /**
     * Simplify the supplied filter.
     *
     * @param filter the filter
     *
     * @return the simplified filter
     */
    private Filter<?> simplify(Filter<?> filter) {
        if (filter instanceof ComparisonFilter) {
            return simplifyComparison((ComparisonFilter) filter);
        }
        return filter;
    }

    /**
     * Create a {@code Criterion} for the supplied {@code ComparisonFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion create(ComparisonFilter filter) {

        ComparisonOperator op = filter.getOperator();
        String value = filter.getValue();

        switch (filter.getValueReference()) {
            case VR_RESULT:
                return create(op, DataEntity.PROPERTY_VALUE, value);
            case VR_PHENOMENON_TIME:
                return FESCriterionGenerator.this.createPhenomenonTimeCriterion(op, value);
            case VR_RESULT_TIME:
                return createResultTimeCriterion(op, value);
            case VR_PROCEDURE:
                return createProcedureCriterion(op, value);
            case VR_FEATURE:
                return createFeatureCriterion(op, value);
            case VR_OFFERING:
                return createOfferingCriterion(op, value);
            case VR_PHENOMENON:
                return createPhenomenonCriterion(op, value);
            case VR_VALID_TIME:
            case VR_IDENTIFIER:
            case VR_FEATURE_SHAPE:
            case VR_SAMPLING_GEOMETRY:
            default:
                return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for a property of the associated {@linkplain DatasetEntity data set}.
     *
     * @param operator the comparison operator
     * @param property the property of the data set to compare
     * @param value    the value to compare with
     *
     * @return the criterion
     */
    private Criterion createDatasetCriterion(ComparisonOperator operator, String property, Object value) {
        Criterion criterion = create(operator,
                                     matchDomainIds ? DescribableEntity.PROPERTY_DOMAIN_ID
                                             : DescribableEntity.PROPERTY_PKID,
                                     value);

        DetachedCriteria subquery = DetachedCriteria.forClass(DatasetEntity.class)
                .setProjection(Property.forName(DatasetEntity.PROPERTY_PKID))
                .createCriteria(property).add(criterion);

        return Subqueries.propertyIn(DataEntity.PROPERTY_SERIES_PKID, subquery);
    }

    /**
     * Create a {@code Criterion} for the supplied operator, property and value.
     *
     * @param operator the comparison operator
     * @param property the property to compare
     * @param value    the value to compare with
     *
     * @return the criterion
     */
    private Criterion create(ComparisonOperator operator, String property, Object value) {
        switch (operator) {
            case PropertyIsEqualTo:
                return Restrictions.eq(property, value);
            case PropertyIsGreaterThan:
                return Restrictions.gt(property, value);
            case PropertyIsGreaterThanOrEqualTo:
                return Restrictions.ge(property, value);
            case PropertyIsLessThan:
                return Restrictions.lt(property, value);
            case PropertyIsLessThanOrEqualTo:
                return Restrictions.le(property, value);
            case PropertyIsLike:
                return Restrictions.like(property, value);
            case PropertyIsNull:
            case PropertyIsNil:
                return Restrictions.isNull(property);
            case PropertyIsNotEqualTo:
                return Restrictions.ne(property, value);
            default:
                return unsupported(operator);

        }
    }

    /**
     * Create a {@code Criterion} for the supplied {@code SpatialFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion create(SpatialFilter filter) {

        String property = QueryUtils.createAssociation(DataEntity.PROPERTY_GEOMETRY_ENTITY,
                                                       GeometryEntity.PROPERTY_GEOMETRY);
        // TODO remove this once Hibernate supports LocationTech JTS
        Geometry geom = JTSGeometryConverter.convert(filter.getGeometry().toGeometry());

        if (geom.isEmpty()) {
            return unsupported(filter);
        }

        switch (filter.getOperator()) {
            case BBOX:
                return SpatialRestrictions.filter(property, geom);
            case Contains:
                return SpatialRestrictions.contains(property, geom);
            case Crosses:
                return SpatialRestrictions.crosses(property, geom);
            case Disjoint:
                return SpatialRestrictions.disjoint(property, geom);
            case Equals:
                return SpatialRestrictions.eq(property, geom);
            case Intersects:
                return SpatialRestrictions.intersects(property, geom);
            case Overlaps:
                return SpatialRestrictions.overlaps(property, geom);
            case Touches:
                return SpatialRestrictions.touches(property, geom);
            case Within:
                return SpatialRestrictions.within(property, geom);
            case Beyond:
            case DWithin:
            default:
                return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for the supplied {@code TemporalFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion create(TemporalFilter filter) {
        Time time = filter.getTime();

        if (time.isReferenced() ||
            time.isEmpty() ||
            isIndeterminate(time)) {
            return unsupported(filter);
        }
        String valueReference = filter.getValueReference();
        TimeOperator operator = filter.getOperator();
        switch (valueReference) {
            case VR_RESULT_TIME:
                return createResultTimeCriterion(operator, time);
            case VR_PHENOMENON_TIME:
                return createPhenomenonTimeCriterion(operator, time);
            case VR_VALID_TIME:
            default:
                return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for the specified temporal relation.
     *
     * @param op    the temporal relation type
     * @param start the column holding the start time time
     * @param end   the column holding the end time
     * @param time  the time instant or period to compare against
     *
     * @return the criterion
     */
    private Criterion create(TimeOperator op, String start, String end, Time time) {
        switch (op) {
            case TM_After:
                return createAfter(time, start, end);
            case TM_Before:
                return createBefore(time, start, end);
            case TM_Begins:
                return createBegins(time, start, end);
            case TM_BegunBy:
                return createBegunBy(time, start, end);
            case TM_Contains:
                return createContains(time, start, end);
            case TM_During:
                return createDuring(time, start, end);
            case TM_Ends:
                return createEnds(time, start, end);
            case TM_EndedBy:
                return createEndedBy(time, start, end);
            case TM_Equals:
                return createEquals(time, start, end);
            case TM_Meets:
                return createMeets(time, end, start);
            case TM_MetBy:
                return createMetBy(time, start, end);
            case TM_Overlaps:
                return createOverlaps(time, start, end);
            case TM_OverlappedBy:
                return createOverlappedBy(time, start, end);
            default:
                return unsupported(op);
        }
    }

    /**
     * Creates a criterion for an unsupported filter.
     *
     * @param filter the filter
     *
     * @return the restriction
     */
    private Criterion unsupported(Filter<?> filter) {
        LOG.warn("Unsupported filter: {}", filter);
        return unsupportedIsTrue ? alwaysTrue() : alwaysFalse();
    }

    /**
     * Creates a criterion for an unsupported filter.
     *
     * @param filter the filter
     *
     * @return the restriction
     */
    private Criterion unsupported(Enum<?> filter) {
        LOG.warn("Unsupported operator: {}", filter);
        return unsupportedIsTrue ? alwaysTrue() : alwaysFalse();
    }

    /**
     * Creates a criterion for an unsupported filter.
     *
     * @param o the unsupported object
     *
     * @return the restriction
     */
    private Criterion unsupported(Object o) {
        LOG.warn("Unsupported: {}", o);
        return unsupportedIsTrue ? alwaysTrue() : alwaysFalse();
    }

    /**
     * Creates a criterion for an unparsable date time value.
     *
     * @param value the value
     * @param ex    the causing exception
     *
     * @return the criterion
     */
    private Criterion unparsableDateTime(String value, IllegalArgumentException ex) {
        LOG.warn("Could not parse date time " + value, ex);
        return unsupported(value);
    }

    /**
     * Simplifies the comparison filter.
     *
     * @param filter the filter
     *
     * @return the simplified filter
     */
    private Filter<?> simplifyComparison(ComparisonFilter filter) {
        if (filter.getOperator() == ComparisonOperator.PropertyIsBetween) {
            return simplifyPropertyIsBetween(filter);
        }
        return filter;
    }

    /**
     * Transforms a {@code PropertyIsBetween} filter into an {@code PropertyIsGreaterThanOrEqualTo} and
     * {@code PropertyIsLessThanOrEqualTo} filter.
     *
     * @param filter the filter
     *
     * @return the simplified filter
     */
    private Filter<?> simplifyPropertyIsBetween(ComparisonFilter filter) {
        String valueReference = filter.getValueReference();
        String lower = filter.getValue();
        String upper = filter.getValueUpper();
        return Filters.and(Filters.ge(valueReference, lower), Filters.le(valueReference, upper));
    }

    /**
     * Create a {@code Criterion} for the {@code After} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createAfter(Time time, String start, String end) {
        DateTime dt;
        if (time instanceof TimeInstant) {
            dt = ((TimeInstant) time).getValue();
        } else if (time instanceof TimePeriod) {
            dt = ((TimePeriod) time).getEnd();
        } else {
            return unsupported(time);
        }
        return Restrictions.gt(end, dt.toDate());
    }

    /**
     * Create a {@code Criterion} for the {@code Before} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createBefore(Time time, String start, String end) {
        DateTime dt;
        if (time instanceof TimeInstant) {
            dt = ((TimeInstant) time).getValue();
        } else if (time instanceof TimePeriod) {
            dt = ((TimePeriod) time).getStart();
        } else {
            return unsupported(time);
        }
        return Restrictions.lt(start, dt.toDate());
    }

    /**
     * Create a {@code Criterion} for the {@code Begins} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createBegins(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.eq(start, period.getStart().toDate()),
                                    Restrictions.lt(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code BegunBy} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createBegunBy(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.eq(start, period.getStart().toDate()),
                                    Restrictions.gt(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code Contains} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createContains(Time time, String start, String end) {
        if (time instanceof TimeInstant) {
            Date date = ((TimeInstant) time).getValue().toDate();
            return Restrictions.and(Restrictions.lt(start, date),
                                    Restrictions.gt(end, date));
        } else if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.lt(start, period.getStart().toDate()),
                                    Restrictions.gt(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code During} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createDuring(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.gt(start, period.getStart().toDate()),
                                    Restrictions.lt(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code Ends} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createEnds(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.gt(start, period.getStart().toDate()),
                                    Restrictions.eq(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code EndedBy} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createEndedBy(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.lt(start, period.getStart().toDate()),
                                    Restrictions.eq(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code Equals} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createEquals(Time time, String start, String end) {
        if (time instanceof TimeInstant) {
            Date date = ((TimeInstant) time).getValue().toDate();
            return Restrictions.and(Restrictions.eq(start, date),
                                    Restrictions.eq(end, date));
        } else if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.eq(start, period.getStart().toDate()),
                                    Restrictions.eq(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code Meets} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createMeets(Time time, String end, String start) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.eq(end, period.getStart().toDate()),
                                    Restrictions.neProperty(start, end));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code MetBy} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createMetBy(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.eq(start, period.getEnd().toDate()),
                                    Restrictions.neProperty(start, end));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code Overlaps} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createOverlaps(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.lt(start, period.getStart().toDate()),
                                    Restrictions.gt(end, period.getStart().toDate()),
                                    Restrictions.gt(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Create a {@code Criterion} for the {@code OverlappedBy} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createOverlappedBy(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.gt(start, period.getStart().toDate()),
                                    Restrictions.lt(end, period.getStart().toDate()),
                                    Restrictions.gt(end, period.getEnd().toDate()));
        } else {
            return unsupported(time);
        }
    }

    /**
     * Checks if {@code time} contains any indeterminate values.
     *
     * @param time the time
     *
     * @return if it contains any indeterminate values
     */
    private boolean isIndeterminate(Time time) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return period.isSetStartIndeterminateValue() ||
                   period.isSetEndIndeterminateValue();
        } else if (time instanceof TimeInstant) {
            TimeInstant instant = (TimeInstant) time;
            return instant.isSetIndeterminateValue();
        } else {
            return false;
        }
    }

    /**
     * Create a new {@code Criterion} for the procedure.
     *
     * @param operator the filter operator
     * @param value    the filter value
     *
     * @return the criterion
     */
    private Criterion createProcedureCriterion(ComparisonOperator operator, String value) {
        return createDatasetCriterion(operator, DatasetEntity.PROPERTY_PROCEDURE, value);
    }

    /**
     * Create a new {@code Criterion} for the feature.
     *
     * @param operator the filter operator
     * @param value    the filter value
     *
     * @return the criterion
     */
    private Criterion createFeatureCriterion(ComparisonOperator operator, String value) {
        return createDatasetCriterion(operator, DatasetEntity.PROPERTY_FEATURE, value);
    }

    /**
     * Create a new {@code Criterion} for the offering.
     *
     * @param operator the filter operator
     * @param value    the filter value
     *
     * @return the criterion
     */
    private Criterion createOfferingCriterion(ComparisonOperator operator, String value) {
        return createDatasetCriterion(operator, DatasetEntity.PROPERTY_OFFERING, value);
    }

    /**
     * Create a new {@code Criterion} for the phenomenon.
     *
     * @param operator the filter operator
     * @param value    the filter value
     *
     * @return the criterion
     */
    private Criterion createPhenomenonCriterion(ComparisonOperator operator, String value) {
        return createDatasetCriterion(operator, DatasetEntity.PROPERTY_PHENOMENON, value);
    }

    /**
     * Creates a new {@code Criterion} for the result time.
     *
     * @param operator the temporal operator
     * @param value    the filter value
     *
     * @return the criterion
     */
    private Criterion createResultTimeCriterion(ComparisonOperator operator, String value) {
        Date date;
        try {
            date = Instant.parse(value).toDate();
        } catch (IllegalArgumentException ex) {
            return unparsableDateTime(value, ex);
        }
        return create(operator, DataEntity.PROPERTY_RESULTTIME, date);
    }

    /**
     * Creates a new {@code Criterion} for the phenomenon time.
     *
     * @param operator the temporal operator
     * @param value    the filter value
     *
     * @return the criterion
     */
    private Criterion createPhenomenonTimeCriterion(ComparisonOperator operator, String value) {
        Date date;
        try {
            date = Instant.parse(value).toDate();
        } catch (IllegalArgumentException ex) {
            return unparsableDateTime(value, ex);
        }

        return Restrictions.or(create(operator, DataEntity.PROPERTY_TIMESTART, date),
                               create(operator, DataEntity.PROPERTY_TIMEEND, date));
    }

    /**
     * Creates a new {@code Criterion} for the phenomenon time.
     *
     * @param operator the temporal operator
     * @param time     the filter value
     *
     * @return the criterion
     */
    private Criterion createPhenomenonTimeCriterion(TimeOperator operator, Time time) {
        return create(operator, DataEntity.PROPERTY_TIMESTART, DataEntity.PROPERTY_TIMEEND, time);
    }

    /**
     * Creates a new {@code Criterion} for the result time.
     *
     * @param operator the temporal operator
     * @param time     the filter value
     *
     * @return the criterion
     */
    private Criterion createResultTimeCriterion(TimeOperator operator, Time time) {
        return create(operator, DataEntity.PROPERTY_RESULTTIME, DataEntity.PROPERTY_RESULTTIME, time);
    }

    /**
     * Creates a criterion that is always {@code true}.
     *
     * @return the criterion
     */
    private static Criterion alwaysTrue() {
        return Restrictions.sqlRestriction("1=1");
    }

    /**
     * Creates a criterion that is always {@code false}.
     *
     * @return the criterion
     */
    private static Criterion alwaysFalse() {
        return Restrictions.sqlRestriction("0=1");
    }

    /**
     * Create a {@code Collector} that collects criterions to a conjunction.
     *
     * @return the collector
     */
    private static Collector<Criterion, ?, Criterion> toConjunction() {
        return toCriterion(Restrictions::conjunction);
    }

    /**
     * Create a {@code Collector} that collects criterions to a disjunction.
     *
     * @return the collector
     */
    private static Collector<Criterion, ?, Criterion> toDisjunction() {
        return toCriterion(Restrictions::disjunction);
    }

    /**
     * Creates a {@code Collector} that collects criterions to a single criterion.
     *
     * @param finisher the finishing function to a create a single criterion from an criterion array
     *
     * @return the collector
     */
    private static Collector<Criterion, ?, Criterion> toCriterion(Function<Criterion[], Criterion> finisher) {
        return collectingAndThen(collectingAndThen(toSet(), s -> s.stream().toArray(Criterion[]::new)), finisher);
    }

}
