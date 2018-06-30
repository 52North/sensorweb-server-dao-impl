
package org.n52.series.springdata;

import java.io.IOException;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.n52.series.db.beans.DatasetEntity;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Configures a {@link HibernatePersistenceLoadingLocalContainerEntityManagerFactoryBean} which scans for a
 * persistence xml at a given location. Within the persistence xml file, all mapping files shall be listed
 * which are important for the tests.
 * <p>
 * This enables flexible setup for different tests loading different kinds of mapping assemblies (e.g. of
 * different profiles).
 * <p>
 * Test configuration can just inherit from this class and set proper annotation config
 * ({@link SpringBootCondition}, {@link EnableJpaRepositories} etc.) needed within that test class.
 *
 * @see HibernatePersistenceLoadingLocalContainerEntityManagerFactoryBean
 */
public abstract class TestRepositoryConfig<T extends DatasetEntity> {

    private final String xmlPersistenceLocation;

    public TestRepositoryConfig(final String xmlPersistenceLocation) {
        this.xmlPersistenceLocation = xmlPersistenceLocation;
    }

    @Bean
    public abstract TestRepositories testRepositories();

    @Bean
    public EntityManagerFactory entityManagerFactory(final DataSource datasource, final JpaProperties properties)
            throws IOException {
        final LocalContainerEntityManagerFactoryBean emf = createEntityManagerFactoryBean(datasource,
                                                                                    properties,
                                                                                    xmlPersistenceLocation);
        return emf.getNativeEntityManagerFactory();
    }

    private LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean(final DataSource datasource,
                                                                                  final JpaProperties properties,
                                                                                  final String xmlPersistenceLocation) {
        return new HibernatePersistenceLoadingLocalContainerEntityManagerFactoryBean(datasource,
                                                                                     properties,
                                                                                     xmlPersistenceLocation);
    }
}
