package org.n52.series.test;

import java.util.List;

import org.assertj.core.util.Arrays;
import org.n52.series.db.beans.DescribableEntity;

public class TestUtils {
    
    private TestUtils() {
        // util class
    }
    
    public static String getIdAsString(DescribableEntity entity) {
        if (entity == null) {
            return null;
        }
        Long id = entity.getId();
        return id != null
            ? id.toString()
            : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(T... items) {
        return (List<T>) Arrays.asList(items);
    }

}
