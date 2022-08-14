import com.easycode.codegen.api.core.mavenplugin.ApiCodegenMojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.codehaus.plexus.PlexusTestCase.getBasedir;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @class-name: MojoTest
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-07-30 16:39
 */
public class MojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @Test
    @Ignore
    public void testMojoGoal() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/projects/demo/pom.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        ApiCodegenMojo mojo = (ApiCodegenMojo) rule.lookupMojo("ApiCodegenMojo", testPom);
        mojo.execute();
    }


}
