package jdependFast.framework;

import java.io.*;
import java.util.*;

/**
 * The <code>AbstractParser</code> class is the base class 
 * for classes capable of parsing files to create a 
 * <code>JavaClass</code> instance.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public abstract class AbstractParser_T {

    private ArrayList parseListeners;
    private PackageFilter_T filter;
    public static boolean DEBUG = false;


    public AbstractParser_T() {
        this(new PackageFilter_T());
    }

    public AbstractParser_T(PackageFilter_T filter) {
        setFilter(filter);
        parseListeners = new ArrayList();
    }

    public void addParseListener(ParserListener_T listener) {
        parseListeners.add(listener);
    }

    /**
     * Registered parser listeners are informed that the resulting
     * <code>JavaClass</code> was parsed.
     */
    public abstract JavaClass_T parse(InputStream is) throws IOException;

    /**
     * Informs registered parser listeners that the specified
     * <code>JavaClass</code> was parsed.
     * 
     * @param jClass Parsed Java class.
     */
    protected void onParsedJavaClass(JavaClass_T jClass) {
        for (Iterator i = parseListeners.iterator(); i.hasNext();) {
            ((ParserListener_T) i.next()).onParsedJavaClass(jClass);
        }
    }

    protected PackageFilter_T getFilter() {
        if (filter == null) {
            setFilter(new PackageFilter_T());
        }
        return filter;
    }

    protected void setFilter(PackageFilter_T filter) {
        this.filter = filter;
    }

    protected void debug(String message) {
        if (DEBUG) {
            System.err.println(message);
        }
    }
}