package org.n52.series.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.n52.series.db.beans.DatasetEntity;
import org.springframework.stereotype.Component;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface ValueAssemblerComponent {

    /**
     * The type of data to assemble.
     *
     * @return the type
     */
    String value() default "";

    /**
     * @return the dataset entity type
     */
    Class<? extends DatasetEntity> datasetEntityType() default DatasetEntity.class;

}
