package config;


import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.util.Properties;
import javax.sql.DataSource;
import org.n52.series.db.SeriesHibernateSessionHolder;
import org.n52.series.db.SeriesLocalSessionFactoryBean;
import org.n52.series.db.da.DefaultDataRepositoryFactory;
import org.n52.springboot.init.Application;
import org.n52.springboot.init.DefaultConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author specki
 */

@SpringBootConfiguration
@EnableAutoConfiguration(/*exclude={DataSourceAutoConfiguration.class*/)
@ImportResource({"classpath*:/WEB-INF/spring/dispatcher-servlet.xml"})
@Import({DefaultConfig.class, Application.class})
public class TestDatabaseConfig {

    @Autowired
    DataSource source;

    @Value("${hibernate.hbm2ddl.import_file}")
    String import_files;

    @Value("${series.database.mappings}")
    String mappings;

    @Bean
    public SeriesHibernateSessionHolder createSeriesHibernateSessionHolder() {
        return new SeriesHibernateSessionHolder();
    }

    @Bean
    public ComboPooledDataSource dataSource() throws PropertyVetoException {
        ComboPooledDataSource bean = new ComboPooledDataSource();
        bean.setJdbcUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL");
        bean.setDriverClass("org.h2.Driver");
        bean.setUser("");
        bean.setPassword("");
        return bean;
    }

    @Bean
    public SeriesLocalSessionFactoryBean createSeriesLocalSessionFactoryBean() {
        SeriesLocalSessionFactoryBean bean = new SeriesLocalSessionFactoryBean();
        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.spatial.dialect.postgis.PostgisDialect");
        hibernateProperties.setProperty("hibernate.default_schema", "public");
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create");
        hibernateProperties.setProperty("hibernate.format_sql", "true");
        hibernateProperties.setProperty("hibernate.hbm2ddl.import_files", import_files);
        hibernateProperties.setProperty("jdbc.time.zone", "UTC");
        bean.setHibernateProperties(hibernateProperties);
        bean.setDataSource(source);
        bean.setMappingResources(mappings.split(","));
        return bean;
    }

    @Bean
    public DefaultDataRepositoryFactory dataRepositoryFactory() {
        return new DefaultDataRepositoryFactory();
    }
}
