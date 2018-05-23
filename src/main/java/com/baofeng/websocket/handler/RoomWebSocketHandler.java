package com.baofeng.websocket.handler;


import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.baofeng.websocket.control.Room;
import com.baofeng.websocket.service.Global;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class RoomWebSocketHandler extends SimpleChannelInboundHandler<Object>{

	private static final Logger logger = Logger.getLogger(WebSocketServerHandshaker.class.getName());
	
	private final StringBuilder responseContent = new StringBuilder();
	
	private WebSocketServerHandshaker handShaker;
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		}else if (msg instanceof WebSocketFrame) {
			handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
		
	}
	
	

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Global.group.add(ctx.channel());
		System.out.println("start connetion");
	}



	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Global.group.remove(ctx.channel());
		System.out.println("end connetion");
	}


	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		//close socket msg
		if(frame instanceof CloseWebSocketFrame) {
			handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		
		//ping information
		if(frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		
		//only support text not binary
		if(!(frame instanceof TextWebSocketFrame)) {
			System.out.println("not support binary msg");
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
		}
		
		
		String request = ((TextWebSocketFrame)frame).text();
		
		//rewrite request info
		TextWebSocketFrame twf = null;
		Room roomC = new Room();
		if(request.equals("match")) {
			roomC.matchRoom(ctx);

		}else if (request.equals("exit")) {
			roomC.exitRoom(ctx);
		}else {
			//send to group
			twf = new TextWebSocketFrame(new Date().toString()+":"+request);
			Global.group.writeAndFlush(twf);
		}

		
		//resend to the sender
		//ctx.writeAndFlush(twf);
	}
	
	
	
	private void handleHttpRequest2(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
		 /**
         * 在服务器端打印请求信息
         */
        System.out.println("VERSION: " + fullHttpRequest.protocolVersion().text() + "\r\n");
        System.out.println("REQUEST_URI: " + fullHttpRequest.uri() + "\r\n\r\n");
        System.out.println("\r\n\r\n");
        for (Entry<CharSequence, CharSequence> entry : fullHttpRequest.headers()) {
            System.out.println("HEADER: " + entry.getKey() + '=' + entry.getValue() + "\r\n");
        }

        /**
         * 服务器端返回信息
         */
        responseContent.setLength(0);
        responseContent.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
        responseContent.append("===================================\r\n");

        responseContent.append("VERSION: " + fullHttpRequest.protocolVersion().text() + "\r\n");
        responseContent.append("REQUEST_URI: " + fullHttpRequest.uri() + "\r\n\r\n");
        responseContent.append("\r\n\r\n");
        for (Entry<CharSequence, CharSequence> entry : fullHttpRequest.headers()) {
            responseContent.append("HEADER: " + entry.getKey() + '=' + entry.getValue() + "\r\n");
        }
        responseContent.append("\r\n\r\n");
//        Set<Cookie> cookies;
//        String value = fullHttpRequest.headers().get(COOKIE);
//        if (value == null) {
//            cookies = Collections.emptySet();
//        } else {
//            cookies = CookieDecoder.decode(value);
//        }
//        for (Cookie cookie : cookies) {
//            responseContent.append("COOKIE: " + cookie.toString() + "\r\n");
//        }
//        responseContent.append("\r\n\r\n");

        if (fullHttpRequest.method().equals(HttpMethod.GET)) {
            //get请求
            QueryStringDecoder decoderQuery = new QueryStringDecoder(fullHttpRequest.uri());
            Map<String, List<String>> uriAttributes = decoderQuery.parameters();
            //请求参数
            for (Entry<String, List<String>> attr : uriAttributes.entrySet()) {
                for (String attrVal : attr.getValue()) {
                    responseContent.append("URI: " + attr.getKey() + '=' + attrVal + "\r\n");
                }
            }
            responseContent.append("\r\n\r\n");

            responseContent.append("\r\n\r\nEND OF GET CONTENT\r\n");
            ByteBuf buf = Unpooled.copiedBuffer(responseContent.toString(),
    				CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
            sendHttpResponse(ctx, fullHttpRequest, (DefaultFullHttpResponse) response);
            return;
        } 
//        else if (fullHttpRequest.method().equals(HttpMethod.POST)) {
//            //post请求
//            decoder = new HttpPostRequestDecoder(factory, fullHttpRequest);
//            readingChunks = HttpHeaders.isTransferEncodingChunked(fullHttpRequest);
//            responseContent.append("Is Chunked: " + readingChunks + "\r\n");
//            responseContent.append("IsMultipart: " + decoder.isMultipart() + "\r\n");
//
//            try {
//                while (decoder.hasNext()) {
//                    InterfaceHttpData data = decoder.next();
//                    if (data != null) {
//                        try {
//                            writeHttpData(data);
//                        } finally {
//                            data.release();
//                        }
//                    }
//                }
//            } catch (EndOfDataDecoderException e1) {
//                responseContent.append("\r\n\r\nEND OF POST CONTENT\r\n\r\n");
//            }
//            writeResponse(ctx.channel());
//            return;
//        } 
	else {
            System.out.println("discard.......");
            return;
        }
	}
	
	private void handleHttpRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) {
		//request failed
		if (!req.decoderResult().isSuccess()
				|| (!"websocket".equals(req.headers().get("Upgrade")))) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
					HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				"ws://localhost:7397/websocket", null, false);
		handShaker = wsFactory.newHandshaker(req);
		if (handShaker == null) {
			WebSocketServerHandshakerFactory
					.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			handShaker.handshake(ctx.channel(), req);
		}
	}
	
	
	private static void sendHttpResponse(ChannelHandlerContext ctx,
			FullHttpRequest req, DefaultFullHttpResponse res) {
		// 返回应答给客户端
		if (res.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		// 如果是非Keep-Alive，关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	private static boolean isKeepAlive(FullHttpRequest req) {
		return false;
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
	
	
	
	
	
	
	
}
