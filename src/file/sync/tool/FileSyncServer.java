/*
 * Copyright © 2021 TongZhen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package file.sync.tool;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

/**
 * 文件同步服务端
 *
 * @author shouchen
 */
public class FileSyncServer {
	private static volatile FileSyncServer fileSyncServer;
	private FileSyncConfig config;
	private NetTransfer netTransfer;
	private Socket socket;

	public static void startServer(FileSyncConfig config) {
		if (fileSyncServer == null) {
			synchronized (FileSyncServer.class) {
				if (fileSyncServer == null) {
					fileSyncServer = new FileSyncServer(config);
				} else {
					return;
				}
			}
			fileSyncServer.startServer();
		}
	}

	private FileSyncServer(FileSyncConfig config) {
		this.config = config;
	}

	private void stop() {
		try {
			socket.close();
		} catch (IOException e) {
			Logger.warn(e);
		}
		System.exit(0);
	}

	private void startServer() {
		try (ServerSocket serverSocket = new ServerSocket(config.getServerPort())) {
			Logger.out("等待客户端连接...");
			this.socket = serverSocket.accept();
			this.netTransfer = new NetTransfer(socket.getInputStream(), socket.getOutputStream(), this.config.getSecretKey());
		} catch (IOException e) {
			Logger.error(e);
			stop();
		}
		Logger.info("连接成功");
		Logger.info("验证客户端身份中...");
		try {
			String serverHostName = netTransfer.checkIdentity();
			Logger.info("身份验证成功，识别为 " + serverHostName);
			netTransfer.sendInt(StatusCode.DONE);
			netTransfer.sendIdentity();
			if (netTransfer.getInt() != StatusCode.DONE) {
				Logger.warn("错误的客户端输入");
				stop();
			}
		} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
			Logger.error("身份验证失败！");
			try {
				netTransfer.sendInt(StatusCode.ERROR);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
			stop();
		}
		try {
			netTransfer.sendObject(config.getIgnoreList());
			List<String> ignoreList = (List<String>) netTransfer.getObject();
			ignoreList.addAll(config.getIgnoreList());
			HashMap<String, String> serverFileMap = FileParse.fileParse(config.getSyncDir(), ignoreList);
			netTransfer.sendObject(serverFileMap);
		} catch (IOException | BadPaddingException | IllegalBlockSizeException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		Logger.out("等待客户端...");
		int status;
		String fileName;
		try {
			while ((status = netTransfer.getInt()) != StatusCode.DONE) {
				switch (status) {
					case StatusCode.DELETE:
						fileName = netTransfer.getString();
						File file = new File(this.config.getSyncDir(), fileName);
						if (file.exists()) {
							if (file.isDirectory()) {
								Logger.info("删除目录 " + fileName);
							} else {
								Logger.info("删除文件 " + fileName);
							}
							FileParse.deleteFile(file);
						}
						break;
					case StatusCode.PUT:
						fileName = netTransfer.getString();
						Logger.info("接收文件 " + fileName);
						netTransfer.getFile(new File(this.config.getSyncDir(), fileName));
						break;
					case StatusCode.GET:
						fileName = netTransfer.getString();
						Logger.info("发送文件 " + fileName);
						netTransfer.sendFile(new File(this.config.getSyncDir(), fileName));
						break;
					case StatusCode.DIR:
						fileName = netTransfer.getString();
						Logger.info("创建目录 " + fileName);
						new File(this.config.getSyncDir(), fileName).mkdirs();
						break;
					case StatusCode.CANCEL:
						Logger.warn("客户端取消传输");
						stop();
						break;
					default:
						Logger.warn("错误的状态码 " + status);
						stop();
				}
			}
		} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
			e.printStackTrace();
		}
		Logger.info("完毕");
		stop();
	}
}
