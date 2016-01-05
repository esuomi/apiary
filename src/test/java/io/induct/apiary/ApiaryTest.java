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
import org.junit.Test;

import java.util.Optional;

/**
 * @author Esko Suomi <suomi.esko@gmail.com>
 * @since 1.1.2016
 */
public class ApiaryTest {


    @Test
    public void generatesFunctioningApiClient() throws Exception {
        try (AsyncHttpClient ningClient = new AsyncHttpClient()) {
            HttpClient httpClient = new NingHttpClient(ningClient);
            Injector injector = Guice.createInjector(new DanielModule(), new AbstractModule() {
                @Override
                protected void configure() {
                    bind(HttpClient.class).toInstance(httpClient);
                    bind(Apiary.class);
                    Multibinder<Module> jacksonModules = Multibinder.newSetBinder(binder(), Module.class);
                    jacksonModules.addBinding().toInstance(new ParameterNamesModule());
                    bind(String.class).annotatedWith(Names.named(Apiary.GENERATED_DIR_KEY)).toInstance("/tmp");
                }
            });
            Apiary apiary = injector.getInstance(Apiary.class);
            NASA nasaClient = apiary.generateClient(NASA.class);
            ApodImage apod = nasaClient.apod(Optional.empty(), Optional.empty(), Optional.empty(), "DEMO_KEY");
            System.out.println("apod = " + apod);
        }
    }
}
