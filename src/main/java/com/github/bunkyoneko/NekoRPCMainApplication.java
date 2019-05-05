package com.github.bunkyoneko;

import java.io.IOException;

import com.github.bunkyoneko.examples.NettyEchoServer;

/**
 * Main class
 *
 * @author bunkyoneko
 * @date 04/28/2019
 */
public class NekoRPCMainApplication {
    public static void main(String[] args) {
        NettyEchoServer server = new NettyEchoServer(6666);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Hello World");
    }
}
