package client;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.security.Key;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AsynchronousClient {
	public static void main(String[] args) throws Exception {
		// Creation of the socket client
		AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
		// This is the server address
		SocketAddress serverAddr = new InetSocketAddress("localhost", 8989);
		// Link our socket client to the socket server
		Future<Void> result = channel.connect(serverAddr);
		// The get() method waits if needed to complete the connexion with the
		// server
		result.get();
		System.out.println("Connected to the server !"); // We are connected !

		// Recording the information of this client-server connection
		ClientInformation clientInfo = new ClientInformation();
		clientInfo.channel = channel;
		clientInfo.buffer = ByteBuffer.allocate(2048);
		clientInfo.isRead = true;
		clientInfo.mainThread = Thread.currentThread();
		clientInfo.address = clientInfo.channel.getLocalAddress();

		// Creating a CompletionHandler used for the read and write operations
		// of the client
		ReadWriteHandler readWriteHandler = new ReadWriteHandler();

		// First, we read the server (it will ask for an existing account, or
		// not)
		channel.read(clientInfo.buffer, clientInfo, readWriteHandler);

		// Let the thread end
		clientInfo.mainThread.join();
	}
}

// Client informations
class ClientInformation {
	AsynchronousSocketChannel channel;
	ByteBuffer buffer;
	Thread mainThread;
	SocketAddress address;

	// isRead=true means the client is waiting for reading the server
	// isRead=false means the client is waiting for writing to the server
	boolean isRead;
}

// The completion handler used to read and write to the server
class ReadWriteHandler implements CompletionHandler<Integer, ClientInformation> {
	// Counter of the tries for connection
	private int tryToConnect = 0;

	// If the read or write operation succeed, we enter in the completed method
	// :
	public void completed(Integer result, ClientInformation attach) {

		// We just read something
		if (attach.isRead) {
			// Get the message send by the server
			attach.buffer.flip();
			Charset cs = Charset.forName("UTF-8");
			int limits = attach.buffer.limit();
			byte bytes[] = new byte[limits];
			attach.buffer.get(bytes, 0, limits);
			String msg = new String(bytes, cs);
			msg = msg.split("\r")[0];
			boolean bool = false;
			try {
				// we treat the message according to the command in the message
				// bool=true the message was treated with succeed
				bool = treatmentForMessage(msg, attach);
			} catch (IOException | NumberFormatException | InterruptedException e) {
				e.printStackTrace();
			}
			if (bool) {
				attach.isRead = false; // Now we will write
				if (attach.channel.isOpen())
					attach.channel.write(attach.buffer, attach, this);
			} else {
				// Here we have a write pending exception caused by the
				// actualization (PRIVMSG command)
				System.out.println("Message not treated");
				attach.buffer.clear();
			}

			// We just wrote something
		} else {
			attach.isRead = true; // Now we will read
			attach.buffer.clear();
			attach.channel.read(attach.buffer, attach, this);
		}
	}

	private boolean treatmentForMessage(String msg, ClientInformation clientInfo)
			throws IOException, NumberFormatException, InterruptedException {
		// Use this SYSO to see what message the server sended to this client :
		//System.out.println("Server says : " + msg);

		// We separate the three elements of the msg : the prefix, the command
		// and the params (msg)
		int prefixEndIndex = msg.indexOf(" ");
		String prefix = msg.substring(1, prefixEndIndex - 1);
		msg = msg.substring(prefixEndIndex + 1);
		int commandEndIndex = msg.indexOf(" ");
		String command = "";
		String change = "";
		if (commandEndIndex < 0) {
			command = msg;
			msg = "";
		} else {
			command = msg.substring(0, commandEndIndex);
			msg = msg.substring(commandEndIndex + 1);
		}

		// Creation of the console readers
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));

		// This console will be used for the chat because it has a timeout : we
		// will actualize the conversation when the timeout is reached
		ConsoleInput console = new ConsoleInput(3, 10, TimeUnit.SECONDS, clientInfo, this);

		Charset cs = Charset.forName("UTF-8");
		SocketAddress address = null;
		try {
			address = clientInfo.channel.getLocalAddress();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		clientInfo.buffer.clear();

		String temporaryLogin = "";
		String response = "";

		// Now we will switch on the command to do the good actions
		switch (command) {

		// CNX is the first message send to the client to know if he already has
		// an account
		case "CNX":
			System.out.println("Do you already have an account ? y/n");
			String answer = bf.readLine();
			while (!answer.equalsIgnoreCase("y") && !answer.equalsIgnoreCase("n")) {
				System.out.println("Yes or No ? (y/n)");
				answer = bf.readLine();
			}
			response = ":" + address + " CNX " + answer;
			break;

		// NEW is the command to create a new account, beginning with a new
		// login
		case "NEW":
			if (msg.contains("used"))
				System.out.println("Login already used");
			System.out.println("Enter your login :");
			String log = bf.readLine();
			while (!isLoginValid(log)) {
				if (log.length() < 1) { // login is non given
					sendNumeric("ERR_NONICKNAMEGIVEN", log);
					response = ":" + address + " ERR_NONICKNAMEGIVEN" + " newErr";
				} else { // login is not valid about characters
					sendNumeric("ERR_ERRONEUSNICKNAME", log);
				}
				System.out.println(
						"Your login must contain only alphanumerical chars and shorter than 9. Enter your login :");
				log = bf.readLine();
			}
			response = ":" + address + " NEW " + log;
			break;

		// NEWPASS is the command to write the PASSWORD, fullname and mail of
		// the new account
		case "NEWPASS":
			System.out.println("Enter your password :");
			response = ":" + address + " NEWPASS " + encrypt(bf.readLine(), "MerleneFanny");
			System.out.println("Enter your full name :");
			response += " " + bf.readLine();
			System.out.println("Enter your mail :");
			response += " " + bf.readLine();
			break;

		// NICK is the command to enter the login to connect to an existing
		// account
		case "NICK":
			if (msg.contains("e"))
				System.out.print("This login does not exist. ");
			System.out.println("Enter your login :");
			temporaryLogin = bf.readLine();
			if (temporaryLogin.length() < 1) { // login is non given
				sendNumeric("ERR_NONICKNAMEGIVEN", temporaryLogin);
				response = ":" + address + " ERR_NONICKNAMEGIVEN" + " nickErr";
			} else
				response = ":" + address + " NICK " + temporaryLogin;
			break;

		// treatment of the error nick collision
		case "ERR_NICKCOLLISION":
			sendNumeric("ERR_NICKCOLLISION", temporaryLogin);
			response = ":" + address + " ERR_NICKCOLLISION";
			break;

		// PASS is the command to enter the password of the existing account
		case "PASS":
			if (msg.contains("e"))
				System.out.print("This password is incorrect. ");
			
			// If the counter is > 3, we will quit the client
			if (tryToConnect == 3) {
				response = ":" + address + " QUIT" + " tooMuchTentative";
			} else {
				Console consoleNotEclipse = System.console();
				if (consoleNotEclipse == null) {
					System.out.println("Couldn't get console instance");
					System.exit(0);
				}
				System.out.println("Enter your password :");
				String passEncrypt = encrypt(new String(consoleNotEclipse.readPassword()), "MerleneFanny");
				response = ":" + address + " PASS " + passEncrypt;
				tryToConnect++;
			}
			break;

		// MENU is the command to show the menu of the chat
		case "MENU":
			System.out.println("Menu :");
			System.out.println("1 : List of contacts");
			if (msg.contains("new"))
				System.out.println("2 : Chat with a contact [NEW]");
			else
				System.out.println("2 : Chat with a contact");
			System.out.println("3 : Chat with a list");
			System.out.println("4 : Options");
			System.out.println("5 : Quit");
			String option = bf.readLine();
			while (!option.matches("[1-5]")) {
				System.out.println("You can only choose an option between 1 and 5. Choose :");
				option = bf.readLine();
			}

			// Sending QUIT command to disconnect the client properly
			if (option.equals("5"))
				response = ":" + address + " QUIT";
			else
				response = ":" + address + " MENU " + option;
			break;

		// Enter in the contacts options menu
		case "CONTACTS_OPTIONS":
			if (msg.contains("added")) {
				System.out.println("Contact added !");
				msg = msg.split(" ")[0];
			} else if (msg.contains("deleted")) {
				System.out.println("Contact deleted !");
				msg = msg.split(" ")[0];
			} else if (msg.contains("new")) {
				msg = msg.split(" ")[0];
			}
			String[] contacts = msg.split("-");
			if (msg.equals(""))
				contacts = new String[0];
			System.out.println("You have " + contacts.length + " contacts.");
			if (contacts.length != 0) {
				for (int i = 1; i <= contacts.length; i++) {
					System.out.println("Contact " + i + " : " + contacts[i - 1]);
				}
			}
			System.out.println("Contact menu :");
			System.out.println("1 : Add a new contact");
			System.out.println("2 : Delete a contact");
			System.out.println("3 : Back to menu");
			option = bf.readLine();
			while (!option.matches("[1-3]")) {
				System.out.println("You can only choose an option between 1 and 3. Choose :");
				option = bf.readLine();
			}
			response = ":" + address + " CONTACTS_OPTIONS " + option;
			break;

		// Add a contact
		case "ADD_CONTACT":
			String[] users = msg.split("-");
			System.out.println("There are " + users.length + " users you can add.");
			if (users.length != 0) {
				for (int i = 1; i <= users.length; i++) {
					System.out.println("User " + i + " : " + users[i - 1]);
				}
				System.out.println("Enter the number of the user you want to add :");
				option = bf.readLine();
				while (!option.matches("[1-" + users.length + "]")) {
					System.out.println("You can only choose an option between 1 and " + users.length + ". Choose :");
					option = bf.readLine();
				}
				response = ":" + address + " ADD_CONTACT " + users[Integer.parseInt(option) - 1];
			} else {
				System.out.println("There is no other user you can add.");
				System.out.println("Contact menu :");
				System.out.println("1 : Add a new contact");
				System.out.println("2 : Delete a contact");
				System.out.println("3 : Back to menu");
				option = bf.readLine();
				while (!option.matches("[1-3]")) {
					System.out.println("You can only choose an option between 1 and 3. Choose :");
					option = bf.readLine();
				}
				response = ":" + address + " CONTACTS_OPTIONS " + option;
			}
			break;

		// Delete a contact
		case "DEL_CONTACT":
			contacts = msg.split("-");
			if (msg.equals(""))
				contacts = new String[0];
			System.out.println("You have " + contacts.length + " contacts.");
			if (contacts.length != 0) {
				for (int i = 1; i <= contacts.length; i++) {
					System.out.println("Contact " + i + " : " + contacts[i - 1]);
				}
			}
			System.out.println("Enter the number of the contact you want to delete : ");
			option = bf.readLine();
			while (!option.matches("[1-" + contacts.length + "]")) {
				System.out.println("You can only choose an option between 1 and " + contacts.length + ". Choose :");
				option = bf.readLine();
			}
			response = ":" + address + " DEL_CONTACT " + option;
			break;

		// Enter in the options menu (change a client information or delete the
		// account!)
		case "OPTIONS":
			if (!msg.equals("")) {
				System.out.println("Your " + msg + " has been changed !");
			}
			System.out.println("Options menu :");
			System.out.println("1 : Change your login");
			System.out.println("2 : Change your password");
			System.out.println("3 : Change your full name");
			System.out.println("4 : Change your mail");
			System.out.println("5 : Delete your account and disconnect (irreversible)");
			System.out.println("6 : Back to the menu");
			option = bf.readLine();
			while (!option.matches("[1-6]")) {
				System.out.println("You can only choose an option between 1 and 6. Choose :");
				option = bf.readLine();
			}
			if (option.equals("5"))
				response = ":" + address + " QUIT delete";
			else
				response = ":" + address + " OPTIONS " + option;
			break;

		// Command to change a client information
		case "CHANGE":
			switch (msg) {
			case "login":
				System.out.println("Enter your new login :");
				change = bf.readLine();
				while (change.length() < 1) {
					change = bf.readLine();
				}
				break;
			case "pass":
				System.out.println("Enter your new password :");
				change = bf.readLine();
				while (change.length() < 1) {
					change = bf.readLine();
				}
				change = encrypt(change, "MerleneFanny");
				break;
			case "fullName":
				System.out.println("Enter your new full name :");
				change = bf.readLine();
				while (change.length() < 1) {
					change = bf.readLine();
				}
				break;
			case "mail":
				System.out.println("Enter your new mail :");
				change = bf.readLine();
				while (change.length() < 1) {
					change = bf.readLine();
				}
				break;
			}
			response = ":" + address + " CHANGE " + msg + "-" + change;
			break;

		// treatment of the error nick name in use
		case "ERR_NICKNAMEINUSE":
			System.out.println("CHANGE is " + change);
			sendNumeric("ERR_NICKNAMEINUSE", change);
			System.out.println("Login already used. Enter your new login :");
			change = bf.readLine();
			while (change.length() < 1) {
				change = bf.readLine();
			}
			response = ":" + address + " CHANGE " + msg + "-" + change;
			break;

		// Command to enter in the CHAT menu and choose the contact you want to
		// talk to
		case "CHAT":
			String another = "";
			if (msg.contains("new")) {
				another = msg.split(" ")[1].split("-")[1];
				msg = msg.split(" ")[0];
			}
			String[] connectedUsers = msg.split("-");
			if (msg.equals(""))
				connectedUsers = new String[0];
			System.out.println("There are " + connectedUsers.length + " contacts you can talk to.");
			if (connectedUsers.length != 0) {
				for (int i = 1; i <= connectedUsers.length; i++) {
					System.out.print("User " + i + " : " + connectedUsers[i - 1]);
					if (connectedUsers[i - 1].equals(another))
						System.out.println(" [NEW]");
					else
						System.out.println();
				}
			}
			System.out.println("Enter the number of the user you want to talk to :");
			option = bf.readLine();
			while (!option.matches("[1-" + connectedUsers.length + "]")) {
				System.out
						.println("You can only choose an option between 1 and " + connectedUsers.length + ". Choose :");
				option = bf.readLine();
			}
			response = ":" + address + " CHAT " + connectedUsers[Integer.parseInt(option) - 1];
			break;

		// Enter in the CHAT TO LISt menu and choose the contactS you want to
		// send a message
		case "CHATN":
			connectedUsers = msg.split("-");
			if (msg.equals(""))
				connectedUsers = new String[0];
			System.out.println("There are " + connectedUsers.length + " contacts you can talk to.");
			if (connectedUsers.length != 0) {
				for (int i = 1; i <= connectedUsers.length; i++) {
					System.out.println("User " + i + " : " + connectedUsers[i - 1]);
				}
			}
			option = "";
			boolean isCorrect = false;
			while (!isCorrect) {
				isCorrect = true;
				System.out.println(
						"Enter the numbers with spaces between of the users you want to send a msg (like 1 2 3) :");
				option = bf.readLine();
				for (int i = 0; i < option.length(); i++) {
					if (i % 2 == 0 && (!Character.isDigit(option.charAt(i))
							|| Character.digit(option.charAt(i), 10) > connectedUsers.length)) {
						System.out.println("ce if pour i=" + i + " et char=" + option.charAt(i));
						System.out.println(i % 2 == 0);
						System.out.println(!Character.isDigit(option.charAt(i)));
						System.out.println(Character.digit(option.charAt(i), 10) > connectedUsers.length - 1);
						isCorrect = false;
					}
					if (i % 2 == 1 && !Character.isWhitespace(option.charAt(i))) {
						System.out.println("i=" + i + " et char=" + option.charAt(i));
						System.out.println(i % 2 == 1);
						System.out.println(!Character.isWhitespace(option.charAt(i)));
						isCorrect = false;
					}
				}
			}
			String[] dests = option.split(" ");
			for (int i = 0; i < dests.length; i++) {
				if (i == 0)
					option = connectedUsers[Integer.parseInt(dests[i]) - 1];
				else
					option += "-" + connectedUsers[Integer.parseInt(dests[i]) - 1];
			}
			response = ":" + address + " CHATN " + option;
			System.out.println("[Write /quit to return to the menu] Your message to the list : ");

			String messageToSend = bf.readLine();
			response += ";@;" + messageToSend;
			break;

		// command to send a private msg
		case "PRIVMSG":

			String dest = msg;
			if (dest.length() < 1) {
				sendNumeric("ERR_NORECIPIENT", "PRIVMSG");
				response = ":" + address + " CONTACTS_OPTIONS " + "3";
				break;
			}
			if (msg.contains("rpl_away")) {
				dest = msg.split("-")[0];
				sendNumeric("RPL_AWAY", dest);
			}
			if (msg.contains("-")) {
				dest = dest.split("-")[0];
				System.out.println(dest + " said : " + msg.split("-")[1]);
			}
			System.out.println("[Write /quit to return to the menu] Your message to " + dest + ": ");

			messageToSend = console.readLine();
			if (Objects.isNull(messageToSend)) {
				return false;
			} else {
				if (messageToSend.length() < 1) {
					sendNumeric("NOTEXTTOSEND", null);
					System.out.println("Please enter a non empty text : \n");
					messageToSend = console.readLine();
				}
				response = ":" + address + " PRIVMSG " + dest + "-" + messageToSend;
			}
			break;

		// Command received to disconnect the client properly
		case "QUIT":
			try {
				clientInfo.channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (msg.contains("collision"))
				System.out.println("Double connexion with the same login.");
			else if (msg.contains("deleted"))
				System.out.println("Your account is deleted and you're disconnected.");
			else if (msg.contains("tooMuchTentative"))
				System.out.println("You try to connect too much time. You're disconnected.");
			else
				System.out.println("You're disconnected.");
			System.exit(0);
		default:
			System.out.println("Command not handled");
			break;
		}
		response = response + "\r\n";
		byte[] data = response.getBytes(cs);
		clientInfo.buffer.put(data);
		clientInfo.buffer.flip();
		return true;
	}

	// Check if the login enter has a valid form (only contains alpha numerical
	// chars and its length is least than 9 chars)
	private boolean isLoginValid(String login) {
		String regExpression = "[a-zA-Z_0-9]*";
		return login.matches(regExpression) && login.length() <= 9 && login.length() > 0;
	}

	// Method used to encrypt the password before send it to the server
	private String encrypt(String password, String key) {
		try {
			Key clef = new SecretKeySpec(key.getBytes("ISO-8859-2"), "Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, clef);
			String p = new String(cipher.doFinal(password.getBytes()));
			return p;
		} catch (Exception e) {
			return null;
		}
	}

	// Sends numerical responses
	private void sendNumeric(String error, String option) throws IOException {
		String response = "";

		switch (error) {
		case ("ERR_NONICKNAMEGIVEN"):
			response = "431 ERR_NONICKNAMEGIVEN\r\n :No nickname given";
			break;
		case ("ERR_NICKNAMEINUSE"):
			response = "433 ERR_NICKNAMEINUSE\r\n <" + option + "> :Nickname is already in use";
			break;
		case ("ERR_NICKCOLLISION"):
			response = "436 ERR_NICKCOLLISION\r\n <" + option + "> :Nickname collision KILL";
			break;
		case ("ERR_ERRONEUSNICKNAME"):
			response = "432 ERR_ERRONEUSNICKNAME\r\n <" + option + "> :Erroneus nickname";
			break;
		case ("ERR_NORECIPIENT"):
			response = "411 ERR_NORECIPIENT\r\n :No recipient given<" + option + ">";
			break;
		case ("ERR_NOSUCHNICK"):
			response = "401 ERR_NOSUCHNICK\r\n <" + option + "> :No such nick/channel";
			break;
		case ("RPL_AWAY"):
			response = "301 RPL_AWAY\r\n <" + option
					+ "> : Hi, I'm not connected for the moment but I will read your message soon :)";
			break;
		case ("ERR_NOTEXTTOSEND"):
			response = "412 ERR_NOTEXTTOSEND\r\n :No text to send";
			break;
		case ("ERR_TOOMANYTARGETS"):
			response = "407 ERR_TOOMANYTARGETS\r\n <" + option + "> :Duplicate recipients. No message delivered";
			break;
		default:
//System.out.println("Erreur non gérée ");
			System.out.println("Erreur non geree ");
		}
		System.out.println(response + "\r\n");
	}

	// We enter in if the read or write operation failed
	@Override
	public void failed(Throwable e, ClientInformation attach) {
		e.printStackTrace();
	}
}
