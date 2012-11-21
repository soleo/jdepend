package jdependFast.framework;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * The <code>FileManager</code> class is responsible for extracting 
 * Java class files (<code>.class</code> files) from a collection of 
 * registered directories.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class FileManager_T{
	public class Semaphore
	{
		private int sem = 0;
		private FileManager_T par;
		public Semaphore(FileManager_T p)
		{
			par = p;
		}
		public synchronized void increase()
	    {
			sem++;
			
	    }
		public synchronized void decrease()
	    {
			sem--;
			if (sem == 0)
			{
				synchronized(par)
				{
					par.notify();
				}
			}
	    }
	}
    private ArrayList directories;
    public boolean acceptInnerClasses;
    private List<Future> fut = new ArrayList();
    private Semaphore sem;
    public void  add(Future f)
    {
    	synchronized(fut){
    		fut.add(f);
    	}
    }
    public FileManager_T() {
        directories = new ArrayList();
        acceptInnerClasses = true;
        sem = new Semaphore(this);
    }

    /**
     * Determines whether inner classes should be collected.
     * 
     * @param b <code>true</code> to collect inner classes; 
     *          <code>false</code> otherwise.
     */
    public void acceptInnerClasses(boolean b) {
        acceptInnerClasses = b;
    }

    public void addDirectory(String name) throws IOException {

        File directory = new File(name);
        
        
	        if (directory.isDirectory() || acceptJarFile(directory)) {
	            directories.add(directory);
	        } else {
	            throw new IOException("Invalid directory or JAR file: " + name);
	        }
        
    }

    public boolean acceptFile(File file) {
        return acceptClassFile(file) || acceptJarFile(file);
    }

    public boolean acceptClassFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        return acceptClassFileName(file.getName());
    }

    public boolean acceptClassFileName(String name) {

        if (!acceptInnerClasses) {
            if (name.toLowerCase().indexOf("$") > 0) {
                return false;
            }
        }

        if (!name.toLowerCase().endsWith(".class")) {
            return false;
        }

        return true;
    }

    public boolean acceptJarFile(File file) {
        return isJar(file) || isZip(file) || isWar(file);
    }

    private boolean isWar(File file) {
        return existsWithExtension(file, ".war");
    }

    private boolean isZip(File file) {
        return existsWithExtension(file, ".zip");
    }
 
    private boolean isJar(File file) {
        return existsWithExtension(file, ".jar");
    }

    private boolean existsWithExtension(File file, String extension) {
        return file.isFile() &&
            file.getName().toLowerCase().endsWith(extension);
    }
    
    public  Collection extractFiles() throws InterruptedException, ExecutionException, IOException {

        ExecutorService es = JDepend_T.getExecutor();
        for (Iterator i = directories.iterator(); i.hasNext();) {
            fut.add(es.submit(new Worker((File)i.next())));
        }
        synchronized(this) {
            this.wait();
        }
        es.shutdown();
        Collection classes = new ArrayList();
        for(Future<Collection<JavaClass_T>> item:fut)
        {
        	Collection<JavaClass_T> temp = item.get();
        	for (JavaClass_T f:temp)
        	{
        		classes.add(f);
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
    	ClassFileParser_T parser = new ClassFileParser_T(JDepend_T.filter);
        if (acceptClassFile(file)) {
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
        } else if (acceptJarFile(file)) {

            JarFile jarFile = new JarFile(file);
            Collection result = buildClasses(jarFile, parser);
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
    public Collection buildClasses(JarFile file, ClassFileParser_T parser) throws IOException {

        Collection javaClasses = new ArrayList();

        Enumeration entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = (ZipEntry) entries.nextElement();
            if (acceptClassFileName(e.getName())) {
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
    
    public class Worker implements Callable{
    	private File job;
    	public Worker(File f)
    	{
    		job =f;
    	}
    	@Override
    	public Object call() throws Exception {
    		  //System.out.println("worker: "+ Thread.currentThread().getId());
    		sem.increase();
    		Collection<File> files = new TreeSet<File>();
    		collectFiles(job, files);
    		Collection classes = new ArrayList();
    		for (File f : files)
	        {
	        	 classes.addAll(buildClasses(f));
	        }
    		sem.decrease();
    		return classes;
    	}
    	
    	@SuppressWarnings("unchecked")
    	private void collectFiles(File item, Collection files) {
            if (item.isFile()) {

                addFile(item, files);

            } else {

                String[] directoryFiles = item.list();

                for (int i = 0; i < directoryFiles.length; i++) {

                    File file = new File(item, directoryFiles[i]);
                    if (acceptFile(file)) {
                    	addFile(file, files);
                    } else if (file.isDirectory()) {
                    	add(JDepend_T.getExecutor().submit(new Worker(file)));
                    }
                }
            }
        }
    	private void addFile(File f, Collection files) {
    	    if (!files.contains(f)) {
    	        files.add(f);
    	    }
    	}
    	  public boolean acceptFile(File file) {
    	        return acceptClassFile(file) || acceptJarFile(file);
    	    }
    	  public boolean acceptClassFile(File file) {
    	        if (!file.isFile()) {
    	            return false;
    	        }
    	        return acceptClassFileName(file.getName());
    	    }

    	    public boolean acceptClassFileName(String name) {

    	        if (!acceptInnerClasses) {
    	            if (name.toLowerCase().indexOf("$") > 0) {
    	                return false;
    	            }
    	        }

    	        if (!name.toLowerCase().endsWith(".class")) {
    	            return false;
    	        }

    	        return true;
    	    }
    	    public boolean acceptJarFile(File file) {
    	        return isJar(file) || isZip(file) || isWar(file);
    	    }
    	    private boolean isWar(File file) {
    	        return existsWithExtension(file, ".war");
    	    }

    	    private boolean isZip(File file) {
    	        return existsWithExtension(file, ".zip");
    	    }
    	 
    	    private boolean isJar(File file) {
    	        return existsWithExtension(file, ".jar");
    	    }
    	    private boolean existsWithExtension(File file, String extension) {
    	        return file.isFile() &&
    	            file.getName().toLowerCase().endsWith(extension);
    	    }
    	
    }
    
}