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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.dsl.Disruptor;

public class NIO2ServerWithDisruptor  implements Runnable {
	
	private static final int NUM_THREADS_READ_COMPLETION_POOL = 2;
	private static final int NUM_THREADS_WRITE_COMPLETION_POOL = 3;
	private static final int RING_BUFFER_SIZE = 1024;
	private static final int NUM_THREADS_DISRUTOR_EVENT_HANDLERS = 6;
	private volatile boolean stopped;
	private Map<String, AsynchronousSocketChannel> activeConnections = new ConcurrentHashMap<String, AsynchronousSocketChannel>();

	@Override
	public void run() {

		try {
			
			ExecutorService readCompletionPool = Executors.newFixedThreadPool(NUM_THREADS_READ_COMPLETION_POOL, new ThreadFactory() {
				
				@Override
				public Thread newThread(Runnable r) {
					
					return new Thread(r, "[ReadCompletionPool]");
				}
			});
			
			AsynchronousChannelGroup channelGroupForReadCompletionEvents = AsynchronousChannelGroup.withThreadPool(readCompletionPool);
			
			AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroupForReadCompletionEvents);
			
			ListeningExecutorService partialWriteCompletionPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS_WRITE_COMPLETION_POOL, new ThreadFactory() {
				
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "[PartialWriteCompletionPool]");
				}
			}));
			
			Disruptor<ClientEvent> disruptor = new Disruptor<>(new EventFactory<ClientEvent>() {
					@Override
					public ClientEvent newInstance() {
						return new ClientEvent();
					}
				}, RING_BUFFER_SIZE, Executors.newFixedThreadPool(NUM_THREADS_DISRUTOR_EVENT_HANDLERS, new ThreadFactory() {
					
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, "[DisruptorEventPool]");
					}
				}));
			
			disruptor.handleEventsWith(new ClientRequestProcessor(disruptor, partialWriteCompletionPool));

			//
			// Bind the server to this port
			//
			serverSocketChannel.bind(new InetSocketAddress(AppConstants.SERVER_PORT));
			
			System.out.println("Server listening on port " + AppConstants.SERVER_PORT);
			
			//
			// Start the disruptor
			//
			disruptor.start();
			
			while(!stopped)
			{
				Future<AsynchronousSocketChannel> future = serverSocketChannel.accept();
				
				//Will be non null on return
				try {
					
					AsynchronousSocketChannel clientConnection = future.get();
					String clientAddress = clientConnection.getRemoteAddress().toString();
					System.out.println("\n\n####### "  + Thread.currentThread().getName() + " - [Connection Event]: Server got connected to client from " + clientAddress);
					
					activeConnections.put(clientAddress, clientConnection);
					
					//These read completion handlers will be called on "read completion pool"
					new ClientRequestsReader(clientAddress, clientConnection, disruptor, partialWriteCompletionPool).listen();
					
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
	
	private class ClientEvent
	{
		private String clientRequst;
		private String reqID;
		private AsynchronousSocketChannel clientConnection;
		private String clientAddress;
		private ByteBuffer writeBuffer;

		public void setClientRequest(String clientRequst) {
			this.clientRequst = clientRequst;
		}

		public String getClientRequest() {
			return clientRequst;
		}

		public String getRequestID() {
			return reqID;
		}

		public AsynchronousSocketChannel getClientConnection() {
			return clientConnection;
		}

		public ByteBuffer getWriteBuffer() {
			return writeBuffer;
		}

		public String getClientAddress() {
			return clientAddress;
		}

		public void setRequestID(String reqID) {
			this.reqID = reqID;
		}

		public void setClientAddress(String clientAddress) {
			this.clientAddress = clientAddress;
		}

		public void setClientChannel(AsynchronousSocketChannel clientConnection) {
			this.clientConnection = clientConnection;
		}

		public void setWriteBuffer(ByteBuffer writeBuffer) {
			this.writeBuffer = writeBuffer;
		}
	}
	
	private class ClientRequestsReader implements CompletionHandler<Integer, ByteBuffer>
	{
		private final AsynchronousSocketChannel clientConnection;
		private final ListeningExecutorService partialWriteCompletionPool;
		private final ByteBuffer writeBuffer;
		private final Disruptor<ClientEvent> disruptor;
		private final String clientAddress;
		private final ByteBuffer readBuffer;
		private int requestsServedSoFar = 0;
		
		public ClientRequestsReader(String clientAddress, AsynchronousSocketChannel clientConnection, Disruptor<ClientEvent> disruptor, ListeningExecutorService partialWriteCompletionPool) 
		{
			this.clientAddress = clientAddress;
			this.clientConnection = clientConnection;
			this.disruptor = disruptor;
			this.partialWriteCompletionPool = partialWriteCompletionPool;
			writeBuffer = ByteBuffer.allocate(1024);
			readBuffer = ByteBuffer.allocate(1024);
		}

		public void listen() 
		{
			if(clientConnection.isOpen())
			{
				//register to read requests from client
				readBuffer.clear();
				clientConnection.read(readBuffer, readBuffer, this);
			}
			else
				System.out.println(Thread.currentThread().getName() + " - Client at address '" + clientAddress + "' seems no longer online");
		}

		@Override
		public void completed(Integer numBytesRead, ByteBuffer readBuffer) {
			
				
			try {
				System.out.println("\n\n"  + Thread.currentThread().getName() + " - ---- >>>> Recieved a request from client , #bytes = "+ numBytesRead +  ",  " + clientAddress);
				
				requestsServedSoFar++;
				
				String reqID = "REQ#" + requestsServedSoFar;
				
				//TODO: same buffer access is not threadsafe, what if same client send multiple events in a short span
				disruptor.publishEvent(new ClientRequestTranslator(reqID, clientAddress, clientConnection, readBuffer, writeBuffer));
				
				//Register for next read completion event from same client
				listen();
			} 
			catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			exc.printStackTrace();
		}
	}
	
	private class ClientRequestTranslator implements EventTranslator<ClientEvent>
	{
		private final ByteBuffer readBuffer;
		private final String reqID;
		private final String clientAddress;
		private final AsynchronousSocketChannel clientConnection;
		private final ByteBuffer writeBuffer;

		public ClientRequestTranslator(String reqID, String clientAddress, AsynchronousSocketChannel clientConnection, ByteBuffer readBuffer, ByteBuffer writeBuffer) {
			this.reqID = reqID;
			this.clientAddress = clientAddress;
			this.clientConnection = clientConnection;
			this.readBuffer = readBuffer;
			this.writeBuffer = writeBuffer;
			System.out.println(Thread.currentThread().getName() + " - translated event for request " + reqID + " coming from address " + clientAddress);
		}

		@Override
		public void translateTo(ClientEvent event, long claimedSeq) {
			readBuffer.flip();
			String clientRequst = StandardCharsets.US_ASCII.decode(readBuffer).toString();
			event.setClientRequest(clientRequst);
			event.setRequestID(reqID);
			event.setClientAddress(clientAddress);
			event.setClientChannel(clientConnection);
			event.setWriteBuffer(writeBuffer);
		}
	}
	
	//Processes events as they become available in ring buffer
	//this is final processing. Results from here will be sent to client channels associated with a particular request.
	private class ClientRequestProcessor implements EventHandler<ClientEvent>
	{
		private final Disruptor<ClientEvent> disruptor;
		private final ListeningExecutorService partialWriteCompletionPool;


		public ClientRequestProcessor(Disruptor<ClientEvent> disruptor, ListeningExecutorService partialWriteCompletionPool) {
			this.disruptor = disruptor;
			this.partialWriteCompletionPool = partialWriteCompletionPool;
		}
		
		@Override
		public void onEvent(ClientEvent event, long sequence, boolean endOfBatch) throws Exception {
		
			String[] multipleClientRequests = event.getClientRequest().split(";");
			System.out.println(Thread.currentThread().getName()  + " - Num requests in block = " + multipleClientRequests.length);
			String requestID = event.getRequestID();
			String clientAddress = event.getClientAddress();

			for(String req : multipleClientRequests)
			{
				if(req.contains("Netstat")){
					
					serveNetStatRequest(requestID, event.getClientConnection(), event.getWriteBuffer());
					System.out.println(Thread.currentThread().getName() + " - Client at '" + event.getClientAddress() + "' sent a 'netstat', serving now " + requestID);
				}
				else
					System.out.println(Thread.currentThread().getName() + " - Client at '"+ clientAddress +"' sent '" + requestID + "', ignoring.");
			}
		}
	
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
	
	public void serveNetStatRequest(final String requestID, final AsynchronousSocketChannel clientConnection, final ByteBuffer buffer) {
		
		partialWriteCompletionPool.submit(new Runnable() {
			
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
								future = clientConnection.write(buffer);//, buffer, new ServerDataResponseDispatcher(clientConnection));
							}
							catch(Exception ex)
							{
								//ex.printStackTrace();
								try {
									System.out.println(Thread.currentThread().getName() + " - Client connection at " + clientConnection.getRemoteAddress() + " seems closed, can't serve request "+ requestID);
								} catch (IOException e) {
									//e.printStackTrace();
								}
								return;
							}
							
							final CountDownLatch latch = new CountDownLatch(1);
							ListenableFuture<Integer> listenableFuture = JdkFutureAdapters.listenInPoolThread(future, partialWriteCompletionPool);
							Futures.addCallback(listenableFuture, new FutureCallback<Integer>() {

								@Override
								public void onSuccess(Integer successfulBytesSent) {
									//so we can send more bytes now
									try {
										System.out.println(Thread.currentThread().getName() + " - requestID +  -- server sent " + successfulBytesSent + " bytes to client " + clientConnection.getRemoteAddress() + ", Request id = " + requestID );
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
						clientConnection.write(buffer);
						try {
							System.out.println(Thread.currentThread().getName() + " - Server sent " + data.length + " bytes to client " + clientConnection.getRemoteAddress()+ ", Request id = " + requestID);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		});
	  }		
	}

	//POC
	public static void main(String[] args) throws IOException, InterruptedException {
		
		new Thread(new NIO2ServerWithDisruptor()).start();
	}
}

