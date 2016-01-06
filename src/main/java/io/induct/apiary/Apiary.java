package io.induct.apiary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import io.induct.http.builders.Request;

import javax.inject.Inject;
import javax.inject.Named;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Inject
    public Apiary(@Named(GENERATED_DIR_KEY) String generatedDir, Injector injector) {
        this.targetRoot = fs.getPath(generatedDir);
        this.injector = injector;
    }

    private static final Function<Method, String> TYPE_REFERENCE_FIELDS_GENERATOR = (method) -> {
        Class<?> returnType = method.getReturnType();
        return "    private static final TypeReference<" + returnType.getSimpleName() + "> mappingOf" + returnType.getSimpleName() + "Type = new TypeReference<" + returnType.getSimpleName() + ">() {};\n";
    };

    public <T> T generateClient(Class<T> apiDefiningInterface) {
        Preconditions.checkNotNull(apiDefiningInterface, "Can not generate client implementation from null class");
        Preconditions.checkArgument(apiDefiningInterface.isInterface(), "Class must be an interface");
        Client clientConfig = apiDefiningInterface.getDeclaredAnnotation(Client.class);
        Preconditions.checkNotNull(clientConfig, "Class must be annotated with " + Client.class.getName());

        String targetPackageName = clientConfig.targetPackage().replace("${root}", apiDefiningInterface.getPackage().getName());
        String targetClassName = clientConfig.targetClassName().replace("${clientName}", apiDefiningInterface.getSimpleName());
        String targetFqn = targetPackageName + "." + targetClassName;

        try {
            String classSource = generateClassSource(apiDefiningInterface, targetPackageName, targetClassName);

            Path targetFile = createTargetFile(targetPackageName, targetClassName);
            Path sourceFile = saveToFile(targetFile, classSource);
            compile(sourceFile);
            T instance = loadGeneratedClass(targetRoot, targetFqn);
            return instance;
        } catch (ApiaryException ae) {
            throw new ApiaryException("Failed to generate client for API defining interface " + apiDefiningInterface, ae);
        }
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

    private <T> String generateClassSource(Class<T> apiDefiningInterface, String targetPackageName, String targetClassName) {
        Client clientConfig = apiDefiningInterface.getDeclaredAnnotation(Client.class);

        String packageDefinition = "package " + targetPackageName + ";\n\n";

        Stream<Class> allImports = Stream.concat(
                Stream.of(apiDefiningInterface, ApiClient.class, Request.class, TypeReference.class),
                findApiMethods(apiDefiningInterface).map(Method::getReturnType)
        );

        String imports = String.join("", allImports
                .map((cls) -> "import " + cls.getName() + ";\n")
                .sorted()
                .collect(Collectors.toList()));

        String classDefinition = "\npublic class "
                + targetClassName + " extends "
                + ApiClient.class.getSimpleName()
                + " implements "
                + apiDefiningInterface.getSimpleName()
                + " {\n";

        String staticFields = String.join("", findApiMethods(apiDefiningInterface).map(TYPE_REFERENCE_FIELDS_GENERATOR).collect(Collectors.toList()));

        String methods = String.join(", ", generateMethods(findApiMethods(apiDefiningInterface), clientConfig.paramFormat()).collect(Collectors.toList()));

        return packageDefinition
                + imports
                + classDefinition
                + staticFields
                + methods
                + "}\n";
    }

    private Stream<String> generateMethods(Stream<Method> apiMethods, CaseFormat paramFormat) {
        return apiMethods.map((methodRef) -> {
            StringBuilder methodSource = new StringBuilder();
            String returnTypeFqn = methodRef.getReturnType().getName();
            methodSource.append("    public ").append(returnTypeFqn).append(" ").append(methodRef.getName()).append("(");

            Parameter[] methodParams = methodRef.getParameters();
            String params = String.join(", ", Stream.of(methodParams)
                .map((param) -> param.getType().getName() + " " + param.getName())
                .collect(Collectors.toList()));
            methodSource.append(params);

            methodSource.append(") {\n");

            methodSource.append("        Request request = createRequestBuilder()\n");
            methodSource.append("                .withUrl(\"").append(methodRef.getAnnotation(Api.class).url()).append("\")\n");
            if (methodParams.length > 0) {
                methodSource.append("                .withParams(params -> {\n");
                for (Parameter param : methodParams) {
                    String methodParamName = param.getName();
                    String apiParamName = CaseFormat.LOWER_CAMEL.to(paramFormat, methodParamName);
                    if (param.getType().isAssignableFrom(Optional.class)) {
                        methodSource.append("                    if (").append(methodParamName).append(".isPresent()) {\n")
                            .append("                        params.put(\"")
                            .append(apiParamName).append("\", ")
                            .append("asString(").append(methodParamName).append(".get())")
                            .append(");\n")
                            .append("                    }\n");
                    } else {
                        methodSource.append("                    params.put(\"")
                            .append(apiParamName).append("\", ")
                            .append("asString(").append(methodParamName).append(")")
                            .append(");\n");
                    }
                }
                methodSource.append("                })\n");
            }

            methodSource.append("                .build();\n");
            methodSource.append("        return handleApiCall(request, mappingOf").append(methodRef.getReturnType().getSimpleName()).append("Type);\n");
            methodSource.append("    }\n");
            return methodSource.toString();
        });
    }

    private <T> Stream<Method> findApiMethods(Class<T> apiDefiningInterface) {
        return Stream.of(apiDefiningInterface.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Api.class));
    }

}
