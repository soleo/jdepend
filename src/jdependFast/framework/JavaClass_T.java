package jdependFast.framework;

import java.util.*;

/**
 * The <code>JavaClass</code> class represents a Java 
 * class or interface.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class JavaClass_T// implements Runnable 
{
	
	private JDepend_T jdt;
	
    private String className;
    private String packageName;
    private boolean isAbstract;
    private HashMap imports;
    private String sourceFile;


    public JavaClass_T(String name) {
        className = name;
        packageName = "default";
        isAbstract = false;
        imports = new HashMap();
        sourceFile = "Unknown";
    }

    public void setName(String name) {
        className = name;
    }

    public String getName() {
        return className;
    }

    public void setPackageName(String name) {
        packageName = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setSourceFile(String name) {
        sourceFile = name;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public Collection getImportedPackages() {
        return imports.values();
    }

    public void addImportedPackage(JavaPackage_T jPackage) {
        if (!jPackage.getName().equals(getPackageName())) {
            imports.put(jPackage.getName(), jPackage);
        }
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void isAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    @Override
	public boolean equals(Object other) {

        if (other instanceof JavaClass_T) {
            JavaClass_T otherClass = (JavaClass_T) other;
            return otherClass.getName().equals(getName());
        }

        return false;
    }

    @Override
	public int hashCode() {
        return getName().hashCode();
    }

    public static class ClassComparator implements Comparator {

        @Override
		public int compare(Object a, Object b) {
            JavaClass_T c1 = (JavaClass_T) a;
            JavaClass_T c2 = (JavaClass_T) b;

            return c1.getName().compareTo(c2.getName());
        }
    }
/*
	public void run() {
		
        if (!jdt.getFilter().accept(packageName)) {
            return;
        }
        
        JavaPackage_T clazzPackage = jdt.addPackage(packageName);
        clazzPackage.addClass(this);

        Collection imports = this.imports.values();
        for (Iterator i = imports.iterator(); i.hasNext();) {
            JavaPackage_T importedPackage = (JavaPackage_T)i.next();
            importedPackage = jdt.addPackage(importedPackage.getName());
            clazzPackage.dependsUpon(importedPackage);
        }
	}
*/
	public void setJdt(JDepend_T jdt) {
		this.jdt = jdt;
	}

	public JDepend_T getJdt() {
		return jdt;
	}
}
