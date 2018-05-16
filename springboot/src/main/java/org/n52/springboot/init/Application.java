/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.n52.springboot.init;

import java.io.IOException;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author specki
 */
@SpringBootApplication
@ImportResource({"classpath*:/spring/dispatcher-servlet.xml"})
public class Application {

    @Autowired
    @Qualifier("entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Bean
    public EntityManagerFactory entityManagerFactory(DataSource datasource, JpaProperties properties)
            throws IOException {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(properties.getProperties());
        emf.setDataSource(datasource);

        emf.afterPropertiesSet();
        return emf.getNativeEntityManagerFactory();
    }

    @Bean
    @Primary
    public SessionFactory sessionFactory() {
        if (entityManagerFactory.unwrap(SessionFactory.class) == null) {
            throw new NullPointerException("factory is not a hibernate factory");
        }
        return entityManagerFactory.unwrap(SessionFactory.class);
    }

    @Bean
    public WebMvcConfigurer createCORSFilter() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/*").allowedOrigins("*").allowedMethods("GET",
                                                                             "POST",
                                                                             "PUT",
                                                                             "DELETE",
                                                                             "OPTIONS").exposedHeaders("Content-Type",
                                                                                                       "Content-Encoding").allowedHeaders("Content-Type",
                                                                                                                                          "Content-Encoding",
                                                                                                                                          "Accept");
            };
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
