
package org.n52.series.test;

import java.util.List;

import org.assertj.core.util.Arrays;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;
import org.n52.series.db.beans.DescribableEntity;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

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

    public static Geometry fromWkt(String wkt) throws ParseException {
         return JTS.to(Wkt.fromWkt(wkt));
    }

}
