
package org.n52.series.db;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Configures a {@link LocalContainerEntityManagerFactoryBean} with proper datasource and JPA properties. It
 * also sets the {@link HibernateJpaVendorAdapter} and scans for xml mappings defined within a given xml
 * resource location, e.g. {@literal classpath*:META-INF/persistence.xml} or {@literal /usecases/case-A.xml}.
 */
public class HibernatePersistenceLoadingLocalContainerEntityManagerFactoryBean extends
        LocalContainerEntityManagerFactoryBean {

    /**
     * @param datasource
     *        the datasource
     * @param properties
     *        the JPA properties
     * @param xmlPersistenceLocation
     *        the xml resource location, e.g. {@literal classpath*:META-INF/persistence.xml}
     */
    public HibernatePersistenceLoadingLocalContainerEntityManagerFactoryBean(DataSource datasource,
                                                                             JpaProperties properties,
                                                                             String xmlPersistenceLocation) {
        super();
        setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        setPersistenceXmlLocation(xmlPersistenceLocation);
        setJpaPropertyMap(properties.getProperties());
        setDataSource(datasource);

        afterPropertiesSet();
    }
}
