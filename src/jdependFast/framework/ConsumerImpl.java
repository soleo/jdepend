package jdependFast.framework;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConsumerImpl implements Consumer{

	private BlockingQueue<Item> itemQueue = new LinkedBlockingQueue<Item>();
	
	private List<ItemProcessor> jobList = new LinkedList<ItemProcessor>();
	
	private ExecutorService executorService = Executors.newCachedThreadPool();
	
	private volatile boolean shutdownCalled = false;
	
	public ConsumerImpl(int poolSize){
		for(int i = 0; i < poolSize; i++){
			ItemProcessor jobThread = new ItemProcessor(itemQueue);
			jobList.add(jobThread);
			executorService.submit(jobThread);
		}
	}
	public boolean consume(Item j) {
		// TODO Auto-generated method stub
		if(!shutdownCalled){
			try{
				itemQueue.put(j);
			}catch(InterruptedException ie){
				Thread.currentThread().interrupt();
				return false;
			}
			return true;
		}else{
			return false;
		}
		//return false;
	}

	public void finishConsumption() {
		// TODO Auto-generated method stub
		//for(ItemProcessor j : jobList){
			//j.cancelExecution();
		//}
		executorService.shutdown();
		
		 try {
		     // Wait a while for existing tasks to terminate
		     if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
		    	 executorService.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		       if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
		           System.err.println("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
			   executorService.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
	}

}
