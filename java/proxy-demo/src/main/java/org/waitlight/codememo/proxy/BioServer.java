package org.waitlight.codememo.proxy;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BioServer {

    private static final Logger logger = LogManager.getLogger(BioServer.class);

    public static void main(String[] args) throws IOException {
        int localPort = 8080;   // Local server port
        ServerSocket serverSocket = new ServerSocket(localPort);
        logger.info("Proxy server is listening on port " + localPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();    // Accept client connection
            Thread.ofVirtual().start(() -> handleRequest(clientSocket));
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try {
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput));
            String line = reader.readLine();
            logger.info(line);

            if (line.startsWith("CONNECT")) {
                String[] requestLineParts = line.split(" ")[1].split(":");
                String host = requestLineParts[0]; // 提取主机名和端口
                int port = Integer.parseInt(requestLineParts[1]);
                logger.info("Host: {}, port: {}", host, port);

                try (Socket targetSocket = new Socket(host, port)) {  // 与目标服务器创建连接
                    logger.info("Target socket created");
                    // 发送HTTP 200响应到客户端
                    PrintWriter clientPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientPrintWriter.println("HTTP/1.1 200 Connection Established");
                    clientPrintWriter.println("Proxy-agent: SimpleProxy/1.0");
                    clientPrintWriter.println();  // HTTP 头部后的空行
                    clientPrintWriter.flush();

                    // 客户端到目标服务器的线程
                    Thread clientToServerThread = Thread.ofVirtual().start(() -> {
                        try {
                            logger.info("client 2 server");
                            copyStream(clientSocket.getInputStream(), targetSocket.getOutputStream());
                        } catch (IOException e) {
                            // 错误处理
                        }
                    });

                    // 目标服务器到客户端的线程
                    Thread serverToClientThread = Thread.ofVirtual().start(() -> {
                        try {
                            logger.info("server 2 client");
                            copyStream(targetSocket.getInputStream(), clientSocket.getOutputStream());
                        } catch (IOException e) {
                            // 错误处理
                        }
                    });

                    // 等待线程结束
                    clientToServerThread.join();
                    serverToClientThread.join();
                } catch (Exception e) {
                    // 连接失败时向客户端报告错误
                    // 发送 HTTP 500 Internal Server Error 或者相关错误代码
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // copyStream 方法定义。这个方法将字节从输入流复制到输出流
    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}