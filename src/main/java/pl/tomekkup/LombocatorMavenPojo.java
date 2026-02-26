package pl.tomekkup;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Main plugin class
 * @author Tomek Kuprowski
 */
@Mojo(name = "lombocator", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class LombocatorMavenPojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/java", required = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/lombok-report.html", required = true)
    private File reportFile;

    private final List<String> reportEntries = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Files.walk(this.sourceDirectory.toPath())
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(this::processFile);

            generateHtmlReport();

        } catch (IOException e) {
            throw new MojoExecutionException("Error processing files", e);
        }
    }

    private void processFile(Path path) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(path);

            for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                for (MethodDeclaration method : clazz.getMethods()) {

                    Optional<FieldDeclaration> matchedField =
                            findMatchingField(method, clazz);

                    if (!matchedField.isPresent())
                        continue;

                    FieldDeclaration field = matchedField.get();

                    if (isTrivialGetter(method, clazz)) {

                        if (!field.isAnnotationPresent("Getter")) {
                            field.addAnnotation("Getter");
                            cu.addImport("lombok.Getter");
                        }

                        method.remove();
                        reportEntries.add("Getter -> field: "
                                + clazz.getNameAsString()
                                + "." + field.getVariable(0).getNameAsString());

                    } else if (isTrivialSetter(method, clazz)) {

                        if (!field.isAnnotationPresent("Setter")) {
                            field.addAnnotation("Setter");
                            cu.addImport("lombok.Setter");
                        }

                        method.remove();
                        reportEntries.add("Setter -> field: "
                                + clazz.getNameAsString()
                                + "." + field.getVariable(0).getNameAsString());
                    }
                }

            }

            if (!reportEntries.isEmpty()) {
                // Backup file
                Path backup = Paths.get(path.toString() + ".bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
                Files.write(path, cu.toString().getBytes());
            }

        } catch (Exception e) {
            getLog().error("Failed processing " + path, e);
        }
    }

    private Optional<FieldDeclaration> findMatchingField(
            MethodDeclaration method,
            ClassOrInterfaceDeclaration clazz) {

        String methodName = method.getNameAsString();

        String fieldName = null;

        if (methodName.startsWith("get") && methodName.length() > 3) {
            fieldName = decapitalize(methodName.substring(3));
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            fieldName = decapitalize(methodName.substring(2));
        }

        if (methodName.startsWith("set") && methodName.length() > 3) {
            fieldName = decapitalize(methodName.substring(3));
        }

        if (fieldName == null)
            return Optional.empty();

        return clazz.getFieldByName(fieldName);
    }

    private boolean isTrivialGetter(MethodDeclaration method, ClassOrInterfaceDeclaration clazz) {
        String methodName = method.getNameAsString();
        if ((methodName.startsWith("get") || methodName.startsWith("is"))
                && method.getParameters().isEmpty()) {
            boolean basic = methodName.startsWith("is"); // TODO zmien nazwe
            if (!method.getBody().isPresent()) return false;
            if (method.getBody().get().getStatements().size() != 1) return false;

            if (!(method.getBody().get().getStatement(0) instanceof ReturnStmt))
                return false;

            ReturnStmt ret = (ReturnStmt) method.getBody().get().getStatement(0);
            if (!ret.getExpression().isPresent()) return false;

            String fieldName = basic ? decapitalize(methodName.substring(2)) : decapitalize(methodName.substring(3));
            Optional<FieldDeclaration> field = clazz.getFieldByName(fieldName);
            return field.isPresent() && ret.getExpression().get().isNameExpr()
                    && ((NameExpr) ret.getExpression().get()).getNameAsString().equals(fieldName);
        } else {
            return false;
        }
    }

    private boolean isTrivialSetter(MethodDeclaration method, ClassOrInterfaceDeclaration clazz) {
        if (!method.getNameAsString().startsWith("set") || method.getParameters().size() != 1)
            return false;

        if (!method.getBody().isPresent()) return false;
        if (method.getBody().get().getStatements().size() != 1) return false;

        if (!(method.getBody().get().getStatement(0) instanceof ExpressionStmt))
            return false;

        ExpressionStmt stmt = (ExpressionStmt) method.getBody().get().getStatement(0);
        if (!(stmt.getExpression() instanceof AssignExpr)) return false;

        AssignExpr assign = (AssignExpr) stmt.getExpression();
        String fieldName = decapitalize(method.getNameAsString().substring(3));
        return assign.getTarget().isFieldAccessExpr() || assign.getTarget().isNameExpr()
                && assign.getTarget().asNameExpr().getNameAsString().equals(fieldName)
                && assign.getValue().isNameExpr()
                && assign.getValue().asNameExpr().getNameAsString()
                .equals(method.getParameter(0).getNameAsString());
    }

    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private void generateHtmlReport() {
        if (reportEntries.isEmpty()) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile))) {
            writer.write("<html><head><title>Lombok Refactor Report</title></head><body>");
            writer.write("<h1>Lombok Refactor Report</h1>");
            writer.write("<ul>");
            for (String entry : reportEntries) {
                writer.write("<li>" + entry + "</li>");
            }
            writer.write("</ul>");
            writer.write("</body></html>");
        } catch (IOException e) {
            getLog().error("Failed to write report", e);
        }
    }

}