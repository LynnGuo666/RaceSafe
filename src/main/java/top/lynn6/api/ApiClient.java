package top.lynn6.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lynn6.config.ConfigManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class ApiClient {
    public static final Logger LOGGER = LoggerFactory.getLogger(top.lynn6.RaceSafe.MOD_ID + "/Api");
    private static final HttpClient client = HttpClient.newHttpClient();
   
    public static HttpResponse<String> sendPostRequest(String path, String jsonBody) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
    	String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    	String signature = generateSignature("POST", path, timestamp, getMD5Hash(jsonBody));
    	LOGGER.debug("[{}] Sending POST request to {}. Timestamp: {}, Body MD5: {}", top.lynn6.RaceSafe.MOD_ID, path, timestamp, getMD5Hash(jsonBody));
   
    	HttpRequest request = HttpRequest.newBuilder()
    			.uri(URI.create(ConfigManager.serverUrl + path))
    			.header("Content-Type", "application/json")
    			.header("x-mod-timestamp", timestamp)
    			.header("Authorization", "MOD-HMAC-SHA256 " + ConfigManager.accessKey + ":" + signature)
    			.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    			.build();
   
    	return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
   
    public static HttpResponse<String> sendGetRequest(String path) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
    	String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    	// GET请求没有请求体，所以MD5哈希为空字符串
    	String signature = generateSignature("GET", path, timestamp, "");
    	LOGGER.debug("[{}] Sending GET request to {}. Timestamp: {}", top.lynn6.RaceSafe.MOD_ID, path, timestamp);
   
    	HttpRequest request = HttpRequest.newBuilder()
    			.uri(URI.create(path.startsWith("http") ? path : ConfigManager.serverUrl + path))
    			.header("x-mod-timestamp", timestamp)
    			.header("Authorization", "MOD-HMAC-SHA256 " + ConfigManager.accessKey + ":" + signature)
    			.GET()
    			.build();
   
    	return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
   
    public static HttpResponse<String> sendScreenshot(String submissionUrl, String metadataJson, byte[] imageBytes) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
    	String path = new java.net.URL(submissionUrl).getPath();
    	String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    	// 根据规范，multipart请求的MD5哈希为空
    	String signature = generateSignature("POST", path, timestamp, "");
    	LOGGER.info("[{}] Preparing to send screenshot to {}. Path for signing: {}", top.lynn6.RaceSafe.MOD_ID, submissionUrl, path);
   
    	String boundary = "Boundary-" + System.currentTimeMillis();
   
    	HttpRequest request = HttpRequest.newBuilder()
    			.uri(URI.create(submissionUrl))
    			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
    			.header("x-mod-timestamp", timestamp)
    			.header("Authorization", "MOD-HMAC-SHA256 " + ConfigManager.accessKey + ":" + signature)
    			.POST(ofMimeMultipartData(metadataJson, imageBytes, boundary))
    			.build();
   
    	LOGGER.info("[{}] Sending screenshot...", top.lynn6.RaceSafe.MOD_ID);
    	return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
   
    private static HttpRequest.BodyPublisher ofMimeMultipartData(String metadataJson, byte[] imageBytes, String boundary) {
    	var byteArrays = new ArrayList<byte[]>();
    	String boundaryString = "--" + boundary + "\r\n";
    	String finalBoundaryString = "--" + boundary + "--\r\n";
   
    	// Metadata part
    	byteArrays.add(boundaryString.getBytes(StandardCharsets.UTF_8));
    	byteArrays.add("Content-Disposition: form-data; name=\"metadata\"\r\n".getBytes(StandardCharsets.UTF_8));
    	byteArrays.add("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
    	byteArrays.add(metadataJson.getBytes(StandardCharsets.UTF_8));
    	byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
   
    	// Screenshot part
    	byteArrays.add(boundaryString.getBytes(StandardCharsets.UTF_8));
    	byteArrays.add("Content-Disposition: form-data; name=\"screenshot\"; filename=\"screenshot.png\"\r\n".getBytes(StandardCharsets.UTF_8));
    	byteArrays.add("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
    	byteArrays.add(imageBytes);
    	byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
   
    	// Final boundary
    	byteArrays.add(finalBoundaryString.getBytes(StandardCharsets.UTF_8));
   
    	return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    private static String generateSignature(String method, String path, String timestamp, String bodyMd5) throws NoSuchAlgorithmException, InvalidKeyException {
        String stringToSign = method + "\n" + path + "\n" + timestamp + "\n" + bodyMd5;

        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(ConfigManager.secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);

        byte[] hmacBytes = sha256Hmac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    private static String getMD5Hash(String input) throws NoSuchAlgorithmException {
        if (input == null || input.isEmpty()) {
            return "";
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}