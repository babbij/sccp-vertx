package com.goodforgoodbusiness.vertx.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.Pipe.SourceChannel;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * Uses {@link PipedWriteStream} to create an {@link InputStream} that has basic flow controls.
 */
public class InputWriteStream extends PipedWriteStream implements WriteStream<Buffer> {
	private static final int DEFAULT_WRITE_QUEUE_SIZE = 1000;
	
	// the underlying stream around the channel
	private final InputStream channelStream;
	
	// the inputstream we create with flow controls
	private final InputStream inputStream;
	
	private int maxWriteQueueSize = DEFAULT_WRITE_QUEUE_SIZE;
	private int available = 0;
	
	private boolean writeQueueFull = false;
	
	private Handler<Throwable> exceptionHandler = null;
	private Handler<Void> drainHandler = null;
	
	public InputWriteStream() throws IOException {
		super();
		
		this.channelStream = Channels.newInputStream(super.getSource());
		
		// wrap with some flow control
		this.inputStream = new InputStream() {
			@Override
			public int available() throws IOException {
				return available;
			}
			
			@Override
			public int read() throws IOException {
				try {
					var b = channelStream.read();
					if (b >= 0) {
						updateAvailable(-1);
						return b;
					}
					else {
						return -1;
					}
				}
				catch (AsynchronousCloseException e) {
					close();
					return -1;
				}
				catch (IOException e) {
					if (exceptionHandler != null) {
						exceptionHandler.handle(e);
					}
					
					close();
					return -1;
				}
			}
			
			@Override
			public int read(byte [] b, int off, int len) throws IOException {
				try {
					var count = channelStream.read(b, off, len);
					if (count >= 0) {
						updateAvailable(-count);
						return count;
					}
					else {
						close();
						return -1;
					}
				}
				catch (IOException e) {
					if (exceptionHandler != null) {
						exceptionHandler.handle(e);
					}
					
					close();
					return -1;
				}
			}
			
			@Override
			public void close() throws IOException {
				getSource().close();
				getSink().close();
				channelStream.close();
			}
		};
	}
	
	private void updateAvailable(int delta) {
		available += delta;
		if (available < maxWriteQueueSize / 2) {
			writeQueueFull = false;
			if (drainHandler != null) {
				this.drainHandler(null);
			}
		}
		else if (available < maxWriteQueueSize) {
			writeQueueFull = false;
		}
		else {
			writeQueueFull = true;
		}
	}
	
	/**
	 * Returns the flow controlled {@link InputStream} wrapping the {@link SourceChannel}
	 */
	public InputStream getInputStream() {
		return inputStream;
	}
	
	@Override
	public boolean writeQueueFull() {
		return this.writeQueueFull;
	}
	
	@Override
	public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
		this.maxWriteQueueSize = maxSize;
		return this;
	}
	
	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		updateAvailable(data.length());
		return super.write(data, handler);
	}
	
	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
		super.drainHandler(handler);
		return this;
	}
	
	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		super.exceptionHandler(handler);
		return this;
	}
}
