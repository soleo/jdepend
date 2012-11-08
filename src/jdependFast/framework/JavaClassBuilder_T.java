package jdependFast.framework;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

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

    public int countClasses() {
        AbstractParser_T counter = new AbstractParser_T() {

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
     */
    public Collection build() {

        Collection classes = new ArrayList();

        for (Iterator i = fileManager.extractFiles().iterator(); i.hasNext();) {

            File nextFile = (File)i.next();

            try {

                classes.addAll(buildClasses(nextFile));

            } catch (IOException ioe) {
                System.err.println("\n" + ioe.getMessage());
            }
        }

        return classes;
    }

    /**
     * Builds the <code>JavaClass</code> instances from the 
     * specified file.
     * 
     * @param file Class or Jar file.
     * @return Collection of <code>JavaClass</code> instances.
     */
    public Collection buildClasses(File file) throws IOException {

        if (fileManager.acceptClassFile(file)) {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(file));
                JavaClass_T parsedClass = parser.parse(is);
                Collection javaClasses = new ArrayList();
                javaClasses.add(parsedClass);
                return javaClasses;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } else if (fileManager.acceptJarFile(file)) {

            JarFile jarFile = new JarFile(file);
            Collection result = buildClasses(jarFile);
            jarFile.close();
            return result;

        } else {
            throw new IOException("File is not a valid " + 
                ".class, .jar, .war, or .zip file: " + 
                file.getPath());
        }
    }

    /**
     * Builds the <code>JavaClass</code> instances from the specified 
     * jar, war, or zip file.
     * 
     * @param file Jar, war, or zip file.
     * @return Collection of <code>JavaClass</code> instances.
     */
    public Collection buildClasses(JarFile file) throws IOException {

        Collection javaClasses = new ArrayList();

        Enumeration entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = (ZipEntry) entries.nextElement();
            if (fileManager.acceptClassFileName(e.getName())) {
                InputStream is = null;
                try {
	                is = new BufferedInputStream(file.getInputStream(e));
                    JavaClass_T jc = parser.parse(is);
                    javaClasses.add(jc);
                } finally {
                    is.close();
                }
            }
        }

        return javaClasses;
    }
}
