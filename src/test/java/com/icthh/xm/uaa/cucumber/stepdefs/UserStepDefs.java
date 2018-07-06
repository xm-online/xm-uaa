package com.icthh.xm.uaa.cucumber.stepdefs;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.uaa.security.DomainUserDetails;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

public class UserStepDefs extends StepDefs {

    @Autowired
    private WebApplicationContext context;

    private MockMvc restUserMockMvc;

    @Before
    public void setup() {
        this.restUserMockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @When("^I search user by users key '(.*)' on tenant '(.*)'$")
    @WithMockUser
    public void i_search_user_admin(String userKey, String tenant) throws Throwable {
        UserDetails userDetails = new DomainUserDetails("xm", "P@ssw0rd",
            Collections.singletonList(new SimpleGrantedAuthority(SUPER_ADMIN)), "XM", "xm", false, null, null, false, null);
        actions = restUserMockMvc.perform(get("/api/users/" + userKey).with(user(userDetails))
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
