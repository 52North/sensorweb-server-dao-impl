/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.sensorweb.server.db.old.dao;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MoreRestrictions;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.CriteriaImpl.Subcriteria;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.locationtech.jts.geom.Geometry;
import org.n52.series.db.beans.CategoryDataEntity;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.shetland.ogc.filter.BinaryLogicFilter;
import org.n52.shetland.ogc.filter.ComparisonFilter;
import org.n52.shetland.ogc.filter.Filter;
import org.n52.shetland.ogc.filter.FilterConstants.ComparisonOperator;
import org.n52.shetland.ogc.filter.FilterConstants.SpatialOperator;
import org.n52.shetland.ogc.filter.FilterConstants.TimeOperator;
import org.n52.shetland.ogc.filter.Filters;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.filter.UnaryLogicFilter;
import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.gml.time.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class FESCriterionGenerator {

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
    private final boolean complexParent;
    private final Criteria criteria;
    private final Set<String> aliases = new HashSet<>();

    /**
     * Creates a new {@code FESCriterionGenerator}.
     *
     * @param criteria
     *        the criteria
     * @param unsupportedIsTrue
     *        if the generator encounters a filter expression it could not translate it may generate a
     *        criterion that is always {@code true} or always {@code false} depending on this flag
     * @param matchDomainIds
     *        if filter on observation parameters like feature, offering or procedure should match on
     *        their respective domain identifiers or on the primary keys in the database
     * @param complexParent
     *        if the queries should result in the parent observation and hide the child observations
     */
    public FESCriterionGenerator(Criteria criteria,
                                 boolean unsupportedIsTrue,
                                 boolean matchDomainIds,
                                 boolean complexParent) {
        this.criteria = Objects.requireNonNull(criteria);
        this.unsupportedIsTrue = unsupportedIsTrue;
        this.matchDomainIds = matchDomainIds;
        this.complexParent = complexParent;
    }

    /**
     * Get the criteria this generator creates criterions for.
     *
     * @return the criteria
     */
    protected Criteria getCriteria() {
        return this.criteria;
    }

    /**
     * If the generator encounters a filter expression it could not translate it may generate a criterion that
     * is always
     * {@code true} or always {@code false} depending on this flag
     *
     * @return the flag
     */
    protected boolean isUnsupportedIsTrue() {
        return this.unsupportedIsTrue;
    }

    /**
     * Ff filter on observation parameters like feature, offering or procedure should match on their
     * respective domain
     * identifiers or on the primary keys in the database
     *
     * @return if domain identifiers should be matched
     */
    protected boolean isMatchDomainIds() {
        return this.matchDomainIds;
    }

    /**
     * If the queries should result in the parent observation and hide the child observations.
     *
     * @return if the queries should result in the parent observation and hide the child observations
     */
    protected boolean isComplexParent() {
        return this.complexParent;
    }

    /**
     * Add a alias for the specified property to the criteria.
     *
     * @param property
     *        the property
     * @return the alias
     */
    protected String addAlias(String property) {
        Iterator<Subcriteria> subcriteria = ((CriteriaImpl) this.criteria).iterateSubcriteria();
        while (subcriteria.hasNext()) {
            Subcriteria sc = subcriteria.next();
            if (sc.getPath()
                  .equals(property)) {
                return sc.getAlias();
            }
        }
        String alias = "odf_" + property;
        if (!this.aliases.contains(alias)) {
            this.criteria.createAlias(property, alias);
            this.aliases.add(alias);
        }
        return alias;
    }

    /**
     * Create a {@code Criterion} for the supplied {@code Filter}.
     *
     * @param filter
     *        the filter
     * @return the criterion
     */
    public Criterion create(Filter<?> filter) {

        Filter<?> f = simplify(filter);

        if (f instanceof ComparisonFilter) {
            return createComparisonCriterion((ComparisonFilter) f);
        } else if (f instanceof BinaryLogicFilter) {
            return createBinaryLogicCriterion((BinaryLogicFilter) f);
        } else if (f instanceof UnaryLogicFilter) {
            return createUnaryLogicCriterion((UnaryLogicFilter) f);
        } else if (f instanceof SpatialFilter) {
            return createSpatialCriterion((SpatialFilter) f);
        } else if (f instanceof TemporalFilter) {
            return createTemporalCriterion((TemporalFilter) f);
        } else if (f == null) {
            return MoreRestrictions.alwaysTrue();
        } else {
            return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for the supplied {@code UnaryLogicFilter}.
     *
     * @param filter
     *        the filter
     * @return the criterion
     */
    private Criterion createUnaryLogicCriterion(UnaryLogicFilter filter) {
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
     * @param filter
     *        the filter
     * @return the criterion
     */
    private Criterion createBinaryLogicCriterion(BinaryLogicFilter filter) {
        Stream<Criterion> predicates = filter.getFilterPredicates().stream().map(this::create);
        switch (filter.getOperator()) {
            case And:
                return predicates.collect(MoreRestrictions.toConjunction());
            case Or:
                return predicates.collect(MoreRestrictions.toDisjunction());
            default:
                return unsupported(filter);
        }
    }

    /**
     * Create a {@code Criterion} for the supplied {@code ComparisonFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createComparisonCriterion(ComparisonFilter filter) {
        switch (filter.getValueReference()) {
            case VR_RESULT:
                return createResultCriterion(filter);
            case VR_PHENOMENON_TIME:
                return createPhenomenonTimeCriterion(filter);
            case VR_RESULT_TIME:
                return createResultTimeCriterion(filter);
            case VR_PROCEDURE:
                return createProcedureCriterion(filter);
            case VR_FEATURE:
                return createFeatureCriterion(filter);
            case VR_OFFERING:
                return createOfferingCriterion(filter);
            case VR_PHENOMENON:
                return createPhenomenonCriterion(filter);
            case VR_VALID_TIME:
            case VR_IDENTIFIER:
            case VR_FEATURE_SHAPE:
            case VR_SAMPLING_GEOMETRY:
            default:
                return unsupported(filter);
        }
    }

    /**
     * Create a spatial filter criterion for the supplied filter.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    protected Criterion createSpatialFilterCriterion(SpatialFilter filter) {
        Geometry geom = filter.getGeometry().toGeometry();
        return createSpatialFilterCriterion(filter.getOperator(), filter.getValueReference(), geom);
    }

    /**
     * Create a spatial filter criterion for the supplied operator, property and geometry.
     *
     * @param operator the spatial operator
     * @param property the property to apply the filter to
     * @param geom     the geometry
     *
     * @return the criterion
     */
    private Criterion createSpatialFilterCriterion(SpatialOperator operator, String property, Geometry geom) {
        if (geom.isEmpty()) {
            return SpatialRestrictions.isEmpty(property);
        }

        switch (operator) {
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
                return unsupported(operator);
        }
    }

    /**
     * Create a {@code Criterion} for the supplied operator, property and value.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    protected Criterion createComparison(ComparisonFilter filter) {
        return createComparison(filter, null);
    }

    /**
     * Create a {@code Criterion} for the supplied filter. {@code value} may hold a typed value to be used instead of
     * the filter's string value.
     *
     * @param filter the filter
     * @param value  a optional object to be used instead of the filter's string value
     *
     * @return the criterion
     */
    protected Criterion createComparison(ComparisonFilter filter, Object value) {
        Object v = Optional.ofNullable(value).orElseGet(filter::getValue);

        switch (filter.getOperator()) {
            case PropertyIsEqualTo:
                return Restrictions.eq(filter.getValueReference(), v);
            case PropertyIsGreaterThan:
                return Restrictions.gt(filter.getValueReference(), v);
            case PropertyIsGreaterThanOrEqualTo:
                return Restrictions.ge(filter.getValueReference(), v);
            case PropertyIsLessThan:
                return Restrictions.lt(filter.getValueReference(), v);
            case PropertyIsLessThanOrEqualTo:
                return Restrictions.le(filter.getValueReference(), v);
            case PropertyIsLike:
                if (!(v instanceof String)) {
                    throw new Error("Could not apply PropertyIsLike to non string value");
                }
                filter.setValue((String) v);
                return createLike(filter);
            case PropertyIsNull:
            case PropertyIsNil:
                return Restrictions.isNull(filter.getValueReference());
            case PropertyIsNotEqualTo:
                return Restrictions.ne(filter.getValueReference(), v);
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
    private Criterion createTemporalCriterion(TemporalFilter filter) {
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
     * @param operator the temporal relation type
     * @param property the column holding the time
     * @param time     the time instant or period to compare against
     *
     * @return the criterion
     */
    private Criterion createTemporalCriterion(TimeOperator operator, String property, Time time) {
        return createTemporalCriterion(operator, property, property, time);
    }

    /**
     * Create a {@code Criterion} for the specified temporal relation.
     *
     * @param operator the temporal relation type
     * @param start    the column holding the start time
     * @param end      the column holding the end time
     * @param time     the time instant or period to compare against
     *
     * @return the criterion
     */
    private Criterion createTemporalCriterion(TimeOperator operator, String start, String end, Time time) {
        switch (operator) {
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
                return unsupported(operator);
        }
    }

    /**
     * Create a {@code Criterion} for the specified comparison filter. The respective time value is parsed from the
     * filter's string value.
     *
     * @param filter   the comparison filter
     * @param property the column holding the time
     *
     * @return the criterion
     */
    private Criterion createTemporalCriterion(ComparisonFilter filter, String property) {
        return createTemporalCriterion(filter, property, property);
    }

    /**
     * Create a {@code Criterion} for the specified comparison filter. The respective time value is parsed from the
     * filter's string value.
     *
     * @param filter the comparison filter
     * @param start  the column holding the start time
     * @param end    the column holding the end time
     *
     * @return the criterion
     */
    private Criterion createTemporalCriterion(ComparisonFilter filter, String start, String end) {
        Time time;
        try {
            time = parseTime(filter.getValue());
        } catch (IllegalArgumentException ex) {
            return unparsableTime(filter.getValue(), ex);
        }

        switch (filter.getOperator()) {
            case PropertyIsBetween:
                if (time instanceof TimePeriod) {
                    return unsupported(filter);
                }
                Time time2;
                try {
                    time2 = parseTime(filter.getValueUpper());
                } catch (IllegalArgumentException ex) {
                    return unparsableTime(filter.getValueUpper(), ex);
                }
                if (time2 instanceof TimePeriod) {
                    return unsupported(filter);
                }
                TimePeriod period = new TimePeriod(time, time2);
                return createDuringEquals(period, start, end);
            case PropertyIsEqualTo:
                return createEquals(time, start, end);
            case PropertyIsGreaterThan:
                return createAfter(time, start, end);
            case PropertyIsGreaterThanOrEqualTo:
                return createAfterEquals(time, start, end);
            case PropertyIsLessThan:
                return createBefore(time, start, end);
            case PropertyIsLessThanOrEqualTo:
                return createBeforeEquals(time, start, end);
            case PropertyIsNotEqualTo:
                return Restrictions.not(createEquals(time, start, end));
            case PropertyIsNil:
                // time phenomenon time is never nil
                return MoreRestrictions.alwaysFalse();
            case PropertyIsNull:
                // time phenomenon time is never null
                return MoreRestrictions.alwaysFalse();
            case PropertyIsLike:
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
     * Creates a criterion for an unsupported filter.
     *
     * @param filter the filter
     *
     * @return the restriction
     */
    protected Criterion unsupported(Filter<?> filter) {
        LOG.warn("Unsupported filter: {}", filter);
        return isUnsupportedIsTrue() ? MoreRestrictions.alwaysTrue() : MoreRestrictions.alwaysFalse();
    }

    /**
     * Creates a criterion for an unsupported filter.
     *
     * @param filter the filter
     *
     * @return the restriction
     */
    protected Criterion unsupported(Enum<?> filter) {
        LOG.warn("Unsupported operator: {}", filter);
        return isUnsupportedIsTrue() ? MoreRestrictions.alwaysTrue() : MoreRestrictions.alwaysFalse();
    }

    /**
     * Creates a criterion for an unsupported filter.
     *
     * @param o the unsupported object
     *
     * @return the restriction
     */
    protected Criterion unsupported(Object o) {
        LOG.warn("Unsupported: {}", o);
        return isUnsupportedIsTrue() ? MoreRestrictions.alwaysTrue() : MoreRestrictions.alwaysFalse();
    }

    /**
     * Creates a criterion for an unparsable date time value.
     *
     * @param value the value
     * @param ex    the causing exception
     *
     * @return the criterion
     */
    protected Criterion unparsableTime(String value, IllegalArgumentException ex) {
        LOG.warn("Could not parse time value " + value, ex);
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
        return Filters.and(Filters.ge(valueReference, lower),
                           Filters.le(valueReference, upper));
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
     * Create a {@code Criterion} for the {@code AfterEquals} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createAfterEquals(Time time, String start, String end) {
        DateTime dt;
        if (time instanceof TimeInstant) {
            dt = ((TimeInstant) time).getValue();
        } else if (time instanceof TimePeriod) {
            dt = ((TimePeriod) time).getEnd();
        } else {
            return unsupported(time);
        }
        return Restrictions.ge(end, dt.toDate());
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
     * Create a {@code Criterion} for the {@code BeforeEquals} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createBeforeEquals(Time time, String start, String end) {
        DateTime dt;
        if (time instanceof TimeInstant) {
            dt = ((TimeInstant) time).getValue();
        } else if (time instanceof TimePeriod) {
            dt = ((TimePeriod) time).getStart();
        } else {
            return unsupported(time);
        }
        return Restrictions.le(start, dt.toDate());
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
     * Create a {@code Criterion} for the {@code ContainsEquals} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createContainsEquals(Time time, String start, String end) {
        if (time instanceof TimeInstant) {
            Date date = ((TimeInstant) time).getValue().toDate();
            return Restrictions.and(Restrictions.le(start, date),
                                    Restrictions.ge(end, date));
        } else if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.le(start, period.getStart().toDate()),
                                    Restrictions.ge(end, period.getEnd().toDate()));
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
     * Create a {@code Criterion} for the {@code DuringEquals} relation.
     *
     * @param time  the time to compare against
     * @param start the property holding the start time
     * @param end   the property holding the end time
     *
     * @return the criterion
     */
    private Criterion createDuringEquals(Time time, String start, String end) {
        if (time instanceof TimePeriod) {
            TimePeriod period = (TimePeriod) time;
            return Restrictions.and(Restrictions.ge(start, period.getStart().toDate()),
                                    Restrictions.le(end, period.getEnd().toDate()));
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
     * Create a {@code Criterion} for the specified {@code PropertyIsLike} comparison filter.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createLike(ComparisonFilter filter) {

        String escapeString = "\\";
        String filterEscapeString = filter.getEscapeString();
        if (filter.isSetEscapeString()) {
            if (filterEscapeString.length() != 1) {
                String escapeStringRegex = "\\\\";
                filter.setValue(filter.getValue().replaceAll(escapeStringRegex, escapeString + escapeString));
                filter.setValue(filter.getValue().replaceAll(Pattern.quote(filterEscapeString), escapeString));
                filter.setEscapeString(escapeString);
            }
        } else {
            filter.setEscapeString(escapeString);
        }

        if (filter.isSetSingleChar()) {
            String underscore = "_";
            if (!filter.getSingleChar().equals(underscore)) {
                filter.setValue(filter.getValue().replaceAll(underscore, filterEscapeString + underscore));
                filter.setValue(filter.getValue().replaceAll(Pattern.quote(filter.getSingleChar()), underscore));
            }
        }

        if (filter.isSetWildCard()) {
            String stringWildcard = "%";
            if (!filter.getWildCard().equals(stringWildcard)) {
                filter.setValue(filter.getValue().replaceAll(stringWildcard, filterEscapeString + stringWildcard));
                filter.setValue(filter.getValue().replaceAll(Pattern.quote(filter.getWildCard()), stringWildcard));
            }
        }
        return MoreRestrictions.like(filter.getValueReference(),
                                     filter.getValue(),
                                     filterEscapeString,
                                     !filter.isMatchCase());
    }

    /**
     * Create a new {@code Criterion} for the procedure.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createProcedureCriterion(ComparisonFilter filter) {
        return createDatasetCriterion(DatasetEntity.PROPERTY_PROCEDURE, filter);
    }

    /**
     * Create a new {@code Criterion} for the feature.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createFeatureCriterion(ComparisonFilter filter) {
        return createDatasetCriterion(DatasetEntity.PROPERTY_FEATURE, filter);
    }

    /**
     * Create a new {@code Criterion} for the offering.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createOfferingCriterion(ComparisonFilter filter) {
        return createDatasetCriterion(DatasetEntity.PROPERTY_OFFERING, filter);
    }

    /**
     * Create a new {@code Criterion} for the phenomenon.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createPhenomenonCriterion(ComparisonFilter filter) {
        return createDatasetCriterion(DatasetEntity.PROPERTY_PHENOMENON, filter);
    }

    /**
     * Create a {@code Criterion} for the supplied {@code SpatialFilter}.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createSpatialCriterion(SpatialFilter filter) {
        switch (filter.getValueReference()) {
            case VR_SAMPLING_GEOMETRY:
                return createSamplingGeometryFilter(filter);
            case VR_FEATURE_SHAPE:
                return createFeatureOfInterestFilter(filter);
            default:
                return unsupported(filter);
        }

    }

    /**
     * Creates a new {@code Criterion} for the phenomenon time.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createPhenomenonTimeCriterion(ComparisonFilter filter) {
        return createDataCriterion(createTemporalCriterion(filter,
                                                           DataEntity.PROPERTY_SAMPLING_TIME_START,
                                                           DataEntity.PROPERTY_SAMPLING_TIME_END));
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
        return createDataCriterion(createTemporalCriterion(operator,
                                                           DataEntity.PROPERTY_SAMPLING_TIME_START,
                                                           DataEntity.PROPERTY_SAMPLING_TIME_END,
                                                           time));
    }

    /**
     * Create a stream of subqueries that apply the comparison filter to the result value of the data entity. No
     * projection is applied to the subqueries, this should be done in the calling method.
     *
     * @param filter the filter
     *
     * @return a stream of subqueries for the {@linkplain DataEntity data entity}
     */
    protected Stream<DetachedCriteria> getResultSubqueries(ComparisonFilter filter) {
        filter.setValueReference(DataEntity.PROPERTY_VALUE);
        //        Optional<DetachedCriteria> count = parseLong(filter.getValue())
        Optional<DetachedCriteria> count = parseInt(filter.getValue())
                // we can't apply PropertyIsLike to count values
                .filter(v -> filter.getOperator() != ComparisonOperator.PropertyIsLike)
                .map(lv -> DetachedCriteria.forClass(CountDataEntity.class)
                .add(createComparison(filter, lv)));
        /* TODO uncomment when BooleanDataEntity exists
        Optional<DetachedCriteria> bool = parseBoolean(filter.getValue())
                // we can't apply PropertyIsLike to boolean values
                .filter(v -> filter.getOperator() != ComparisonOperator.PropertyIsLike)
                .map(lv -> DetachedCriteria.forClass(BooleanDataEntity.class)
                .add(createComparison(filter, lv)));
        */
        Optional<DetachedCriteria> quantity = parseBigDecimal(filter.getValue())
                // we can't apply PropertyIsLike to numeric values
                .filter(v -> filter.getOperator() != ComparisonOperator.PropertyIsLike)
                .map(dv -> DetachedCriteria.forClass(QuantityDataEntity.class)
                .add(createComparison(filter, dv)));

        Optional<DetachedCriteria> text = Optional.of(DetachedCriteria.forClass(TextDataEntity.class)
                .add(createComparison(filter)));
        Optional<DetachedCriteria> category = Optional.of(DetachedCriteria.forClass(CategoryDataEntity.class)
                .add(createComparison(filter)));
        // subqueries resulting in the ids of matching observations
        List<DetachedCriteria> subqueries = Stream.of(count, quantity, text, category/*, bool*/)
                .filter(Optional::isPresent).map(Optional::get)
                .map(q -> q.add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE)))
                .collect(toList());

        if (!isComplexParent()) {

            // we are not returning top-level observations but the children,
            // no need to query profile observation
            return subqueries.stream();
        }

        // we are only returning top-level observations
        DetachedCriteria profile = DetachedCriteria.forClass(ProfileDataEntity.class)
                .add(Restrictions.isNotNull(DataEntity.PROPERTY_PARENT))
                .add(Restrictions.eq(DataEntity.PROPERTY_DELETED, Boolean.FALSE))
                .add(subqueries.stream()
                        // restrict the subqueries to observations that are children
                        .map(q -> q.add(Restrictions.isNull(DataEntity.PROPERTY_PARENT)))
                        // just get the PKID
                        .map(q -> q.setProjection(Projections.property(DataEntity.PROPERTY_ID)))
                        // create a property IN expression for each query
                        .map(q -> Subqueries.propertyIn(DataEntity.PROPERTY_ID, q))
                        // and wrap everything into a disjunction
                        .collect(MoreRestrictions.toDisjunction()));

        // restrict subqueries to observations that are not children
        Stream<DetachedCriteria> topLevelPrimitives = subqueries.stream()
                .map(q -> q.add(Restrictions.isNotNull(DataEntity.PROPERTY_PARENT)));

        return Stream.concat(Stream.of(profile), topLevelPrimitives);
    }

    /**
     * Creates a spatial filter criterion for the sampling geometry.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createSamplingGeometryFilter(SpatialFilter filter) {
        filter.setValueReference(QueryUtils.createAssociation(DataEntity.PROPERTY_GEOMETRY_ENTITY,
                                                              GeometryEntity.PROPERTY_GEOMETRY));
        return createDataCriterion(createSpatialFilterCriterion(filter));
    }

    /**
     * Creates a spatial filter criterion for the geometry of the feature.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createFeatureOfInterestFilter(SpatialFilter filter) {
        filter.setValueReference(QueryUtils.createAssociation(FeatureEntity.PROPERTY_GEOMETRY_ENTITY,
                                                              GeometryEntity.PROPERTY_GEOMETRY));
        return createDatasetCriterion(DatasetEntity.PROPERTY_FEATURE, filter);
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
        return createDataCriterion(createTemporalCriterion(operator, DataEntity.PROPERTY_RESULT_TIME, time));
    }

    /**
     * Creates a new {@code Criterion} for the result time.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    private Criterion createResultTimeCriterion(ComparisonFilter filter) {
        return createDataCriterion(createTemporalCriterion(filter, DataEntity.PROPERTY_RESULT_TIME));
    }

    /**
     * Create a {@code Criterion} for the specified result filter.
     *
     * @param filter the filter
     *
     * @return the criterion
     */
    protected abstract Criterion createResultCriterion(ComparisonFilter filter);

    /**
     * Create a {@code Criterion} for a property of the associated {@linkplain DatasetEntity data set}.
     *
     *
     * @param property the property of the data set to apply the filter to
     * @param filter   the comparison filter
     *
     * @return the criterion
     */
    protected abstract Criterion createDatasetCriterion(String property, ComparisonFilter filter);

    /**
     * Create a {@code Criterion} for a property of the associated {@linkplain DatasetEntity data set}.
     *
     * @param property the property of the data set to apply the filter to
     * @param filter   the spatial filter
     *
     * @return the criterion
     */
    protected abstract Criterion createDatasetCriterion(String property, SpatialFilter filter);

    /**
     * Create a {@code Criterion} for a property of the associated {@linkplain DataEntity data entity}.
     *
     * @param criterion the criterion
     *
     * @return the criterion
     */
    protected abstract Criterion createDataCriterion(Criterion criterion);

    /**
     * Parse the {@code value} either as a {@link TimeInstant} or as a {@link TimePeriod}.
     *
     * @param value the string value
     *
     * @return the time
     *
     * @throws IllegalArgumentException if the {@code value} does not represent a valud time instant or period
     */
    public static Time parseTime(String value) throws IllegalArgumentException {
        try {
            return new TimeInstant(Instant.parse(value));
        } catch (IllegalArgumentException ex1) {
            try {
                return new TimePeriod(Interval.parse(value));
            } catch (IllegalArgumentException ex2) {
                ex2.addSuppressed(ex1);
                throw ex2;
            }
        }
    }

    /**
     * Trys to parse {@code value} as a {@code long}.
     *
     * @param value the value
     *
     * @return the parsed value or {@code Optional.empty()}
     */
    public static Optional<Long> parseLong(String value) {
        return Optional.ofNullable(Longs.tryParse(value));
    }

    /**
     * Trys to parse {@code value} as a {@code int}.
     *
     * @param value the value
     *
     * @return the parsed value or {@code Optional.empty()}
     */
    public static Optional<Integer> parseInt(String value) {
        return Optional.ofNullable(Ints.tryParse(value));
    }

    /**
     * Trys to parse {@code value} as a {@code double}.
     *
     * @param value the value
     *
     * @return the parsed value or {@code Optional.empty()}
     */
    public static Optional<Double> parseDouble(String value) {
        return Optional.ofNullable(Doubles.tryParse(value));
    }

    /**
     * Trys to parse {@code value} as a {@code BigDecimal}.
     *
     * @param value the value
     *
     * @return the parsed value or {@code Optional.empty()}
     */
    public static Optional<BigDecimal> parseBigDecimal(String value) {
        Optional<Double> parseDouble = parseDouble(value);
        return parseDouble.isPresent() ? Optional.of(BigDecimal.valueOf(parseDouble.get())) : Optional.empty();
    }

    /**
     * Trys to parse {@code value} as a {@code boolean}.
     *
     * @param value the value
     *
     * @return the parsed value or {@code Optional.empty()}
     */
    public static Optional<Boolean> parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return Optional.of(Boolean.TRUE);
        } else if (value.equalsIgnoreCase("false")) {
            return Optional.of(Boolean.FALSE);
        } else {
            return Optional.empty();
        }
    }
}
