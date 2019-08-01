package com.colabriq.vertx;

import java.nio.ByteBuffer;

import io.vertx.core.buffer.Buffer;


public class Test {
	public static void main(String[] args) {
		var b1 = ByteBuffer.wrap("hello".getBytes());
		
		b1.position(3);
		b1.limit(4);
		
		var b2 = ByteBuffer.allocate(1);		
		b2.put(b1);
		
		System.out.println(b2.remaining());
		System.out.println(b1.position());
		System.out.println(b1.limit());
	}
}
