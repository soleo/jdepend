package jdependFast.framework;

import java.util.Comparator;

/**
 * The <code>PackageComparator</code> class is a <code>Comparator</code>
 * used to compare two <code>JavaPackage</code> instances for order using a
 * sorting strategy.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class PackageComparator_T implements Comparator {

    private PackageComparator_T byWhat;

    private static PackageComparator_T byName;
    static {
        byName = new PackageComparator_T();
    }

    public static PackageComparator_T byName() {
        return byName;
    }

    private PackageComparator_T() {
    }

    public PackageComparator_T(PackageComparator_T byWhat) {
        this.byWhat = byWhat;
    }

    public PackageComparator_T byWhat() {
        return byWhat;
    }

    @Override
	public int compare(Object p1, Object p2) {

        JavaPackage_T a = (JavaPackage_T) p1;
        JavaPackage_T b = (JavaPackage_T) p2;

        if (byWhat() == byName()) {
            return a.getName().compareTo(b.getName());
        }

        return 0;
    }
}