package org.n52.series.db.da;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.n52.series.db.beans.AggregationTypeEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.dao.DbQuery;

public class AveragingTimeUtil {

    public static final String HOURLY = "hourly";

    public static final String DAILY = "daily";

    public static final String SECOND = "second";

    public static final String MINUTE = "minute";

    public static final String HOUR = "hour";

    public static final String DAY = "day";

    public static final String P1D = "P1D";

    public static boolean checkForPrimaryData(DbQuery query, QuantityDatasetEntity seriesEntity) {
        String primaryData = seriesEntity.getPrimaryData();
        if ((primaryData.equals(HOUR) || primaryData.equals(SECOND) || primaryData.equals(MINUTE))
                && query.getParameters().getAveragingTime().equals("hourly")) {
            return true;
        } else if (primaryData.equals(DAY) && (query.getParameters().getAveragingTime().equals(DAILY)
                || query.getParameters().getAveragingTime().equals(P1D))) {
            return true;
        }
        return false;
    }

    public static boolean checkForLowerResolution(DbQuery query, QuantityDatasetEntity seriesEntity) {
        return seriesEntity.getPrimaryData().equals(DAY) && query.getParameters().getAveragingTime().equals(HOURLY);
    }

    public static String getAggregationType(DbQuery query) {
        return query.getParameters().getAveragingTime().equals(DAILY) ? P1D : query.getParameters().getAveragingTime();
    }

    public static Set<String> getAggregationTypes(Set<AggregationTypeEntity> aggregationTypes, String primaryData) {
        Set<String> types = new LinkedHashSet<>();
        if (aggregationTypes != null && !aggregationTypes.isEmpty()) {
            types.addAll(aggregationTypes.stream().map(at -> at.getDomainId()).collect(Collectors.toSet()));
        }
        if (primaryData.equals(HOUR) || primaryData.equals(SECOND) || primaryData.equals(MINUTE)) {
            types.add(HOURLY);
        } else if (primaryData.equals(DAY)) {
            types.add(DAILY);
        } else {
            types.add(primaryData);
        }
        if (types.contains(P1D)) {
            types.add(DAILY);
        }
        return types;
    }

}
