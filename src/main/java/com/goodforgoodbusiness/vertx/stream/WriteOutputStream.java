package com.goodforgoodbusiness.vertx.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;

import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * Provides an implementation of the {@link WriteStream} that can be used as an {@link OutputStream}
 */
public class WriteOutputStream extends OutputStream {
	private final WriteStream<Buffer> writeStream;
	
	public WriteOutputStream(WriteStream<Buffer> writeStream) {
		this.writeStream = writeStream;
	}
	
	@Override
	public void write(int b) throws IOException {
		var blocker = new CompletableFuture<AsyncResult<Void>>();
		writeStream.write(Buffer.buffer(new byte [] { (byte)b }), result -> blocker.complete(result));
		
//		while (true) {
//			try {
//				var result = blocker.take();
//				if (result.succeeded()) {
//					return;
//				}
//				else {
//					throw new IOException(result.cause());
//				}
//			}
//			catch (InterruptedException e) {
//			}
//		}
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		var blocker = new CompletableFuture<AsyncResult<Void>>();
		writeStream.write(Buffer.buffer(Unpooled.wrappedBuffer(b, off, len)), result -> blocker.complete(result));
		
//		while (true) {
//			try {
//				var result = blocker.take();
//				if (result.succeeded()) {
//					return;
//				}
//				else {
//					throw new IOException(result.cause());
//				}
//			}
//			catch (InterruptedException e) {
//			}
//		}
	}
	
	@Override
	public void flush() throws IOException {
	}
	
	@Override
	public void close() throws IOException {
		writeStream.end();
	}
}
