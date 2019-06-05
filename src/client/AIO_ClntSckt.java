package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.Future;

import serveur.Radio;

public class AIO_ClntSckt {

	private ArrayList<String> globalListSong = new ArrayList<String>();
	private ArrayList<Radio> listRadio = new ArrayList<>();
	private ArrayList<PrintWriter> userList;

	public static void main(String[] args) throws Exception {
		AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
		SocketAddress serverAddr = new InetSocketAddress("localhost", 8989);
		Future<Void> result = channel.connect(serverAddr);
		result.get();

		System.out.println("Connecte");

		Attachment attach = new Attachment();
		attach.channel = channel;
		attach.buffer = ByteBuffer.allocate(2048);
		attach.isRead = false;
		attach.mainThread = Thread.currentThread();

		Charset cs = Charset.forName("UTF-8");
		String msg = "Bonjour";
		byte[] data = msg.getBytes(cs);
		attach.buffer.put(data);
		attach.buffer.flip();

		//ReadWriteHandler readWriteHandler = new ReadWriteHandler();
		//channel.write(attach.buffer, attach, readWriteHandler);
		attach.mainThread.join();
	}
}
class Attachment {
	AsynchronousSocketChannel channel;
	ByteBuffer buffer;
	Thread mainThread;
	boolean isRead;
}
/*class ReadWriteHandler implements CompletionHandler<Integer, Attachment> {
  @Override
  public void completed(Integer result, Attachment attach) {
    if (attach.isRead) {
      attach.buffer.flip();
      Charset cs = Charset.forName("UTF-8");
      int limits = attach.buffer.limit();
      byte bytes[] = new byte[limits];
      attach.buffer.get(bytes, 0, limits);
      String msg = new String(bytes, cs);
      System.out.format("Le Serveur a repondu: "+ msg);
      try {
        msg = this.getTextFromUser();
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (msg.equalsIgnoreCase("a+")) {
        attach.mainThread.interrupt();
        return;
      }
      attach.buffer.clear();
      byte[] data = msg.getBytes(cs);
      attach.buffer.put(data);
      attach.buffer.flip();
      attach.isRead = false; // Ecriture
      attach.channel.write(attach.buffer, attach, this);
    }else {
      attach.isRead = true;
      attach.buffer.clear();
      attach.channel.read(attach.buffer, attach, this);
    }
  }
  @Override
  public void failed(Throwable e, Attachment attach) {
    e.printStackTrace();
  }
  private String getTextFromUser() throws Exception{
    System.out.print("SVP enter votre message (a+ pour sortir du programme):");
    BufferedReader consoleReader = new BufferedReader(
        new InputStreamReader(System.in));
    String msg = consoleReader.readLine();
    return msg;
  }
}
 */