package com.colabriq.vertx.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Deque;
import java.util.LinkedList;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * A {@link WriteStream} that also implements a blocking {@link ReadableByteChannel}
 * that can be used in a threaded scenario to get bytes the {@link WriteStream} receives
 * blocking appropriately as you consume.
 */
public class ReadableWriteStream implements WriteStream<Buffer>, ReadableByteChannel {
	private final Deque<ByteBuffer> queue = new LinkedList<>();
	
	private boolean open = true, ended = false;
	private int queued = 0;
	private int writeQueueMaxSize = 100;
	
	private Handler<Void> drainHandler;
	private Handler<Throwable> exceptionHandler = null;
	
	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		System.out.println("drainHandler");
		this.drainHandler = handler;
		return this;
	}
	
	private void checkDrain() {
		System.out.println("checkDrain");
		if (!ended && (queue.isEmpty() || queued < writeQueueMaxSize) && (drainHandler != null)) {
			drainHandler.handle(null);
		}
	}
	
	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}
	
	@Override
	public synchronized WriteStream<Buffer> setWriteQueueMaxSize(int size) {
		System.out.println("setWriteQueueMaxSize = " + size);
		this.writeQueueMaxSize = size;
		return this;
	}

	@Override
	public synchronized boolean writeQueueFull() {
		if ((queue.isEmpty() || queued < writeQueueMaxSize)) {
			System.out.println("writeQueue (not) Full");
			return false;
		}
		else {
			System.out.println("writeQueueFull!");
			return true;
		}
	}
	
	@Override
	public WriteStream<Buffer> write(Buffer data) {
		write(data, null);
		return this;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		System.out.println("write: " + data.length() + " (queued=" + (queued + data.length()) + ")");
		
		if (data.length() > 0) {
			// wrap into NIO buffer
			var bb = data.getByteBuf();
			bb.resetReaderIndex(); // doc specifies this might not be 0
			var niobb = bb.nioBuffer();
		
			synchronized (this) {
				if (data.length() > 0) {
					queued += niobb.remaining();
					queue.addLast(niobb);
					notifyAll();
				}
			}
		}
		
		if (handler != null) {
			handler.handle(new AsyncResult<Void>() {
				@Override public boolean succeeded() { return true; }
				@Override public boolean failed() { return false; }
				@Override public Void result() { return null; }
				@Override public Throwable cause() { return null; }					
			});
		}
		
		return this;
	}

	@Override
	public void end() {
		end((Handler<AsyncResult<Void>>)null);
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		synchronized (this) {
			this.ended = true;
			notifyAll();
		}
		
		if (handler != null) {
			handler.handle(new AsyncResult<Void>() {
				@Override public boolean succeeded() { return true; }
				@Override public boolean failed() { return false; }
				@Override public Void result() { return null; }
				@Override public Throwable cause() { return null; }					
			});
		}
	}

	@Override
	public synchronized boolean isOpen() {
		return open;
	}
	
	@Override
	public synchronized int read(ByteBuffer dst) throws IOException {
		System.out.println("read: open=" + open + " ended=" + ended + " queue=" + queue.size() + " queued=" + queued + " dst.rem=" + dst.remaining());
		
		int read = 0;
		
		while (this.open) {
			// check if dst buffer has any capacity left
			if (dst.remaining() == 0) {
				return read;
			}
			
			if (ended && queue.isEmpty()) {
				this.open = false;
				return -1;
			}
			
			// wait for some bytes if the queue is empty
			while (!ended && queue.isEmpty()) {
				checkDrain();
				
				try {
					wait();
				}
				catch (InterruptedException e) {
				}
			}
			
			if (!queue.isEmpty()) {
				var nxt = queue.peek();
				if (nxt.remaining() <= dst.remaining()) {
					read += nxt.remaining();
					dst.put(nxt);
				}
				else {
					read += dst.remaining();
					nxt.limit(nxt.position() + dst.remaining());
					dst.put(nxt);
					nxt.limit(nxt.capacity()); // back to start for next run 
				}
				
				if (!nxt.hasRemaining()) {
					queue.removeFirst();
				}
				
				checkDrain();
				this.queued -= read;
				return read;
			}
		}
		
		return -1;
	}

	@Override
	public void close() throws IOException {
		
	}
}
