package qa.thinogueiras.runners;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({"qa.thinogueiras.servicos"})
public class SuiteTest {

}

