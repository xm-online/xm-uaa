package com.icthh.xm.uaa.cucumber.stepdefs;

import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.icthh.xm.uaa.web.rest.UserResource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserStepDefs extends StepDefs {

    @Autowired
    private UserResource userResource;

    private MockMvc restUserMockMvc;

    @Before
    public void setup() {
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource).build();
    }

    @When("^I search user by users key '(.*)' on tenant '(.*)'$")
    public void i_search_user_admin(String userKey, String tenant) throws Throwable {
        actions = restUserMockMvc.perform(get("/api/users/" + userKey)
            .header(HEADER_TENANT, tenant)
            .accept(MediaType.APPLICATION_JSON));
        actions.andDo(r -> System.out.println("********" + r.getResponse()));
    }

    @Then("^the user is found$")
    public void the_user_is_found() throws Throwable {
        actions
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }

    @Then("^his first name is '(.*)'$")
    public void his_first_name_is(String firstName) throws Throwable {
        actions.andExpect(jsonPath("$.firstName").value(firstName));
    }

}
