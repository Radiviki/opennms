/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.telemetry.listeners;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.opennms.netmgt.telemetry.api.receiver.Listener;
import org.opennms.netmgt.telemetry.api.receiver.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.SocketUtils;

public class TcpListener implements Listener {
    private static final Logger LOG = LoggerFactory.getLogger(TcpListener.class);

    private final String name;
    private final TcpParser parser;

    private final Meter packetsReceived;

    private String host = null;
    private int port = 50000;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ChannelFuture socketFuture;

    private ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public TcpListener(final String name,
                       final TcpParser parser,
                       final MetricRegistry metrics) {
        this.name = Objects.requireNonNull(name);
        this.parser = Objects.requireNonNull(parser);

        packetsReceived = metrics.meter(MetricRegistry.name("listeners", name, "packetsReceived"));
    }

    public void start() throws InterruptedException {
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();

        this.parser.start(this.bossGroup);

        final InetSocketAddress address = this.host != null
                ? SocketUtils.socketAddress(this.host, this.port)
                : new InetSocketAddress(this.port);

        this.socketFuture = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        final TcpParser.Handler session = TcpListener.this.parser.accept(ch.remoteAddress(), ch.localAddress());
                        ch.pipeline()
                                .addFirst(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public  void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        packetsReceived.mark();
                                        super.channelRead(ctx, msg);
                                    }
                                })
                                .addLast(new ByteToMessageDecoder() {
                                    @Override
                                    protected void decode(final ChannelHandlerContext ctx,
                                                          final ByteBuf in,
                                                          final List<Object> out) throws Exception {
                                        session.parse(in)
                                               .ifPresent(out::add);
                                    }

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelActive(ctx);
                                        session.active();
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelInactive(ctx);
                                        session.inactive();
                                    }
                                })
                                .addLast(new SimpleChannelInboundHandler<CompletableFuture<?>>() {
                                    @Override
                                    protected void channelRead0(final ChannelHandlerContext ctx,
                                                                final CompletableFuture<?> future) throws Exception {
                                        future.handle((result, ex) -> {
                                            if (ex != null) {
                                                ctx.fireExceptionCaught(ex);
                                            }
                                            return result;
                                        });
                                    }
                                })
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
                                        LOG.warn("Invalid packet: {}", cause.getMessage());
                                        LOG.debug("", cause);

                                        session.inactive();

                                        ctx.close();
                                    }
                                });
                    }

                    @Override
                    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
                        TcpListener.this.channels.add(ctx.channel());
                        super.channelActive(ctx);
                    }

                    @Override
                    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                        TcpListener.this.channels.remove(ctx.channel());
                        super.channelInactive(ctx);
                    }
                })
                .bind(address)
                .sync();
    }

    public void stop() throws InterruptedException {
        LOG.info("Closing channel...");
        if (this.socketFuture != null) {
            this.socketFuture.channel().close().sync();
        }

        LOG.info("Disconnecting clients...");
        this.channels.close().awaitUninterruptibly();

        LOG.info("Stopping parser...");
        if (this.parser != null) {
            this.parser.stop();
        }

        LOG.info("Closing boss group...");
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully().sync();
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return String.format("TCP %s:%s",  this.host != null ? this.host : "*", this.port);
    }

    @Override
    public Collection<? extends Parser> getParsers() {
        return Collections.singleton(this.parser);
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }
}
