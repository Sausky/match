package com.baofeng.websocket.control;

import java.util.ArrayList;
import java.util.List;


import com.baofeng.websocket.service.Global;
import com.baofeng.websocket.util.MD5Hasher;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Room {
 
	
	private static final int MAX_WAITING_TIME = 3;
	
	/** 
	 * 獲取匹配對手
	 *
	 * @param room
	 * @return List<Channel> 
	 */
	public static List<Channel> getMatchers(ChannelGroup room) {
		List<Channel> ids = new ArrayList<Channel>();
		if(room == null) {
			Global.group.forEach(p->ids.add(p));
		}else {
			room.forEach(p->ids.add(p));
		}
		return ids;
	}
	
	/** 
	 * 退出房間
	 *
	 * @param ctx      
	 * @return void 
	 */
	public void exitRoom(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		//验证id是否已在房间中
		if(Global.useId.contains(channel)) {
			//房间中用户中移除
			Global.useId.remove(channel);
			//获取房间的秘钥
			String roomKey = Global.roomKey.get(channel);
			//获取房间
			ChannelGroup room = Global.rooms.get(roomKey);
			//房间中移除退出用户
			room.remove(channel);
			//丢弃钥匙
			Global.roomKey.remove(channel);
			List<Channel> matchers = getMatchers(room);
			TextWebSocketFrame twf = new TextWebSocketFrame("退出成功！");
			ctx.writeAndFlush(twf);
			matchers.forEach(p->{
				if(p != channel) {
					//房间中用户中移除
					Global.useId.remove(p);
					//丢弃钥匙
					Global.roomKey.remove(p);
					TextWebSocketFrame twf2 = new TextWebSocketFrame("您的对手已退出，房间失效！");
					room.writeAndFlush(twf2);
					//房间关闭
					Global.rooms.remove(roomKey);
				}
			});
		}
	}
	
	/** 
	 * 匹配房間
	 *
	 * @param ctx
	 */
	public void matchRoom(ChannelHandlerContext ctx) {

		
		Channel channel = ctx.channel();
		TextWebSocketFrame twf = null;
		
		if(Global.matcheringGroup.contains(channel)) {
			
			synchronized(Global.matcherTime) {
				if((int)(System.currentTimeMillis()/1000)-Global.matcherTime.get(channel).intValue()>MAX_WAITING_TIME) {
					//从匹配队列中删除这个用户
					Global.matcheringGroup.remove(channel);
					//删除过期时间
					
						Global.matcherTime.remove(channel);
					
					twf = new TextWebSocketFrame("匹配超时，请过一会再匹配");
					ctx.writeAndFlush(twf);
				}else {
					twf = new TextWebSocketFrame("正在匹配中，请稍等");
					ctx.writeAndFlush(twf);
				}
			}
			return;
		}else {
			//加入匹配队列
			Global.matcheringGroup.add(channel);
			synchronized(Global.matcherTime) {
				Global.matcherTime.put(channel, (int) (System.currentTimeMillis()/1000));
			}
			
		}
		

		//验证id是否已在房间中
		if(Global.useId.contains(channel)) {
			twf = new TextWebSocketFrame("您已在房间中，无法匹配");
			ctx.writeAndFlush(twf);
			return;
		}
		ChannelGroup room = null;
		List<Channel> ids = getMatchers(Global.matcheringGroup);
		if(ids.size()>1) {
			for(int i = 0; i<ids.size(); i++) {
				//存在其他可用channel
				if(ids.get(i) != channel&&!Global.useId.contains(ids.get(i))) {
					room =  arrangRoom(channel, ids.get(i));
					break;
				}
			}
		}

		
		if(null != room) {
			twf = new TextWebSocketFrame("匹配成功！");
			room.writeAndFlush(twf);
		}else {
			twf = new TextWebSocketFrame("匹配中，请稍后");
			ctx.writeAndFlush(twf);
		}
		
		return;
	}
	
	/** 
	 * 安排房间
	 *
	 * @param c1
	 * @param c2
	 * @return      
	 * @return: ChannelGroup 
	 */
	public ChannelGroup arrangRoom(Channel c1,Channel c2) {
		//建立私人房间
		ChannelGroup newGlobal = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		newGlobal.add(c1);
		newGlobal.add(Global.group.find(c2.id()));
		String roomKey = new MD5Hasher().getMd5Str(String.valueOf(System.currentTimeMillis()));
		//给房间绑定两个用户
		Global.rooms.put(roomKey,newGlobal);
		Global.useId.add(c1);
		Global.useId.add(c2);
		//给予用户钥匙
		Global.roomKey.put(c1, roomKey);
		Global.roomKey.put(c2, roomKey);
		//从匹配队列中删除这两个用户
		Global.matcheringGroup.remove(c1);
		Global.matcheringGroup.remove(c2);
		//删除过期时间
		synchronized(Global.matcherTime) {
			Global.matcherTime.remove(c1);
			Global.matcherTime.remove(c2);
		}

		return newGlobal;
	}
	
	/** 
	 * 過濾匹配超時
	 *      
	 * @return void 
	 */
	public void checkMatchingTime() {
		List<Channel> channels = new ArrayList<>();

		synchronized(Global.matcherTime) {
			//改用redis或前端控制
			Global.matcherTime.forEach((c,t)->{
				if((int)(System.currentTimeMillis()/1000)-t.intValue()>MAX_WAITING_TIME) {
					channels.add(c);
				}
			});
		}

		channels.forEach(c->{
			//从匹配队列中删除这个用户
			Global.matcheringGroup.remove(c);
			//删除过期时间
			Global.matcherTime.remove(c);
			TextWebSocketFrame twf = new TextWebSocketFrame("匹配超时，请过一会再匹配");
			c.writeAndFlush(twf);
		});
	}
}
