package com.colabriq.vertx.stream;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

/**
 * A {@link ReadStream} that also implements a blocking {@link WritableByteChannel}
 * that can be used in a threaded scenario to send bytes the {@link ReadStream} can read
 * blocking appropriately as it is consumed.
 */
public class WritableReadStream implements ReadStream<Buffer>, WritableByteChannel {	
	private Handler<Buffer> dataHandler;
	private Handler<Void> endHandler;
	private Handler<Throwable> exceptionHandler;
	
	private boolean open = true;
	private boolean flowing = true;
	private int demand = 0;

	public WritableReadStream() {
	}
	
	@Override
	public ReadStream<Buffer> handler(Handler<Buffer> handler) {
		this.dataHandler = handler;
		return this;
	}
	
	@Override
	public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public ReadStream<Buffer> endHandler(Handler<Void> handler) {
		this.endHandler = handler;
		return this;
	}
	
	@Override
	public synchronized ReadStream<Buffer> pause() {
		this.flowing = false;
		this.demand = 0;
		
		notifyAll();
		
		return this;
	}

	@Override
	public synchronized ReadStream<Buffer> resume() {
		this.flowing = true;
		this.demand = 0;
		
		notifyAll();
		
		return this;
	}
	
	@Override
	public synchronized ReadStream<Buffer> fetch(long amount) {
		if (!this.flowing) {
			this.demand += amount;
			notifyAll();
		}
		
		return this;
	}
	
	@Override
	public synchronized boolean isOpen() {
		return this.open;
	}
	
	@Override
	public int write(ByteBuffer src) throws IOException {
		var remain = src.remaining();
		
		while (open) {
			try {
				if (flowing) {
					if (dataHandler != null) {
						dataHandler.handle(Buffer.buffer(wrappedBuffer(src)));
					}
					
					// move position to end of buffer
					src.position(src.position() + src.remaining());
					
					return remain;
				}
				else if (demand >= remain) {
					if (this.dataHandler != null) {
						this.dataHandler.handle(Buffer.buffer(wrappedBuffer(src)));
					}
					
					// move position to end of buffer
					src.position(src.position() + src.remaining());
					
					return remain;
				}
				else if (demand > 0) {
					// set limit to demand amount
					src.limit(src.position() + demand);
					
					if (this.dataHandler != null) {
						this.dataHandler.handle(Buffer.buffer(wrappedBuffer(src)));
					}
					
					// move position by demand consumed
					src.position(src.position() + demand);
					
					return demand;
				}
				else {
					wait(); // wait until at least one byte can be written
				}
			}
			catch (InterruptedException e) {
				// continue loop
			}
		}
		
		if (this.exceptionHandler != null) {
			var e = new IOException("ReadStream has closed");
			e.fillInStackTrace();
			this.exceptionHandler.handle(e);
		}
		
		return -1;
	}
	
	@Override
	public synchronized void close() throws IOException {
		this.open = false;
		if (this.endHandler != null) {
			this.endHandler.handle(null);
		}
		
		synchronized (this) {
			this.notifyAll();
		}
	}
}
