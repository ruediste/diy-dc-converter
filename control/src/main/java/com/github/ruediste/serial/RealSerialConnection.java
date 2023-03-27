package com.github.ruediste.serial;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

public class RealSerialConnection {

	private final Logger log = LoggerFactory.getLogger(RealSerialConnection.class);

	private volatile boolean closing;
	private CountDownLatch closed = new CountDownLatch(1);

	private PipedOutputStream out;

	private final PipedInputStream in;

	private final String port;

	public RealSerialConnection(String port, int baudRate) {
		this.port = port;
		log.info("Opening {}", port);
		commPort = SerialPort.getCommPort(port);
		// commPort.setBaudRate(115200);
		commPort.setBaudRate(baudRate);
		commPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING | SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000,
				1000);
		commPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		commPort.setNumDataBits(8);
		commPort.setNumStopBits(2);
		commPort.setParity(SerialPort.NO_PARITY);
		if (!commPort.openPort())
			throw new RuntimeException("Error while opening port");

		log.info("Port {} opened", port);

		out = new PipedOutputStream();
		try {
			in = new PipedInputStream(out, 2048);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		new Thread(this::readLoop, "serialReader " + port).start();
	}

	private void readLoop() {
		log.info("Starting to read from serial port {}", getPort());
		try {
			byte buffer[] = new byte[1024];
			while (true) {
				int m = commPort.readBytes(buffer, buffer.length);
				if (m < 0) {
					throw new RuntimeException("read() failed ");
				}
				if (m > 0) {
					log.debug("received {}", hexDump(buffer, 0, m));
					out.write(buffer, 0, m);
					out.flush();
				}
				if (closing) {
					break;
				}
			}
		} catch (Throwable t) {
			log.error("Error in read loop of port " + getPort(), t);
		}
		closed.countDown();
		log.info("reading from serial port {} stopped", getPort());
	}

	private static String hexChars = "0123456789ABCDEF";
	private SerialPort commPort;

	public static String hexDump(byte[] buffer, int offset, int len) {
		StringBuilder sbHex = new StringBuilder();
		StringBuilder sbAscii = new StringBuilder();
		for (int idx = offset; idx < offset + len; idx++) {
			byte ch = buffer[idx];
			sbHex.append(hexChars.charAt((ch >> 4) & 0xF));
			sbHex.append(hexChars.charAt(ch & 0xF));
			sbHex.append(" ");
			if (ch > 32)
				sbAscii.append((char) ch);
			else
				sbAscii.append(' ');
		}
		return sbHex.toString() + "   " + sbAscii.toString();
	}

	public void close() {
		closing = true;
		try {

			if (!commPort.closePort())
				throw new RuntimeException("Error while closing port " + getPort());

			getIn().close();
			out.close();
			closed.await();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void sendBytes(byte[] tx) {
		log.debug("Sending {}", hexDump(tx, 0, tx.length));
		int l = 0;
		while (true) {

			int n = commPort.writeBytes(tx, tx.length - l, l);
			if (n < 0) {
				throw new RuntimeException("write() failed ");
			}
			l += n;
			if (l >= tx.length)
				break;
		}
		log.debug("Sending complete");
	}

	public String getPort() {
		return port;
	}

	public PipedInputStream getIn() {
		return in;
	}

}
