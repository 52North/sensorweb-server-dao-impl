package org.hibernate.criterion;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;

/**
 * Utility functions to ease creation of {@linkplain Criterion restrictions}. The package was chosen to be able to pass
 * escape chars to the {@link LikeExpression}.
 *
 * @author Christian Autermann
 */
public final class MoreRestrictions {
    private static final char DEF_ESC_CHR = '\\';
    private static final String DEF_ESC_STR = "" + DEF_ESC_CHR;

    /**
     * Private utility class constructor.
     */
    private MoreRestrictions() {
    }

    /**
     * Create a new case-insensitive like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param escapeChar   the escape char to use
     *
     * @return the expression
     */
    public static LikeExpression ilike(String propertyName, String value, Character escapeChar) {
        return like(propertyName, value, escapeChar, true);
    }

    /**
     * Create a new case-insensitive like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param escapeString the escape string to use
     *
     * @return the expression
     */
    public static LikeExpression ilike(String propertyName, String value, String escapeString) {
        return like(propertyName, value, escapeString, true);
    }

    /**
     * Create a new like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param escapeChar   the escape char to use
     * @param ignoreCase   if the match should be case-insensitive
     *
     * @return the expression
     */
    public static LikeExpression like(String propertyName, String value, Character escapeChar, boolean ignoreCase) {
        return new LikeExpression(propertyName, value, escapeChar, ignoreCase);
    }

    /**
     * Create a new like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param escapeString the escape string to use
     * @param ignoreCase   if the match should be case-insensitive
     *
     * @return the expression
     */
    public static LikeExpression like(String propertyName, String value, String escapeString, boolean ignoreCase) {
        if (escapeString != null) {
            if (escapeString.length() > 1) {
                return like(propertyName, normalize(escapeString, value), DEF_ESC_CHR, ignoreCase);
            }
            return like(propertyName, value, escapeString.charAt(0), ignoreCase);
        }
        return like(propertyName, value, (Character) null, ignoreCase);
    }

    /**
     * Create a new case-sensitive like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     *
     * @return the expression
     */
    public static LikeExpression like(String propertyName, String value) {
        return new LikeExpression(propertyName, value);
    }

    /**
     * Create a new case-sensitive like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param matchMode    the match mode
     *
     * @return the expression
     */
    public static LikeExpression like(String propertyName, String value, MatchMode matchMode) {
        return new LikeExpression(propertyName, value, matchMode);
    }

    /**
     * Create a new case-insensitive like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param matchMode    the match mode
     * @param escapeChar   the escape char to use
     *
     * @return the expression
     */
    public static LikeExpression ilike(String propertyName, String value, MatchMode matchMode, Character escapeChar) {
        return like(propertyName, value, matchMode, escapeChar, true);
    }

    /**
     * Create a new case-insensitive like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param matchMode    the match mode
     * @param escapeString the escape string to use
     *
     * @return the expression
     */
    public static LikeExpression ilike(String propertyName, String value, MatchMode matchMode, String escapeString) {
        return like(propertyName, value, matchMode, escapeString, true);
    }

    /**
     * Create a new like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param matchMode    the match mode
     * @param escapeString the escape string to use
     * @param ignoreCase   if the match should be case-insensitive
     *
     * @return the expression
     */
    public static LikeExpression like(String propertyName, String value, MatchMode matchMode, String escapeString,
                                      boolean ignoreCase) {
        if (escapeString != null) {
            if (escapeString.length() > 1) {
                return like(propertyName, normalize(escapeString, value), matchMode, DEF_ESC_CHR, ignoreCase);
            }
            return like(propertyName, value, matchMode, escapeString.charAt(0), ignoreCase);
        }
        return like(propertyName, value, matchMode, (Character) null, ignoreCase);
    }

    /**
     * Create a new like expression.
     *
     * @param propertyName the property name
     * @param value        the value to compare against
     * @param matchMode    the match mode
     * @param escapeChar   the escape char to use
     * @param ignoreCase   if the match should be case-insensitive
     *
     * @return the expression
     */
    public static LikeExpression like(String propertyName, String value, MatchMode matchMode, Character escapeChar,
                                      boolean ignoreCase) {
        return new LikeExpression(propertyName, value, matchMode, escapeChar, ignoreCase);
    }

    /**
     * Replaces all occurences of {@code escapeString} with the default escape character.
     *
     * @param escapeString the escape string
     * @param value        the value
     *
     * @return the newly escaped value
     */
    private static String normalize(String escapeString, String value) {
        return Pattern.compile(escapeString, Pattern.LITERAL).matcher(value
                .replaceAll("\\\\", DEF_ESC_STR + DEF_ESC_STR))
                .replaceAll(DEF_ESC_STR);
    }

    /**
     * Creates a criterion that is always {@code false}.
     *
     * @return the criterion
     */
    public static Criterion alwaysFalse() {
        return Restrictions.sqlRestriction("0=1");
    }

    /**
     * Creates a criterion that is always {@code true}.
     *
     * @return the criterion
     */
    public static Criterion alwaysTrue() {
        return Restrictions.sqlRestriction("1=1");
    }

    /**
     * Create a {@code Collector} that collects criterions to a disjunction.
     *
     * @return the collector
     */
    public static Collector<Criterion, ?, Criterion> toDisjunction() {
        return toCriterion(Restrictions::disjunction);
    }

    /**
     * Create a {@code Collector} that collects criterions to a conjunction.
     *
     * @return the collector
     */
    public static Collector<Criterion, ?, Criterion> toConjunction() {
        return toCriterion(Restrictions::conjunction);
    }

    /**
     * Creates a {@code Collector} that collects criterions to a single criterion.
     *
     * @param finisher the finishing function to a create a single criterion from an criterion array
     *
     * @return the collector
     */
    private static Collector<Criterion, ?, Criterion> toCriterion(Function<Criterion[], Criterion> finisher) {
        return collectingAndThen(collectingAndThen(toSet(), (Set<Criterion> s) -> s.stream()
                .toArray(Criterion[]::new)), finisher);
    }

}
