package main.java.team.cs425.g54;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;
import org.json.JSONException;


public class MP {
	public Node myNode;
	// Set a contactor(fixed)
	public Node introducer = new Node(1,"172.22.158.178", 12345);
	// Membership List & Group
	// Make sure they are thread-safe
	public static CopyOnWriteArrayList<Node> membershipList = new CopyOnWriteArrayList<Node>();
	public CopyOnWriteArrayList<Node> groupList = new CopyOnWriteArrayList<Node>();
	// Listen & Ping
	public Listener listener;
	public Pinger pinger;
	public int pingerPort = 12333;
	public int nodePort = 12345;
	public String configFile="mp.config";
	
	public void setConfig() {
		// Read From File to Know the ID of this VM
    	try {
    		BufferedReader in=new BufferedReader(new FileReader(configFile));
    		String line=in.readLine();
    		int count=1;
    		int myID=0;
    		if(line!=null) {
    			myID=Integer.parseInt(line);
    			line=in.readLine();
    		}
    		while(line!=null) {
    			String[] splites=line.split(";");
    			int id= Integer.parseInt(splites[0]);
    			String addr = splites[1];
    			int port = Integer.parseInt(splites[2]);
    			
    			if(id==myID) {
    				myNode=new Node(id,addr,port);
    				return;
    			}
    			line=in.readLine();
    		}
    		
    	}catch(IOException e){
    		e.printStackTrace();
    	}
	}
	
	public void init() {
		setConfig();
		pinger = new Pinger(pingerPort, membershipList);
		listener = new Listener(myNode, membershipList, groupList, myNode.nodeID==introducer.nodeID);
	}
	
	public int findPositionToInsert(Node node, CopyOnWriteArrayList<Node> nodeList) {
		if (nodeList.size()==0)
			return 0;
		for (int i=0;i<nodeList.size();i++) {
			if(node.nodeID<nodeList.get(i).nodeID) {
				return i;
			}
		}
		return nodeList.size();
	}
	
	private void joinNodeIntoGroup() {
		int pos = findPositionToInsert(myNode, groupList);
		if (pos==groupList.size()) {
			groupList.add(myNode);
		}
		else {
			groupList.add(pos,myNode);
		}
	}
	
	private void removeNodeFromGroup() {
		for (int i=0;i<groupList.size();i++) {
			Node node = groupList.get(i);
            if (node.nodeID==myNode.nodeID && node.nodeAddr.equals(myNode.nodeAddr) && node.nodePort==myNode.nodePort) {
                groupList.remove(i);
                return;
            }
        }
	}
	
	public void broadcastToAll(String type) {
		DatagramSocket ds;
		try {
			ds = new DatagramSocket(nodePort);
			for(Node node : groupList) {
	            JSONObject message = new JSONObject();
	            message.put("type", type);
	            message.put("nodeID", node.nodeID);
	            message.put("nodeAddr", node.nodeAddr);
	            message.put("nodePort", node.nodePort);
	            InetAddress address = InetAddress.getByName(node.nodeAddr);
	            DatagramPacket send_message = new DatagramPacket(message.toString().getBytes(), message.toString().getBytes().length, address, node.nodePort);
	            ds.send(send_message);
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sendMsgToIntroducer(String type) {
		try {
			DatagramSocket ds = new DatagramSocket(nodePort);
			JSONObject message = new JSONObject();
	        message.put("type", type);
	        message.put("nodeID", myNode.nodeID);
	        message.put("nodeAddr", myNode.nodeAddr);
	        message.put("nodePort", myNode.nodePort);
	        InetAddress address = InetAddress.getByName(introducer.nodeAddr);
	        DatagramPacket send_message = new DatagramPacket(message.toString().getBytes(), message.toString().getBytes().length, address, introducer.nodePort);
			ds.send(send_message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void joinGroup() {
		// If this node is Introducer
		if(myNode.nodeID==introducer.nodeID) {
			System.out.println("I am Introducer");
			joinNodeIntoGroup();
			broadcastToAll("join");
		}
		else {
			sendMsgToIntroducer("join");
		}
	}
	
	public void sendLeaveMsg() {
		DatagramSocket ds;
		try {
			ds = new DatagramSocket(nodePort);
			for(Node node : membershipList) {
	            JSONObject message = new JSONObject();
	            message.put("type", "leave");
	            message.put("nodeID", myNode.nodeID);
	            message.put("nodeAddr", myNode.nodeAddr);
	            message.put("nodePort", myNode.nodePort);
	            InetAddress address = InetAddress.getByName(node.nodeAddr);
	            DatagramPacket send_message = new DatagramPacket(message.toString().getBytes(), message.toString().getBytes().length, address, node.nodePort);
	            ds.send(send_message);
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e){
			e.printStackTrace();
		}
	}
	
	public void leaveGroup() {
		if(myNode.nodeID==introducer.nodeID) {
			removeNodeFromGroup();
			broadcastToAll("leave");
		}
		else {
			sendLeaveMsg();
		}
	}
	
	public void showID() {
		System.out.println("This is VM"+myNode.nodeID+", Node Address:"+myNode.nodeAddr+", Node Port:"+myNode.nodePort);
	}
	
	public void showMembershipList() {
		System.out.println("Number of Members:"+membershipList.size());
		for (int i=0;i<membershipList.size();i++) {
			Node node=membershipList.get(i);
			System.out.println("Member"+(i+1)+" :");
			System.out.println("Node ID:"+node.nodeID+", Node Address:"+node.nodeAddr+", Node Port:"+node.nodePort);
		}
	}
	
	public void showGroupList() {
		System.out.println("Size of group:"+groupList.size());
		for (int i=0;i<groupList.size();i++) {
			Node node=groupList.get(i);
			System.out.println("Member"+(i+1)+" :");
			System.out.println("Node ID:"+node.nodeID+", Node Address:"+node.nodeAddr+", Node Port:"+node.nodePort);
		}
	}

	
	public static void main(String[] args) {  
		
		MP mp = new MP();
		mp.init();
		// User input
		InputStreamReader is_reader = new InputStreamReader(System.in);
		while(true) {
			try {
				System.out.println("Input Your Command:");
				String cmdInput = new BufferedReader(is_reader).readLine();
				// Quit this program
				if(cmdInput.toLowerCase().equals("quit")) {
					break;
				}
				
				// Join command
				if(cmdInput.toLowerCase().equals("join")) {
					mp.joinGroup();
					mp.showGroupList();
				}
				
				if(cmdInput.toLowerCase().equals("leave")) {
					mp.leaveGroup();
				}
				if(cmdInput.toLowerCase().equals("show")) {
					mp.showID();
					mp.showMembershipList();
				}
				if(cmdInput.toLowerCase().equals("introducer")) {
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
	}
	
	
}
