package com.icthh.xm.uaa.cucumber.stepdefs;

import com.icthh.xm.uaa.UaaApp;

import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;

import org.springframework.boot.test.context.SpringBootTest;

@WebAppConfiguration
@SpringBootTest
@ContextConfiguration(classes = {UaaApp.class, XmOverrideConfiguration.class})
public abstract class StepDefs {

    protected ResultActions actions;

}
