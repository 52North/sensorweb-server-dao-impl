
package sandbox;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(
    locations = {
        "file:src/main/webapp/WEB-INF/spring/dispatcher-servlet.xml"
    })
public class SpringIntegrationTest {
    
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void test() throws Exception {
        mockMvc.perform(get("/services/1").accept(MediaType.APPLICATION_JSON))
               .andDo(MockMvcResultHandlers.print())
               .andExpect(jsonPath("$.id").value("1"));
    }

    // @Test
    // public void testGetFoo() {
    // String URI = "http://localhost:8080/api/services/{id}";
    // RestTemplate restTemplate = new RestTemplate();
    // ServiceOutput service = restTemplate.getForObject(URI, ServiceOutput.class, 1);
    // MatcherAssert.assertThat(service.getId(), Matchers.is("1"));
    // }
    
}
