package io.induct.apiary.generation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.CaseFormat;
import io.induct.apiary.ApiClient;
import io.induct.apiary.annotations.Api;
import io.induct.apiary.annotations.Client;
import io.induct.http.builders.Request;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Esko Suomi <suomi.esko@gmail.com>
 * @since 9.1.2016
 */
public class SourceGenerator {

    private static final Function<Method, String> TYPE_REFERENCE_FIELDS_GENERATOR = (method) -> {
        Class<?> returnType = method.getReturnType();
        return "    private static final TypeReference<" + returnType.getSimpleName() + "> mappingOf" + returnType.getSimpleName() + "Type = new TypeReference<" + returnType.getSimpleName() + ">() {};\n";
    };

    public <T> String generateClassSource(
        Class<T> apiDefiningInterface,
        String targetPackageName,
        String targetClassName,
        Client.Environment env) {
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

        String methods = String.join(", ", generateMethods(findApiMethods(apiDefiningInterface), clientConfig.paramFormat(), env).collect(Collectors.toList()));

        return packageDefinition
            + imports
            + classDefinition
            + staticFields
            + methods
            + "}\n";
    }


    private <T> Stream<Method> findApiMethods(Class<T> apiDefiningInterface) {
        return Stream.of(apiDefiningInterface.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Api.class));
    }


    private Stream<String> generateMethods(Stream<Method> apiMethods, CaseFormat apiParamFormat, Client.Environment env) {
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

            Api apiConfig = methodRef.getAnnotation(Api.class);
            String apiUrl = resolveApiUrl(env, apiConfig);

            methodSource.append("                .withUrl(\"").append(apiUrl).append("\")\n");
            if (methodParams.length > 0) {
                methodSource.append("                .withParams(params -> {\n");
                for (Parameter param : methodParams) {
                    String methodParamName = param.getName();
                    String apiParamName = CaseFormat.LOWER_CAMEL.to(apiParamFormat, methodParamName);
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


    private String resolveApiUrl(Client.Environment targetEnv, Api apiConfig) {
        return targetEnv.root() + apiConfig.path();
    }

}
