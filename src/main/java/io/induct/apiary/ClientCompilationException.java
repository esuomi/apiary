package io.induct.apiary;

import com.google.common.collect.Lists;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.1.2016
 */
public class ClientCompilationException extends ApiaryException {
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    public ClientCompilationException(String message, Throwable throwable) {
        super(message, throwable);
        this.diagnostics = Collections.emptyList();
    }

    public ClientCompilationException(String message, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        super(message);
        this.diagnostics = diagnostics;
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics;
    }
}
