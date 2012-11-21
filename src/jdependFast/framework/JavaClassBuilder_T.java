package jdependFast.framework;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * The <code>JavaClassBuilder</code> builds <code>JavaClass</code> 
 * instances from .class, .jar, .war, or .zip files.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class JavaClassBuilder_T {

    private AbstractParser_T parser;
    private FileManager_T fileManager;
    
    public JavaClassBuilder_T() {
        this(new ClassFileParser_T(), new FileManager_T());
    }

    public JavaClassBuilder_T(FileManager_T fm) {
        this(new ClassFileParser_T(), fm);
    }
    
    
    public JavaClassBuilder_T(AbstractParser_T parser, FileManager_T fm) {
        this.parser = parser;
        this.fileManager = fm;
    }

    public int countClasses() throws InterruptedException, ExecutionException, IOException {
        AbstractParser_T counter = new AbstractParser_T() {

            @Override
			public JavaClass_T parse(InputStream is) {
                return new JavaClass_T("");
            }
        };

        JavaClassBuilder_T builder = new JavaClassBuilder_T(counter, fileManager);
        Collection classes = builder.build();
        return classes.size();
    }

    /**
     * Builds the <code>JavaClass</code> instances.
     * 
     * @return Collection of <code>JavaClass</code> instances.
     * @throws ExecutionException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public Collection build() throws InterruptedException, ExecutionException, IOException {

        return fileManager.extractFiles();
    }
    

}
