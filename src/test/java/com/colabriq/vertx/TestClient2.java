package com.colabriq.vertx;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.util.concurrent.CompletableFuture;

public class TestClient2 {
	public static void main(String[] args) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.version(Version.HTTP_2)
		    .uri(new URI("http://localhost:8080/rpc"))
		    .headers("Content-Type", "text/plain;charset=UTF-8")
		    .POST(HttpRequest.BodyPublishers.ofString("Hello"))
		    .build();
		
		CompletableFuture<HttpResponse<String>> response = HttpClient.newBuilder()
			.build()
			.sendAsync(request, HttpResponse.BodyHandlers.ofString());

		var result = response.get();
		
		System.out.println(result.statusCode());
		
		System.out.println(result.body());
		
	}
}
