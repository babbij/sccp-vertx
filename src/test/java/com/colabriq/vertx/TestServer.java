package com.goodforgoodbusiness.vertx;

import java.io.IOException;
import java.nio.channels.Channels;

import com.goodforgoodbusiness.vertx.stream.WritableReadStream;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class TestServer {
	public static void main(String[] args) {
		var vertx = Vertx.vertx();
		var router = Router.router(vertx);
		
		router.post("/rpc").handler(ctx -> {
			ctx.response().putHeader("Content-type", "text/plain");
			ctx.response().setChunked(true);
			
			var rStream = new WritableReadStream();
			rStream.pipeTo(ctx.response());
			
			new Thread(() -> {
				try {	
					var writer = Channels.newWriter(rStream, "US-ASCII");
					
					for (int i = 0; i < 20; i++) {
						writer.write("testing testing 1 2 3 4 5 6 7 8 9 0\n");
					}
					
					writer.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}).start();
		});
		
		vertx
			.createHttpServer()
			.requestHandler(router)
			.listen(8080)
		;
	}
}
