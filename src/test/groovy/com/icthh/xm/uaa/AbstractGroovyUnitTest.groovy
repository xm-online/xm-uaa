package com.icthh.xm.uaa

import groovy.test.GroovyAssert
import org.junit.Before
import org.junit.experimental.categories.Category


/**
 * Abstract test for extension for any Groovy Unit test.
 * Marks test with junit {@link Category} the same as java unit tests
 *
 * <p>
 * Guidelines for groovy unit test writing:
 *
 * <ol>
 *  <li> Read the manual: <a href="http://docs.groovy-lang.org/next/html/documentation/core-testing-guide.html">Testing Guide</a>
 *  <li> Extend {@link AbstractGroovyUnitTest} class for all groovy Unit tests.
 *  <li> Use one Unit test for one LEP script.
 *  <li> Define script ame under test with AbstractGroovyUnitTest.resolveScriptName(path) method.
 *  <li> Use native groovy assert instead of assert*() methods from JUnit. It is quite powerful.
 *  <li> Define lepContext basic structure used in test when overrides setLepContext() method:
 *  <pre>
 *     Object setLepContext() {
 *         [
 *                 inArgs     : [
 *                         functionInput: [
 *                                 companyType: null
 *                         ]
 *                 ],
 *                 services   : [
 *                         xmEntity         : null,
 *                         xmTenantLifeCycle: null
 *                 ],
 *                 authContext: [
 *                         getRequiredUserKey: { null }
 *                 ]
 *         ]
 *    }
 *  </pre>
 *  <li> Cover business logic in LEPs with Unit test and use mocking for XM Services and external calls when possible.
 *  <li> Use simplest possible mocking method if possible. Note you can mix it with the assertions:
 *  <pre>
 *         lepContext.services.xmEntity = [
 *                 findAll    : { Specification<XmEntity> spec -> [] },
 *                 updateState: { IdOrKey idOrKey, String state, Map context ->
 *                     assert 'REQUIRED_STATE' == state
 *                     [id: idOrKey.id] as XmEntity
 *                 }
 *         ] as XmEntityService
 *  </pre>
 *  <li> Using Mockito is also possible:
 *  <pre>
 *         import static org.mockito.Mockito.*
 *         ...
 *
 *      void testChangeState() {
 *
 *         XmTenantLifecycleService xmTenantLifeCycle = mock(XmTenantLifecycleService.class)
 *         doNothing().when(xmTenantLifeCycle).changeState(any(XmEntity), anyString(), anyMap())
 *         lepContext.services.xmTenantLifeCycle = xmTenantLifeCycle
 *
 *         XmEntity value = evaluateScript()
 *
 *         verify(xmTenantLifeCycle).changeState([id: 1] as XmEntity, 'STATE1', [:])
 *      }
 *  </pre>
 * </ol>
 */
abstract class AbstractGroovyUnitTest {

    private static  String LEP_CUSTOM_TEST_PATH = 'src/main/lep/%s/uaa/lep'

    private GroovyShell groovyShell = new GroovyShell()
    def lepContext

    /**
     * Initializes lepContext with object enough to execute particular LEP under test.
     * This method executes before each test.
     */
    abstract Object setLepContext()

    @Before
    void setUp() {
        lepContext = setLepContext()
        groovyShell.setVariable('lepContext', lepContext)
    }

    def <T> T evaluateScript(String scriptName) {
        groovyShell.evaluate(new File(scriptName)) as T
    }

    static <T extends Throwable> T shouldFail(Class clazz, Closure code) {
        GroovyAssert.shouldFail(clazz, code) as T
    }

    /**
     * Resolves relative path of the LEP to absolute path with tenant name awareness.
     * @param path relative path of the lep script after lep/ folder inside microservice.
     *  For example if we have
     *  such LEP in config repo for xm-entity: '/config/tenants/XM/entity/lep/service/entity/Save$$around.groovy'
     *  then we need to define input parameter path as '/service/entity/Save$$around.groovy'
     * @return returns path of the script in the test execution context.
     *  For example above result will be 'src/main/lep/XM/entity/lep/service/entity/Save$$around.groovy'
     */
    protected resolveScriptName(String path) {
        String.format(LEP_CUSTOM_TEST_PATH, extractLepTestTenant()) + path
    }

    protected String extractLepTestTenant() {
        this.class.getPackage().getName().replaceAll("\\..*", "")
    }

}
