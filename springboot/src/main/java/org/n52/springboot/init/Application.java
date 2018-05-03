/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.springboot.init;

import java.net.URL;
import java.net.URLClassLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



/**
 *
 * @author specki
 */
@RestController
@ComponentScan
@Controller
@EnableAutoConfiguration
@ImportResource({"classpath*:/WEB-INF/spring/dispatcher-servlet.xml"})
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ServletRegistrationBean createDispatcherServlet() {
        ServletRegistrationBean reg = new ServletRegistrationBean(new DispatcherServlet());
        reg.setLoadOnStartup(1);
        reg.setName("api-dispatcher");
        reg.addUrlMappings("/api/*");
        reg.addInitParameter("contextConfigLocation", "classpath*:/WEB-INF/spring/dispatcher-servlet.xml");
        return reg;
    }

    @Bean
    public FilterRegistrationBean createEncodingFilter() {
        FilterRegistrationBean filter = new FilterRegistrationBean();
        filter.setFilter(new CharacterEncodingFilter());
        filter.addInitParameter("encoding", "UTF-8");
        return filter;
    }

    @Bean
    public WebMvcConfigurer createCORSFilter() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/*")
                    .allowedOrigins("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .exposedHeaders("Content-Type", "Content-Encoding")
                    .allowedHeaders("Content-Type", "Content-Encoding", "Accept");
            };
        };
    }
}
