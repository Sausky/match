package com.baofeng.websocket.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Global {
	//TODO
	public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	public static Map<String, ChannelGroup> rooms = new HashMap<>();
	
	public static ChannelGroup matcheringGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	public static Map<Channel, String> roomKey = new HashMap<>();
	
	public static Map<Channel, Integer> matcherTime = new HashMap<>();
	
	public static Set<Channel> useId = new HashSet<>();
	
}
