import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class WatchDog {
	public static void main(String[] args) {
		try (WatchService ws = FileSystems.getDefault().newWatchService()) {
			Path dirToWatch = Paths.get("/mnt/VMs/GAS");
			dirToWatch.register(ws, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
			while (true) {
				WatchKey key = ws.take();
				for (WatchEvent<?> event : key.pollEvents()) {
					Kind<?> eventKind = event.kind();
					if (eventKind == OVERFLOW) {
						System.out.println("Event overflow occurred");
						continue;
					}
					WatchEvent<Path> currEvent = (WatchEvent<Path>) event;
					Path dirEntry = currEvent.context();
					System.out.println(eventKind + "\toccurred on\t" + dirEntry);
				}
				boolean isKeyValid = key.reset();
				if (!isKeyValid) {
					System.out.println("No longer watching " + dirToWatch);
					break;
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
	}
}
}
