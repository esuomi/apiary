package io.induct.apiary;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import io.induct.apiary.annotations.Api;
import io.induct.apiary.annotations.Client;
import io.induct.apiary.base.ApiClient;
import io.induct.apiary.generation.SourceGenerator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static io.induct.apiary.annotations.Client.Environment;

/**
 * Apiary allows easy generation of HTTP API clients from interfaces marked with {@link Client} annotation.
 * To be usable, the interface has to have at least one {@link Api} annotated method.
 *
 * @since 1.1.2016
 * @see Client
 * @see Api
 */
public class Apiary extends ApiClient {

    public static final String GENERATED_DIR_KEY = "apiary.generated.dir";

    private final Injector injector;

    private final FileSystem fs = FileSystems.getDefault();

    private final Path targetRoot;

    private final SourceGenerator sourceGenerator;

    @Inject
    public Apiary(@Named(GENERATED_DIR_KEY) String generatedDir, Injector injector, SourceGenerator sourceGenerator) {
        this.sourceGenerator = sourceGenerator;
        this.targetRoot = fs.getPath(generatedDir);
        this.injector = injector;
    }

    public <T> T generateClient(Class<T> apiDefiningInterface, String targetEnvironmentName) {
        Preconditions.checkNotNull(apiDefiningInterface, "Can not generate client implementation from null class");
        Preconditions.checkArgument(apiDefiningInterface.isInterface(), "Class must be an interface");
        Client clientConfig = apiDefiningInterface.getDeclaredAnnotation(Client.class);
        Preconditions.checkNotNull(clientConfig, "Class must be annotated with " + Client.class.getName());
        Environment targetEnv = resolveEnv(apiDefiningInterface, targetEnvironmentName, clientConfig);

        String targetPackageName = clientConfig.targetPackage().replace("${root}", apiDefiningInterface.getPackage().getName());
        String targetClassName = clientConfig.targetClassName().replace("${clientName}", apiDefiningInterface.getSimpleName());
        String targetFqn = targetPackageName + "." + targetClassName;

        try {
            String classSource = sourceGenerator.generateClassSource(apiDefiningInterface, targetPackageName, targetClassName, targetEnv);

            Path targetFile = createTargetFile(targetPackageName, targetClassName);
            Path sourceFile = saveToFile(targetFile, classSource);
            compile(sourceFile);
            T instance = loadGeneratedClass(targetRoot, targetFqn);
            return instance;
        } catch (ApiaryException ae) {
            throw new ApiaryException("Failed to generate client for API defining interface " + apiDefiningInterface, ae);
        }
    }

    private <T> Environment resolveEnv(Class<T> apiDefiningInterface, String targetEnvironmentName, Client clientConfig) {
        Preconditions.checkNotNull(targetEnvironmentName, "You must specify the environment to run the client in");
        Optional<Environment> possibleTargetEnvironment = Stream.of(clientConfig.environments()).filter(e -> e.name().equals(targetEnvironmentName)).findFirst();
        Preconditions.checkArgument(possibleTargetEnvironment.isPresent(), "The interface " + apiDefiningInterface + " does not define an environment with name '" + targetEnvironmentName + "'");
        return possibleTargetEnvironment.get();
    }

    private <T> T loadGeneratedClass(Path targetRoot, String targetFqn) {
        try {
            URLClassLoader classLoader = new URLClassLoader(new URL[]{ targetRoot.toUri().toURL() });
            @SuppressWarnings("unchecked")
            Class<T> cls = (Class<T>) classLoader.loadClass(targetFqn);
            return injector.getInstance(cls);
        } catch (MalformedURLException | ClassNotFoundException e) {
            throw new ClientCompilationException("Failed to load class", e);
        }
    }

    private void compile(Path sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            Iterable<String> compilerOptions = Arrays.asList(
                    "-classpath", System.getProperty("java.class.path"),
                    "-parameters");
            Iterable<? extends JavaFileObject> javaFo = fileManager.getJavaFileObjects(sourceFile.toFile());

            if (compiler.getTask(null, fileManager, diagnostics, compilerOptions, null, javaFo).call()
                && diagnostics.getDiagnostics().isEmpty()) {
                // everything's good to go!
            } else {
                throw new ClientCompilationException("Compilation of " + sourceFile + " failed", diagnostics.getDiagnostics());
            }
        } catch (IOException e) {
            throw new ClientCompilationException("Failed to compile source file " + sourceFile, e);
        }
    }

    private Path saveToFile(Path target, String content) {
        try {
            return Files.write(target, content.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new ClientGenerationException("Failed to write source to file", e);
        }
    }

    private <T> Path createTargetFile(String targetPackage, String targetClassName) {
        Path targetDir;
        Path packageDir = targetRoot.resolve(targetPackage.replace(".", "/"));
        try {
            if (!Files.exists(packageDir)) {
                targetDir = Files.createDirectories(packageDir);
            } else {
                targetDir = packageDir;
            }
        } catch (IOException e) {
            throw new ClientGenerationException("Cannot create directory " + packageDir, e);
        }
        Path targetFile = targetDir.resolve(Paths.get(targetClassName + ".java"));
        try {
            if (Files.exists(targetFile)) {
                // TODO: Overwrite config flag
                throw new ClientGenerationException("Target file already exists at path " + targetFile);
            }
            return Files.createFile(targetFile);
        } catch (IOException e) {
            throw new ClientGenerationException("Could not save generated source to " + targetFile, e);
        }
    }
}
