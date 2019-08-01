package com.goodforgoodbusiness.vertx.stream;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * Implements {@link WriteStream} backed by an NIO {@link Pipe}
 * which can be used to consume it via {@link SourceChannel}.
 * 
 * No flow control.
 */
public class PipedWriteStream implements WriteStream<Buffer> {
	private final SourceChannel nioSource;
	private final SinkChannel nioSink;
	
	private boolean ended = false;
	private Handler<Throwable> exceptionHandler = null;
	
	public PipedWriteStream() throws IOException {
		var pipe = Pipe.open();
		
		this.nioSource = pipe.source();
		this.nioSink = pipe.sink();
	}
	
	public boolean isEnded() {
		return ended;
	}
	
	/**
	 * Returns the source to which bytes are being written
	 */
	public WritableByteChannel getSink() {
		return nioSink;
	}
	
	/**
	 * Returns the source to which bytes are being written
	 */
	public ReadableByteChannel getSource() {
		return nioSource;
	}
	
	@Override
	public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		this.exceptionHandler = handler;
		return this;
	}
	
	@Override
	public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
		// since we never declare the writeQueue as full in this implementation
		// we don't need to do anything with the drain handler as it won't be called.
		return this;
	}
	
	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data, Handler<AsyncResult<Void>> handler) {
		try {
			var byteBuf = data.getByteBuf();
			byteBuf.resetReaderIndex();
			for (var nbuf : byteBuf.nioBuffers()) nioSink.write(nbuf);
			
			if (handler != null) {
				// generic void success
				handler.handle(new AsyncResult<Void> () {
					@Override public Void result() { return null; }
					@Override public Throwable cause() { return null; }
					@Override public boolean succeeded() { return true; }
					@Override public boolean failed() { return false; }
				});
			}
		}
		catch (IOException e) {
			if (this.exceptionHandler != null) {
				this.exceptionHandler.handle(e);
			}

			if (handler != null) {
				// generic void failure
				handler.handle(new AsyncResult<Void> () {
					@Override public Void result() { return null; }
					@Override public Throwable cause() { return e; }
					@Override public boolean succeeded() { return false; }
					@Override public boolean failed() { return true; }
				});
			}
		}
		
		return this;
	}

	@Override
	public WriteStream<Buffer> write(Buffer data) {
		write(data, result -> {});
		return this;
	}
	
	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		this.ended = true;
		
		try {
			this.nioSink.close();
			
			if (handler != null) {
				// generic void success
				handler.handle(new AsyncResult<Void> () {
					@Override public Void result() { return null; }
					@Override public Throwable cause() { return null; }
					@Override public boolean succeeded() { return true; }
					@Override public boolean failed() { return false; }
				});
			}
		}
		catch (Exception e) {
			if (this.exceptionHandler != null) {
				this.exceptionHandler.handle(e);
			}

			if (handler != null) {
				// generic void failure
				handler.handle(new AsyncResult<Void> () {
					@Override public Void result() { return null; }
					@Override public Throwable cause() { return e; }
					@Override public boolean succeeded() { return false; }
					@Override public boolean failed() { return true; }
				});
			}
		}
	}
	
	@Override
	public void end() {
		end(result -> { });
	}
}
