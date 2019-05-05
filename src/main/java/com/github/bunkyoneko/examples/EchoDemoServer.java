package com.github.bunkyoneko.examples;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Echo demo server in NIO2
 *
 * @author bunkyoneko
 * @date 04/28/2019
 */
public class EchoDemoServer {
    public void serve(int port) throws IOException {
        System.out.println("Listening for connections on port " + port);
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(Executors.newSingleThreadExecutor());
        final AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open(group);
        InetSocketAddress address = new InetSocketAddress(port);
        serverChannel.bind(address);
        final CountDownLatch latch = new CountDownLatch(1);

        serverChannel.accept(null, new
            CompletionHandler<AsynchronousSocketChannel, Object>() {
                @Override
                public void completed(final AsynchronousSocketChannel channel, Object attachment) {
                     //If the server socket channel is still open,
                     //we call the accept API again to get ready for another incoming connection while reusing the
                     //same handler.
                    if (serverChannel.isOpen()) {
                        serverChannel.accept(null, this);
                    }
                    ByteBuffer byteBuffer = ByteBuffer.allocate(100);
                    channel.read(byteBuffer, byteBuffer, new EchoCompletionHandler(channel));
                }

                @Override
                public void failed(Throwable throwable, Object attachment) {
                    try {
                        serverChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        );

        // wait until group.shutdown()/shutdownNow(), or the thread is interrupted:
        try {
            latch.await();
        } catch (InterruptedException e) {
            serverChannel.close();
            e.printStackTrace();
        }

        System.out.println("In Serve method " + Thread.currentThread().getId());
    }

    private final class EchoCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {
        private AsynchronousSocketChannel channel;
        final CountDownLatch latch = new CountDownLatch(1);

        EchoCompletionHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void completed(Integer result, ByteBuffer buffer) {
            buffer.flip();

            System.out.println("In EchoCompletionHandler completed method " + Thread.currentThread().getId());

            channel.write(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    System.out.println("In channel write completed method " + Thread.currentThread().getId());

                    if (buffer.hasRemaining()) {
                        channel.write(buffer, buffer, this);
                        buffer.compact();
                    } else {
                        buffer.compact();
                        channel.read(buffer, buffer, this);
                    }

                    latch.countDown();
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void failed(Throwable throwable, ByteBuffer attachment) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
