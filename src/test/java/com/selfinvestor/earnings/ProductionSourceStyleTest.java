package com.selfinvestor.earnings;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSourceStyleTest {

    @Test
    void productionMethodsHaveJavaDocAndConditionalBranchesUseBlocks() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<Path> sources;
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        List<String> violations = new ArrayList<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
                    files.getJavaFileObjectsFromPaths(sources));
            DocTrees docs = DocTrees.instance(task);
            task.parse().forEach(unit -> new TreePathScanner<Void, Void>() {
                @Override
                public Void visitMethod(MethodTree method, Void unused) {
                    if (docs.getDocCommentTree(getCurrentPath()) == null) {
                        violations.add(location(unit, method) + " missing JavaDoc: " + method.getName());
                    }
                    return super.visitMethod(method, unused);
                }

                @Override
                public Void visitIf(IfTree conditional, Void unused) {
                    if (!(conditional.getThenStatement() instanceof BlockTree)) {
                        violations.add(location(unit, conditional) + " if branch must use braces");
                    }
                    Tree otherwise = conditional.getElseStatement();
                    if (otherwise != null && !(otherwise instanceof BlockTree) && !(otherwise instanceof IfTree)) {
                        violations.add(location(unit, conditional) + " else branch must use braces");
                    }
                    return super.visitIf(conditional, unused);
                }

                private String location(com.sun.source.tree.CompilationUnitTree source, Tree tree) {
                    long position = docs.getSourcePositions().getStartPosition(source, tree);
                    long line = source.getLineMap().getLineNumber(position);
                    return source.getSourceFile().getName() + ":" + line;
                }
            }.scan(unit, null));
        }
        assertThat(violations).isEmpty();
    }
}
