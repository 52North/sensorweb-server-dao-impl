
package org.n52.series.springdata;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.type.BasicType;
import org.n52.hibernate.type.SmallBooleanType;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
public class DatabaseConfig {

    @Autowired
    @Qualifier("entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Bean
    public DbQueryFactory dbQueryFactory(@Value("${database.srid:'EPSG:4326'}") String srid) {
        return new DefaultDbQueryFactory(srid);
    }

    @Bean
    public EntityManagerFactory entityManagerFactory(DataSource datasource, JpaProperties properties)
            throws IOException {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(addCustomTypes(properties));
        emf.setDataSource(datasource);
        emf.afterPropertiesSet();
        return emf.getNativeEntityManagerFactory();
    }

    private Map<String, Object> addCustomTypes(JpaProperties jpaProperties) {
        Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties());
        properties.put(EntityManagerFactoryBuilderImpl.TYPE_CONTRIBUTORS, createTypeContributorsList());
        return properties;
    }

    private TypeContributorList createTypeContributorsList() {
        return (TypeContributorList) Arrays.asList(toTypeContributor(SmallBooleanType.INSTANCE, "small_boolean"));
    }

    private <T extends BasicType> TypeContributor toTypeContributor(T type, String... keys) {
        return (typeContributions, serviceRegistry) -> typeContributions.contributeType(type, keys);
    }

    @Bean
    @Primary
    public SessionFactory sessionFactory() {
        if (entityManagerFactory.unwrap(SessionFactory.class) == null) {
            throw new NullPointerException("factory is not a hibernate factory");
        }
        return entityManagerFactory.unwrap(SessionFactory.class);
    }
}
