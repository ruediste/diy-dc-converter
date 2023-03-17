package com.github.ruediste.serial;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches /dev/* for changes
 */
@Singleton
public class AvailableDevicesAppController {

	private final Logger log = LoggerFactory.getLogger(AvailableDevicesAppController.class);

	private WatchService watchService;
	private Path directory = Paths.get("/dev");

	public void start() throws IOException {
		watchService = FileSystems.getDefault().newWatchService();
		new Thread(this::watch, "availaleDevicesScanner").start();
	}

	private final Set<Path> currentSerialConnections = new HashSet<>();

	public final Set<Runnable> onChange = new HashSet<>();

	private void watch() {
		try {
			directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
			scan();
			WatchKey key;
			while ((key = watchService.take()) != null) {
				boolean changed = false;
				for (WatchEvent<?> event : key.pollEvents()) {
					log.debug("{} event happened on {}", event.kind(), event.context());
					if (event.kind() == ENTRY_CREATE) {
						synchronized (this) {
							Path file = directory.resolve((Path) event.context()).toAbsolutePath();
							if (isSerialConnection(file)) {
								log.info("Adding serial connection {}", file);
								currentSerialConnections.add(file);
								changed = true;
							}
						}
					}
					if (event.kind() == ENTRY_DELETE) {
						synchronized (this) {
							Path file = directory.resolve((Path) event.context()).toAbsolutePath();
							if (isSerialConnection(file)) {
								log.info("Removing serial connection {}", file);
								currentSerialConnections.remove(file);
								changed = true;
							}
						}
					}
					if (event.kind() == OVERFLOW) {
						scan();
					}
				}

				if (changed) {
					onChange.forEach(Runnable::run);
				}

				key.reset();
			}
		} catch (ClosedWatchServiceException e) {
			// NOP
		} catch (Exception e) {
			log.error("Error while watching directory", e);
		}

	}

	private boolean isSerialConnection(Path path) {
		return path.getFileName().toString().startsWith("ttyACM");
	}

	private synchronized void scan() {
		log.info("Device scan of {} starting ...", directory.toAbsolutePath());
		currentSerialConnections.clear();
		currentSerialConnections.add(Path.of("simulator"));
		try {
			Files.list(directory).forEach(file -> {
				if (isSerialConnection(file)) {
					log.info("Adding serial connection {}", file.toAbsolutePath());
					currentSerialConnections.add(file.toAbsolutePath());
				}
			});

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		onChange.forEach(Runnable::run);
		log.info("Serial connection scan of {} complete", directory.toAbsolutePath());
	}

	public synchronized Set<Path> getCurrentSerialConnections() {
		return new HashSet<>(currentSerialConnections);
	}

}
