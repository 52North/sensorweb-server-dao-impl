
package org.n52.springboot.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableWebMvc
@Configuration
// @ImportResource({"classpath*:/spring/dispatcher-servlet.xml"})
@ImportResource({"classpath*:/spring/application-context.xml"})
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private ObjectMapper objectMapper;

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

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
                  .mediaType("json", MediaType.APPLICATION_JSON)
                  .mediaType("pdf", MediaType.APPLICATION_PDF)
                  .mediaType("csv", new MediaType("text", "csv"))
                  .useRegisteredExtensionsOnly(true);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        View jsonView = createJsonView();
        registry.enableContentNegotiation(jsonView);
    }

    private View createJsonView() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        jsonView.setExtractValueFromSingleKeyModel(true);
        jsonView.setObjectMapper(getObjectMapper());
        return jsonView;
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper om = objectMapper == null
            ? createDefaultObjectMapper()
            : objectMapper;
        return om;
    }

    private ObjectMapper createDefaultObjectMapper() {
        return new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
    }

}
