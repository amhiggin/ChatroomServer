package main.java;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * Threadpool executor which can manage and execute a queue of threads.
 */

public class ThreadPoolExecutor {

	private static final long WAIT_TIME = 10; // 10 seconds
	private ExecutorService threadPool;
	private static final int NUMBER_OF_THREADS = 1000; // max number of threads

	public ThreadPoolExecutor() {
		try {
			initializeThreadPool();
		} catch (Exception e) {
			ChatroomServer.printServerMessageToConsole("Failed to initialise threadpool: " + e.getMessage());
		}
	}

	private void initializeThreadPool() throws Exception {
		try {
			this.threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			ChatroomServer.printServerMessageToConsole("Threadpool executor has initialised successfully");
		} catch (IllegalArgumentException e) {
			throw new Exception("Threadpool couldn't be initialised", e);
		}
	}

	public void submitTask(Socket clientSocket) throws Exception {
		try {
			ClientThread thread = new ClientThread(clientSocket);
			this.threadPool.execute(thread);
			ChatroomServer.printServerMessageToConsole("Submitted thread to threadpool");
		} catch (Exception e) {
			ChatroomServer
					.printServerMessageToConsole("Error occurred submitting task to threadpool: " + e.getMessage());
		}
	}

	public void shutdown() throws Exception {
		try {
			this.threadPool.shutdown();
			this.threadPool.awaitTermination(WAIT_TIME, TimeUnit.SECONDS);
			ChatroomServer.printServerMessageToConsole("Threadpool executor has terminated successfully");
		} catch (InterruptedException e) {
			throw new Exception("Threadpool failed to shut down correctly", e);
		}
	}

}
