package client;
import java.io.*;
import java.net.*;
import java.util.*;

import sun.audio.AudioData;
import sun.audio.AudioDataStream;
import sun.audio.AudioPlayer;

import javax.sound.sampled.*;


public class RadioClient {
	Clip clip1;
	Socket sock;
	BufferedReader reader;
	PrintWriter writer;

	public RadioClient() {
		connectTo();
	}
	
	private class IncomingReader implements Runnable {
		public void run() {
			String message = null;
			try {
				while((message = reader.readLine()) != null) {
					if (message.startsWith("DONE")) {

					}
					else if(message.startsWith("CHOOSE")) {
						System.out.println(message);
						Scanner scan = new Scanner(System.in);
						String choice = scan.nextLine();
						writer.println(choice);
						writer.flush();
					}
					else if(message.startsWith("STREAM")) {
						System.out.println("Streaming..");
						message = message.replaceFirst("STREAM", "");
						byte[] byteToPlay = message.getBytes();
						//InputStream listen = new ByteArrayInputStream(byteToPlay);
						AudioData audiodata = new AudioData(byteToPlay);
						// Create an AudioDataStream to play back
						AudioDataStream audioStream = new AudioDataStream(audiodata);
						// Play the sound
						AudioPlayer.player.start(audioStream);
						/*sound = AudioSystem.getAudioInputStream(listen);
						// load the sound into memory (a Clip)
						DataLine.Info info = new DataLine.Info(Clip.class, sound.getFormat());
						clip1 = (Clip) AudioSystem.getLine(info);
						clip1.open(sound);
						clip1.addLineListener(new LineListener() {
							public void update(LineEvent event) {
								if (event.getType() == LineEvent.Type.STOP) {
									event.getLine().close();
								}
							}
						});
						// play the sound clip
						clip1.start();*/
					}
					else if(message.startsWith("INFO")) {
						System.out.println(message);
					}
					else {
					}
				}
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}
	
	private void connectTo() {
		try {
			sock = new Socket("localhost", 5001);
			
			InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
			Thread readerThread = new Thread(new IncomingReader());
			readerThread.start();
			reader = new BufferedReader(streamReader);
			writer = new PrintWriter(sock.getOutputStream());
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new RadioClient();
	}

}