package practice.nio.SwingNIOClientServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * A server app showing the use of Java 7 NIO.2 async socket channel APIs.
 * 
 * Server receives a Telnet listing request from an NIO client and serves them via Guava's listenable 
 * futures asynchronously.
 * 
 * //TODO: don't send the response if client has died. 
 *	 
 * @author Nitin
 *
 */
public class NIO2NetStatDataProvider implements Runnable {
	
	private volatile boolean stopped;
	private Map<String, AsynchronousSocketChannel> activeConnections = new ConcurrentHashMap<String, AsynchronousSocketChannel>();

	@Override
	public void run() {

		try {
			
			//We don't need an async channel group if we are using listenable futures
			AsynchronousChannelGroup channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
			AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
			
			ListeningExecutorService threadPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

			//
			// Bind the server to this port
			//
			serverSocketChannel.bind(new InetSocketAddress(AppConstants.SERVER_PORT));
			
			System.out.println("Server listening on port " + AppConstants.SERVER_PORT);
			
			while(!stopped)
			{
				Future<AsynchronousSocketChannel> future = serverSocketChannel.accept();
				
				//Will be non null on return
				try {
					
					AsynchronousSocketChannel clientConnection = future.get();
					String clientAddress = clientConnection.getRemoteAddress().toString();
					System.out.println("\n\n####### [Connection Event]: Server got connected to client from " + clientAddress);
					
					activeConnections.put(clientAddress, clientConnection);
					
					// A new buffer for each different client connection
					ByteBuffer buffer = ByteBuffer.allocate(1024);
					registerForClientRequests(clientAddress, clientConnection, buffer, new ClientRequestsReadHandler(clientConnection, threadPool));
					
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} catch (ExecutionException e1) {
					e1.printStackTrace();
				}
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Invoked on a channel group pool thread. Once client request is parsed, serves the request on 
	 * a Guava listenable pool. 
	 */
	private class ClientRequestsReadHandler implements CompletionHandler<Integer, ByteBuffer>
	{
		
		private final AsynchronousSocketChannel clientConnection;
		private final ListeningExecutorService threadPool;
		private int requestsServedSoFar = 0;
		private final ByteBuffer writeBuffer;

		public ClientRequestsReadHandler(AsynchronousSocketChannel clientConnection, ListeningExecutorService threadPool) {
			this.clientConnection = clientConnection;
			this.threadPool = threadPool;
			writeBuffer = ByteBuffer.allocate(1024);
		}

		@Override
		public void completed(Integer numBytesRead, ByteBuffer readBuffer) {
			
				
			try {
				String clientAddress = clientConnection.getRemoteAddress().toString();
				System.out.println("\n\n---- >>>> Recieved a request from client , #bytes = "+ numBytesRead +  ",  " + clientAddress);
				

				readBuffer.flip();
				String clientRequst = StandardCharsets.US_ASCII.decode(readBuffer).toString();
				String[] multipleClientRequests = clientRequst.split(";");
				System.out.println("Num requests in block = " + multipleClientRequests.length);
				for(String req : multipleClientRequests)
				{
					if(clientRequst.contains("Netstat")){
						requestsServedSoFar++;
						String requestID = "REQ#"+requestsServedSoFar;
						serveNetStatRequest(requestID, clientConnection, writeBuffer, threadPool);
						System.out.println("Client at '" + clientAddress + "' sent a 'netstat', serving now " + requestID);
					}
					else
					{
						System.out.println("Client at '"+ clientAddress +"' sent '" + clientRequst + "', ignoring.");
						break;
					}
				}
				
				registerForClientRequests(clientAddress, clientConnection, readBuffer, this);
			} 
			catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			exc.printStackTrace();
		}
	}
	
	/**
	 * Blocking call to read netstat data from Windows command line.
	 */
	private String readNetStatData() {
		
		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/C", "netstat");
		StringBuilder buff = new StringBuilder();
		Process process = null;
		InputStream in = null;
		try {
			process = builder.start();

			in = process.getInputStream();
			InputStreamReader data = new InputStreamReader(in);
			BufferedReader lineReader = new BufferedReader(data);
			
			String line = null;
			
			while((line = lineReader.readLine()) != null)
			{
				System.out.println(line);
				buff.append(line);
			}
			
			//process.waitFor();
			return buff.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
		finally
		{
			if(in != null)
			{
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(process != null)
				process.destroy();
		}
	}
	
	/**
	 * Serve in a different thread. 
	 */
	public void serveNetStatRequest(final String requestID, final AsynchronousSocketChannel clientConnection, final ByteBuffer buffer, final ListeningExecutorService threadPool) {
		
		//threadPool.submit(new Runnable() {//TODO Spawning a new thread for each request.. We could have used a pool for this
		new Thread(){
			
			@Override
			public void run() {
				
				String netStatData = readNetStatData();
				if(netStatData != null && clientConnection.isOpen())
				{
					byte[] data = netStatData.getBytes();
					
					if(data.length > buffer.capacity())
					{
						int remainingBytes = data.length;
						int offset = 0;
						int numBytesToWrite = buffer.capacity();
						while(remainingBytes > 0)
						{
							buffer.clear();
							buffer.put(data, offset, numBytesToWrite);
							buffer.flip();
							
							Future<Integer> future = null;
							try
							{
								future = clientConnection.write(buffer);
							}
							catch(Exception ex)
							{
								//ex.printStackTrace();
								try {
									System.out.println("Client connection at " + clientConnection.getRemoteAddress() + " seems closed, can't serve request "+ requestID);
								} catch (IOException e) {
									//e.printStackTrace();
								}
								return;
							}
							
							final CountDownLatch latch = new CountDownLatch(1);
							ListenableFuture<Integer> listenableFuture = JdkFutureAdapters.listenInPoolThread(future, threadPool);
							Futures.addCallback(listenableFuture, new FutureCallback<Integer>() {

								@Override
								public void onSuccess(Integer successfulBytesSent) {
									//so we can send more bytes now
									try {
										System.out.println(requestID + " -- server sent " + successfulBytesSent + " bytes to client " + clientConnection.getRemoteAddress());
									} catch (IOException e) {
										e.printStackTrace();
									}
									latch.countDown();
								}

								@Override
								public void onFailure(Throwable t) {
									t.printStackTrace();
									latch.countDown();
								}
							});
							
							//
							//	Wait here until we get a successful send ack. via the future.
							//
							try {
								latch.await();
								
								//Prepare to send more data
								offset = offset + numBytesToWrite;
								remainingBytes = data.length - offset;
								if(remainingBytes > buffer.capacity())
									numBytesToWrite = buffer.capacity();
								else
									numBytesToWrite = remainingBytes;
								
							} 
							catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
					else
					{
						buffer.clear();
						buffer.put(data);
						buffer.flip();
						clientConnection.write(buffer);//, buffer, new ServerDataResponseDispatcher(clientConnection));
						try {
							System.out.println("Server sent " + data.length + " bytes to client " + clientConnection.getRemoteAddress());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		}.start();
				
				
	}

	public static void registerForClientRequests(String clientAddress, AsynchronousSocketChannel clientConnection, ByteBuffer buffer, ClientRequestsReadHandler handler) {

		if(clientConnection.isOpen())
		{
			//register again to read requests from client
			buffer.clear();
			clientConnection.read(buffer, buffer, handler);
		}
		else
			System.out.println("Client at address '" + clientAddress + "' seems no longer online");
	}

	
	//POC
	public static void main(String[] args) throws IOException, InterruptedException {
		
		new Thread(new NIO2NetStatDataProvider()).start();
	}

}
