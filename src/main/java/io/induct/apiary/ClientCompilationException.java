package io.induct.apiary;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.1.2016
 */
class ClientCompilationException extends ApiaryException {
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics;

    public ClientCompilationException(String message, Throwable throwable) {
        super(message, throwable);
        this.diagnostics = Collections.emptyList();
    }

    public ClientCompilationException(String message, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        super(message);
        this.diagnostics = diagnostics;
    }

    @SuppressWarnings("unused")
    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return diagnostics;
    }
}
