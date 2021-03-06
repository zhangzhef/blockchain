package com.v5ent.p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.v5ent.domain.Block;
/**
 * 结点链通讯线程
 * @author Mignet
 * 
 */
public class NodeThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeThread.class);
	final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private Socket socket=null;
	private List<Block> blockChain = null;
	
	public NodeThread(Socket socket,List<Block> blockChain){
		this.socket = socket;
		this.blockChain = blockChain;
	}
	
	
	/**
	 * 如果有别的链比你长，就用比你长的链作为当前链
	 * 
	 * @param newBlocks
	 */
	public void replaceChain(List<Block> newBlocks) {
		if (newBlocks.size() > blockChain.size()) {
			blockChain = newBlocks;
		}
	}
	
	@Override
	public void run() {
		InetAddress address = socket.getInetAddress();
		LOGGER.info("New collected,node IP：" + address.getHostAddress() + " ,port：" + socket.getPort());
		BufferedReader br = null;
		PrintWriter pw = null;
		try {
			// 读取连接结点发送的信息
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter(socket.getOutputStream());
			pw.write("please enter a new number(vac):\n");
			pw.flush();
			String info = null;

			while ((info = br.readLine()) != null) {
				try {
					int vac = Integer.parseInt(info);
					// 根据vac创建区块
					Block lastBlock = blockChain.get(blockChain.size() - 1);
					Block newBlock = Common.generateBlock(lastBlock, vac);
					if (Common.isBlockValid(newBlock, lastBlock)) {
						blockChain.add(newBlock);
						pw.write("Success!\n");
						pw.write(gson.toJson(blockChain));
					} else {
						pw.write("HTTP 500: Invalid Block Error\n");
					}
					LOGGER.info("add new block with vac：" + vac);
				} catch (Exception e) {
					LOGGER.error("not a number:", e);
					pw.write("not a number! \n");
				}
				pw.write("Please enter a new number(vac):" + "\n");
				// 调用flush()方法将缓冲输出
				pw.flush();
			}
			
		} catch (Exception e) {
			LOGGER.error("TCP i/o error Or client closed", e);
		} finally {
			LOGGER.info("node closed:" + address.getHostAddress() + ",port:" + socket.getPort());
			// 关闭资源
			try {
				if (pw != null) {
					pw.close();
				}
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				LOGGER.error("close error:", e);
			}
		}
	}
	
}
