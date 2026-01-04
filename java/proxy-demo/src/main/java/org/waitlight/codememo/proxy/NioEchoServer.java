package org.waitlight.codememo.proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NioEchoServer {

    private static final Logger logger = LogManager.getLogger(NioEchoServer.class);

    private static final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private static final int PORT = 8080;
    private static boolean running = true;

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(PORT));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (running) {
            if (selector.select() < 1) continue;
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isAcceptable()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_ACCEPT));
                    accept(selector, key);
                } else if (key.isReadable()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                    Thread.ofVirtual().start(() -> read(selector, key));
                } else if (key.isWritable()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
                    write(selector, key);
                }
            }
        }
    }

    private static void accept(Selector selector, SelectionKey key) {
        logger.info(">>> accepted");
        key.interestOps(key.interestOps() & (~SelectionKey.OP_ACCEPT));
        try {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            logger.info("[accepted] connection from {}", client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void read(Selector selector, SelectionKey key) {
        logger.info("{} >>> readable", Thread.currentThread());
        try {
            SocketChannel client = (SocketChannel) key.channel();
            int count;
            buffer.clear();
            while ((count = client.read(buffer)) > 0) {
                buffer.flip();
                String str = StandardCharsets.UTF_8.decode(buffer).toString();
                logger.info("[read] str: {}", str);
                ByteBuffer buffer = ByteBuffer.wrap("Hello client".getBytes());
                client.write(buffer);
                buffer.clear();
            }

            if (count < 0) {
                client.close();
                logger.info("[read] client closed the connection");
            }

            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            key.selector().wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write(Selector selector, SelectionKey key) {
        logger.info(">>> writeable");
        key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
        try {
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.wrap("Hello client".getBytes());
            client.write(buffer);
            logger.info("[write] client");
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
