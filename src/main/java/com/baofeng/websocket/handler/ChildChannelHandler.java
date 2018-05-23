package com.baofeng.websocket.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		//TODO
		ch.pipeline().addLast("http-codec",new HttpServerCodec());
		ch.pipeline().addLast("aggregator",new HttpObjectAggregator(65535));
		ch.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
		ch.pipeline().addLast("handler", new RoomWebSocketHandler());
		
	}

}
