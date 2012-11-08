package jdependFast.framework;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The <code>JDepend</code> class analyzes directories of Java class files 
 * and generates the following metrics for each Java package.
 * <p>
 * <ul>
 * <li>Afferent Coupling (Ca)
 * <p>
 * The number of packages that depend upon the classes within the analyzed
 * package.
 * </p>
 * </li>
 * <li>Efferent Coupling (Ce)
 * <p>
 * The number of packages that the classes in the analyzed package depend upon.
 * </p>
 * </li>
 * <li>Abstractness (A)
 * <p>
 * The ratio of the number of abstract classes (and interfaces) in the analyzed
 * package to the total number of classes in the analyzed package.
 * </p>
 * <p>
 * The range for this metric is 0 to 1, with A=0 indicating a completely
 * concrete package and A=1 indicating a completely abstract package.
 * </p>
 * </li>
 * <li>Instability (I)
 * <p>
 * The ratio of efferent coupling (Ce) to total coupling (Ce + Ca) such that I =
 * Ce / (Ce + Ca).
 * </p>
 * <p>
 * The range for this metric is 0 to 1, with I=0 indicating a completely stable
 * package and I=1 indicating a completely instable package.
 * </p>
 * </li>
 * <li>Distance from the Main Sequence (D)
 * <p>
 * The perpendicular distance of a package from the idealized line A + I = 1. A
 * package coincident with the main sequence is optimally balanced with respect
 * to its abstractness and stability. Ideal packages are either completely
 * abstract and stable (x=0, y=1) or completely concrete and instable (x=1,
 * y=0).
 * </p>
 * <p>
 * The range for this metric is 0 to 1, with D=0 indicating a package that is
 * coincident with the main sequence and D=1 indicating a package that is as far
 * from the main sequence as possible.
 * </p>
 * </li>
 * <li>Package Dependency Cycle
 * <p>
 * Package dependency cycles are reported along with the paths of packages
 * participating in package dependency cycles.
 * </p>
 * </li>
 * </ul>
 * <p>
 * These metrics are hereafter referred to as the "Martin Metrics", as they are
 * credited to Robert Martin (Object Mentor Inc.) and referenced in the book
 * "Designing Object Oriented C++ Applications using the Booch Method", by
 * Robert C. Martin, Prentice Hall, 1995.
 * </p>
 * <p>
 * Example API use:
 * <p>
 * <blockquote>
 * 
 * <pre>
 * JDepend jdepend = new JDepend();
 * jdepend.addDirectory(&quot;/path/to/classes&quot;);
 * Collection packages = jdepend.analyze();
 * 
 * Iterator i = packages.iterator();
 * while (i.hasNext()) {
 *     JavaPackage jPackage = (JavaPackage) i.next();
 *     String name = jPackage.getName();
 *     int Ca = jPackage.afferentCoupling();
 *     int Ce = jPackage.efferentCoupling();
 *     float A = jPackage.abstractness();
 *     float I = jPackage.instability();
 *     float D = jPackage.distance();
 *     boolean b = jPackage.containsCycle();
 * }
 * </pre>
 * 
 * </blockquote>
 * </p>
 * <p>
 * This class is the data model used by the <code>jdepend.textui.JDepend</code>
 * and <code>jdepend.swingui.JDepend</code> views.
 * </p>
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class JDepend_T {

    private HashMap packages;
    private FileManager_T fileManager;
    private PackageFilter_T filter;
    private ClassFileParser_T parser;
    private JavaClassBuilder_T builder;
    private Collection components;
    private ExecutorService executor;

    public ExecutorService getExecutor() {
		return executor;
	}

	public JDepend_T() {
        this(new PackageFilter_T());
    }

    public JDepend_T(PackageFilter_T filter) {
    	
    	executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        setFilter(filter);

        this.packages = new HashMap();
        this.fileManager = new FileManager_T();

        this.parser = new ClassFileParser_T(filter);
        this.builder = new JavaClassBuilder_T(parser, fileManager);

        PropertyConfigurator_T config = new PropertyConfigurator_T();
        addPackages(config.getConfiguredPackages());
        analyzeInnerClasses(config.getAnalyzeInnerClasses());
    }
    
    /**
     * Analyzes the registered directories and returns the collection of
     * analyzed packages.
     * 
     * @return Collection of analyzed packages.
     */
    public Collection analyze() {

        Collection classes = builder.build();
        
        for (Iterator i = classes.iterator(); i.hasNext();) {
            analyzeClass((JavaClass_T)i.next());
        }

        return getPackages();
    }

    /**
     * Adds the specified directory name to the collection of directories to be
     * analyzed.
     * 
     * @param name Directory name.
     * @throws IOException If the directory is invalid.
     */
    public void addDirectory(String name) throws IOException {
        fileManager.addDirectory(name);
    }
    
    /**
     * Sets the list of components.
     * 
     * @param components Comma-separated list of components.
     */
    public void setComponents(String components) {
        this.components = new ArrayList();
        StringTokenizer st = new StringTokenizer(components, ",");
        while (st.hasMoreTokens()) {
            String component = st.nextToken();
            this.components.add(component);
        }
    }

    /**
     * Determines whether inner classes are analyzed.
     * 
     * @param b <code>true</code> to analyze inner classes; 
     *          <code>false</code> otherwise.
     */
    public void analyzeInnerClasses(boolean b) {
        fileManager.acceptInnerClasses(b);
    }

    /**
     * Returns the collection of analyzed packages.
     * 
     * @return Collection of analyzed packages.
     */
    public Collection getPackages() {
        return packages.values();
    }

    /**
     * Returns the analyzed package of the specified name.
     * 
     * @param name Package name.
     * @return Package, or <code>null</code> if the package was not analyzed.
     */
    public JavaPackage_T getPackage(String name) {
        return (JavaPackage_T)packages.get(name);
    }

    /**
     * Returns the number of analyzed Java packages.
     * 
     * @return Number of Java packages.
     */
    public int countPackages() {
        return getPackages().size();
    }

    /**
     * Returns the number of registered Java classes to be analyzed.
     * 
     * @return Number of classes.
     */
    public int countClasses() {
        return builder.countClasses();
    }

    /**
     * Indicates whether the packages contain one or more dependency cycles.
     * 
     * @return <code>true</code> if one or more dependency cycles exist.
     */
    public boolean containsCycles() {
        for (Iterator i = getPackages().iterator(); i.hasNext();) {
            JavaPackage_T jPackage = (JavaPackage_T)i.next();
            if (jPackage.containsCycle()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates whether the analyzed packages match the specified 
     * dependency constraint.
     * 
     * @return <code>true</code> if the packages match the dependency
     *         constraint
     */
    public boolean dependencyMatch(DependencyConstraint_T constraint) {
        return constraint.match(getPackages());
    }

    /**
     * Registers the specified parser listener.
     * 
     * @param listener Parser listener.
     */
    public void addParseListener(ParserListener_T listener) {
        parser.addParseListener(listener);
    }

    /**
     * Adds the specified Java package name to the collection of analyzed
     * packages.
     * 
     * @param name Java package name.
     * @return Added Java package.
     */
    public JavaPackage_T addPackage(String name) {
        name = toComponent(name);
        JavaPackage_T pkg = (JavaPackage_T)packages.get(name);
        if (pkg == null) {
            pkg = new JavaPackage_T(name);
            addPackage(pkg);
        }

        return pkg;
    }

    private String toComponent(String packageName) {
        if (components != null) {
            for (Iterator i = components.iterator(); i.hasNext();) {
                String component = (String)i.next();
                if (packageName.startsWith(component + ".")) {
                    return component;
                }
            }
        }
        return packageName;
    }

    /**
     * Adds the specified collection of packages to the collection 
     * of analyzed packages.
     * 
     * @param packages Collection of packages.
     */
    public void addPackages(Collection packages) {
        for (Iterator i = packages.iterator(); i.hasNext();) {
            JavaPackage_T pkg = (JavaPackage_T)i.next();
            addPackage(pkg);
        }
    }

    /**
     * Adds the specified Java package to the collection of 
     * analyzed packages.
     * 
     * @param pkg Java package.
     */
    public void addPackage(JavaPackage_T pkg) {
        if (!packages.containsValue(pkg)) {
            packages.put(pkg.getName(), pkg);
        }
    }

    public PackageFilter_T getFilter() {
        if (filter == null) {
            filter = new PackageFilter_T();
        }

        return filter;
    }

    public void setFilter(PackageFilter_T filter) {
        if (parser != null) {
            parser.setFilter(filter);
        }
        this.filter = filter;
    }

    private void analyzeClass(JavaClass_T clazz) {

        String packageName = clazz.getPackageName();

        if (!getFilter().accept(packageName)) {
            return;
        }

        JavaPackage_T clazzPackage = addPackage(packageName);
        clazzPackage.addClass(clazz);

        Collection imports = clazz.getImportedPackages();
        for (Iterator i = imports.iterator(); i.hasNext();) {
            JavaPackage_T importedPackage = (JavaPackage_T)i.next();
            importedPackage = addPackage(importedPackage.getName());
            clazzPackage.dependsUpon(importedPackage);
        }
    }
}
