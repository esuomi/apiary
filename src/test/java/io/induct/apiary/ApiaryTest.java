package io.induct.apiary;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.ning.http.client.AsyncHttpClient;
import io.induct.apiary.nasa.ApodImage;
import io.induct.apiary.nasa.NASA;
import io.induct.daniel.ioc.guice.DanielModule;
import io.induct.http.HttpClient;
import io.induct.http.ning.NingHttpClient;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;

/**
 * @since 1.1.2016
 */
public class ApiaryTest {

    @ClassRule
    public static TestingHttpServer server = new TestingHttpServer();

    private File tempRoot = createTempRoot("/tmp");

    private File createTempRoot(String dir) {
        File r = new File(dir);
        if (!r.exists() && !r.mkdir()) {
            throw new RuntimeException("Failed to create directory " + dir + ", cannot run tests");
        }
        return r;
    }

    @Rule
    public TemporaryFolder temp = new TemporaryFolder(tempRoot);

    @Rule
    public TestName name = new TestName();

    private Injector injector;
    private AsyncHttpClient ningClient;

    @Before
    public void setUp() throws Exception {
        ningClient = new AsyncHttpClient();

        injector = Guice.createInjector(new DanielModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(HttpClient.class).toInstance(new NingHttpClient(ningClient));
                bind(Apiary.class);
                Multibinder<Module> jacksonModules = Multibinder.newSetBinder(binder(), Module.class);
                jacksonModules.addBinding().toInstance(new ParameterNamesModule());
                try {
                    File testDir = temp.newFolder("ApiaryTest", name.getMethodName());
                    bind(String.class).annotatedWith(Names.named(Apiary.GENERATED_DIR_KEY)).toInstance(testDir.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        if (ningClient != null) {
            ningClient.close();
        }
    }

    // Is it just me or is JUnit everything's-an-annotation kind of overly verbose?

    @Test
    public void generatesFunctioningApiClient() throws Exception {
        Apiary apiary = injector.getInstance(Apiary.class);
        NASA nasaClient = apiary.generateClient(NASA.class, "local");
        ApodImage apod = nasaClient.apod(Optional.empty(), Optional.empty(), Optional.empty(), "DEMO_KEY");
        // TODO: Could assert this further
        assertNotNull(apod);
    }
}
