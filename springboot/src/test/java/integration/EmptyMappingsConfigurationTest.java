/*package unit;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(
    locations = {
        "file:classpath*:/WEB-INF/spring/dispatcher-servlet.xml"
    })
@TestPropertySource(properties = "series.database.mappings=")
public class EmptyMappingsConfigurationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void queryServices() throws Exception {
        mockMvc.perform(get("/services/1").accept(MediaType.APPLICATION_JSON))
               .andDo(MockMvcResultHandlers.print())
               .andExpect(status().is4xxClientError());
    }

    @Test
    public void queryOfferings() throws Exception {
        mockMvc.perform(get("/offerings").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is4xxClientError());
    }

}
*/