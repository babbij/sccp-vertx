package com.colabriq.vertx;

import java.io.BufferedReader;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

import com.colabriq.vertx.stream.ReadableWriteStream;
import com.colabriq.vertx.stream.WritableReadStream;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

public class TestClient {
	public static void main(String[] args) throws Exception {
		var vertx = Vertx.vertx();
		
		var client = WebClient.create(
			vertx, 
			new WebClientOptions()
				.setKeepAlive(true)
				.setProtocolVersion(HttpVersion.HTTP_2)
				.setHttp2ClearTextUpgrade(true)
		);
		
		var future = new CompletableFuture<AsyncResult<HttpResponse<Void>>>();
		
		var rStream = new WritableReadStream();
		var wStream = new ReadableWriteStream();
		
//		// start the reader thread now
//		new Thread(() -> {
//			try {
//				var is = new BufferedReader(Channels.newReader(wStream, Charset.defaultCharset().name()));
//				String i; int y = 0;
//				while ((i = is.readLine()) != null) {
//					System.out.println(y++ + ": " + i);
////					Thread.sleep(1000);
//				}
//				
//				is.close();
//			}
//			catch (Exception e) {
//				e.printStackTrace();
//			}
//		}).start();
		
		var rStream2 = new WritableReadStream();
		var wStream2 = new ReadableWriteStream();
		
		client
			.postAbs("http://localhost:8080/rpc")
			.as(BodyCodec.pipe(wStream2))
			.sendStream(rStream, result -> {
				if (result.succeeded()) {
					System.out.println("---------- Succeeded! ----------");
					System.out.println(result.result().body());
				}
				else {
					System.out.println("---------- Failure :( ----------");
					result.cause().printStackTrace();
				}
			})
		;
		
		var os = Channels.newOutputStream(rStream);
		os.write("Hello\n".getBytes());
		os.close();
		
		client
			.postAbs("http://localhost:8080/rpc")
			.as(BodyCodec.pipe(wStream2))
			.sendStream(rStream, result -> {
				if (result.succeeded()) {
					System.out.println("---------- Succeeded! ----------");
					System.out.println(result.result().body());
				}
				else {
					System.out.println("---------- Failure :( ----------");
					result.cause().printStackTrace();
				}
			})
		;
		
		var os2 = Channels.newOutputStream(rStream2);
		os2.write("Hello\n".getBytes());
		os2.close();
	}
}
