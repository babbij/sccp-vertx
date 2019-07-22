package com.goodforgoodbusiness.vertx.stream;

import java.io.IOException;
import java.io.OutputStream;

import io.netty.buffer.Unpooled;
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
		writeStream.write(Buffer.buffer(new byte [] { (byte)b }));
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		writeStream.write(Buffer.buffer(Unpooled.wrappedBuffer(b, off, len)));
	}
	
	@Override
	public void flush() throws IOException {
	}
	
	@Override
	public void close() throws IOException {
		writeStream.end();
	}
}
