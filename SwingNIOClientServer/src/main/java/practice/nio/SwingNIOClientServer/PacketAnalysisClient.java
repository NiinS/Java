package practice.nio.SwingNIOClientServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * A simple socket based client which sends 5 netsat requests to server and then dies.
 * Any number of clients can be created and run in parallel.
 * @author Nitin
 *
 */
public class PacketAnalysisClient implements Runnable {

	@Override
	public void run() {
		
		
		try {
			
			SocketChannel channel = SocketChannel.open();
			
			channel.connect(new InetSocketAddress(AppConstants.SERVER_PORT));
			System.out.println("Client connected to server at "+ channel.getRemoteAddress());
			channel.configureBlocking(false);
			
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			int i=1;
			while(i <= 5)
			{
				i++;
				buffer.clear();
				buffer.put(StandardCharsets.US_ASCII.encode("Netstat please;"));
				buffer.flip();
				channel.write(buffer);
				System.out.println("Client sent a netstat request");
				Thread.sleep(3000);
			}
			
			Thread.sleep(2000);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		};
	}
	
	
	public static void main(String[] args) {
		
		new Thread (new PacketAnalysisClient()).start();
	}
}
