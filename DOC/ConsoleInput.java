package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.CompletionHandler;
import java.nio.channels.WritePendingException;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//Console input class to have a timeout gestion
public class ConsoleInput {
	// the number of tries of the user
	private final int tries;

	// the number of timeunit before timeout
	private final int timeout;

	// the time unit used to know when the timeout is
	private final TimeUnit unit;

	// The client informations
	private final ClientInformation attach;

	// The completion handler used to read and write to the server
	private final CompletionHandler<Integer, ClientInformation> ch;

	// Constructor
	public ConsoleInput(int tries, int timeout, TimeUnit unit, ClientInformation attach,
			CompletionHandler<Integer, ClientInformation> ch) {
		this.tries = tries;
		this.timeout = timeout;
		this.unit = unit;
		this.attach = attach;
		this.ch = ch;
	}

	// Method used to read a line
	public String readLine() throws InterruptedException {
		// Creation of the thread that will be listening to the console next
		// Line
		ExecutorService ex = Executors.newSingleThreadExecutor();

		// Input is what the user type on the console
		String input = null;
		try {
			// Start working until the number of tries is reached
			for (int i = 0; i < tries; i++) {
				// Execute the callable ConsoleInputReadTask to wait for the
				// client to type
				Future<String> result = ex.submit(new ConsoleInputReadTask());
				try {
					input = result.get(timeout, unit);
					break;
				} catch (ExecutionException e) {
					e.getCause().printStackTrace();
				} catch (TimeoutException e) {
					// If a timetout is catched, we will ask the server to
					// actualize the conversation
					result.cancel(true);

					Charset cs = Charset.forName("UTF-8");
					attach.buffer.clear();
					String response = ":" + attach.address + " ACTUALIZE";
					byte[] data = response.getBytes(cs);
					attach.buffer.put(data);
					attach.buffer.flip();

					attach.isRead = false; // It is a write
					if (attach.channel.isOpen()) {
						attach.buffer.rewind();
						try {
							attach.channel.write(attach.buffer, attach, ch);
						} catch (WritePendingException wpe) {
							// Write Pending Exception :(
							System.out.println("OUPS");
						}
					}
				}
			}
		} finally {
			// Stop the thread
			ex.shutdownNow();
		}
		// returns the input
		return input;
	}
}

// Callable console waiting for typing
class ConsoleInputReadTask implements Callable<String> {
	public String call() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input;
		do {
			try {
				// wait until we have data to complete a readLine()
				while (!br.ready()) {
					Thread.sleep(200);
				}
				input = br.readLine();
			} catch (InterruptedException e) {
				return null;
			}
		} while ("".equals(input));
		return input;
	}
}
