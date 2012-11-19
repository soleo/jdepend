package jdependFast.framework;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * The <code>FileManager</code> class is responsible for extracting 
 * Java class files (<code>.class</code> files) from a collection of 
 * registered directories.
 * 
 * @author <b>Mike Clark</b>
 * @author Clarkware Consulting, Inc.
 */

public class FileManager_T{

    private ArrayList directories;
    private boolean acceptInnerClasses;
    private Consumer consumer = new ConsumerImpl(10);

    public FileManager_T() {
        directories = new ArrayList();
        acceptInnerClasses = true;
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

    public  Collection extractFiles() {

        Collection<File> files = new TreeSet();
       
        for (Iterator i = directories.iterator(); i.hasNext();) {
            File directory = (File)i.next();
            //ArrayList<File> dir;
            collectFiles(directory, files);
        }
        //System.out.println(files.toArray().toString());
        for (File f : files) {
            System.out.println(f.getName());
            //System.out.println(element.getIvar2());
        }
        return files;
    }

    class CollectJob implements Item{
    	File dir;
    	Collection files;
    	CollectJob(File dir, Collection files){
    		this.dir = dir;
    		this.files = files;
    	}
		@Override
		public void process() {
			// TODO Auto-generated method stub
			if(dir.isFile()){
				addFile(dir, files);
				System.out.println(Thread.currentThread().getName() + " consuming : "+dir);
			}else{
				String[] directoryFiles = dir.list();
				for (int i = 0; i < directoryFiles.length; i++) {

	                File file = new File(dir, directoryFiles[i]);
	                if (acceptFile(file)) {
	                    addFile(file, files);
	                } else if (file.isDirectory()) {
	                    //return true; 
	                	System.out.println(Thread.currentThread().getName() +" : dir :"+file.toString());
	                	//consumer.consume(new CollectJob(file, files));
	                	
	                		  Stack<File> stack = new Stack<File>();
	                		  stack.push(file);
	                		  while(!stack.isEmpty()) {
	                		    File child = stack.pop();
	                		    if (child.isDirectory()) {
	                		      for(File f : child.listFiles()) stack.push(f);
	                		    } else if (child.isFile() && acceptFile(child)) {
	                		      System.out.println(Thread.currentThread().getName() + " :"+ child.getPath());
	                		      addFile(child, files);
	                		    }
	                		  }
	                	
	                }
	            }
			}
			
		}
    	
    }
    private void collectFiles(File directory, Collection files) {
    	
        if (directory.isFile()) {

            addFile(directory, files);
            
        } else {
        	// should be dir, use a thread to deal with it
        	
            String[] directoryFiles = directory.list();
            
            for (int i = 0; i < directoryFiles.length; i++) {

                File file = new File(directory, directoryFiles[i]);
                if (acceptFile(file)) {
                    addFile(file, files);
                } else if (file.isDirectory()) {
                    //return true; 
                	consumer.consume(new CollectJob(file, files));
                	//collectFiles(file, files);
                }
            }
        }
        
        consumer.finishConsumption();
		
    }
    // sharing files Collection
    private  synchronized void addFile(File f, Collection files) {
        if (!files.contains(f)) {
            files.add(f);
        }
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