package de.teammeet.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.util.Log;

import de.teammeet.service.ServiceInterfaceImpl;

public class GroupMessageListener implements PacketListener {

	private static String CLASS = GroupMessageListener.class.getSimpleName();
	
	ServiceInterfaceImpl mServiceImpl = null;
	
	public GroupMessageListener(ServiceInterfaceImpl serviceImpl) {
		mServiceImpl = serviceImpl;
	}
	
	@Override
	public void processPacket(Packet packet) {
		String from = packet.getFrom();
		String xml = packet.toXML();
		Log.d(CLASS, from + " sent " + xml);
	}

}
