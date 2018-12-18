package org.n52.series.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import org.n52.series.db.beans.DatasetEntity;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
@Inherited
public @interface DataRepositoryComponent {

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
