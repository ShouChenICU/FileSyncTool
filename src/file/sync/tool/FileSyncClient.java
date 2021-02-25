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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * 文件同步客户端
 *
 * @author shouchen
 */
public class FileSyncClient {
	private static final int DOWNLOAD_MODE = 1;
	private static final int UPLOAD_MODE = 2;
	private static final String DOWNLOAD_MODE_STRING = "1";
	private static final String UPLOAD_MODE_STRING = "2";
	private static final String YES = "yes";
	private static volatile FileSyncClient fileSyncClient;
	private FileSyncConfig config;
	private NetTransfer netTransfer;
	private Socket socket;
	private int mode;
	private Map<String, String> clientFileMap;
	private Map<String, String> serverFileMap;
	private List<Map.Entry<String, String>> fileAddList;
	private List<Map.Entry<String, String>> fileDelList;

	public static void startClient(FileSyncConfig config) {
		if (fileSyncClient == null) {
			synchronized (FileSyncServer.class) {
				if (fileSyncClient == null) {
					fileSyncClient = new FileSyncClient(config);
				} else {
					return;
				}
			}
			fileSyncClient.startClient();
		}
	}

	private FileSyncClient(FileSyncConfig config) {
		this.config = config;
		this.mode = 0;
		this.fileAddList = new ArrayList<>();
		this.fileDelList = new ArrayList<>();
	}

	private void stop() {
		try {
			socket.close();
		} catch (IOException e) {
			Logger.warn(e);
		}
		System.exit(0);
	}

	private void startClient() {
		try {
			Logger.out("尝试连接到 " + config.getServerHost() + ":" + config.getServerPort() + "...");
			this.socket = new Socket();
			this.socket.connect(new InetSocketAddress(config.getServerHost(), config.getServerPort()), 2000);
			this.netTransfer = new NetTransfer(socket.getInputStream(), socket.getOutputStream(), this.config.getSecretKey());
		} catch (IOException e) {
			Logger.error("连接超时，请重试");
			stop();
		}
		Logger.info("连接成功");
		Logger.info("身份验证中...");
		try {
			netTransfer.sendIdentity();
		} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
			Logger.error(e);
			stop();
		}
		try {
			if (netTransfer.getInt() == StatusCode.DONE) {
				Logger.info("身份验证成功");
				String serverHostName = netTransfer.checkIdentity();
				Logger.info("服务器为 " + serverHostName);
			} else {
				Logger.error("身份验证失败！");
				stop();
			}
		} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
			Logger.error(e);
			stop();
		}
		System.out.println("1) 服务器文件同步到本地");
		System.out.println("2) 本地文件同步到服务器");
		System.out.print("请选择(1或2):");
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		if (DOWNLOAD_MODE_STRING.equals(input.trim())) {
			this.mode = DOWNLOAD_MODE;
		} else if (UPLOAD_MODE_STRING.equals(input.trim())) {
			this.mode = UPLOAD_MODE;
		} else {
			Logger.error("输入范围错误！");
			try {
				netTransfer.sendInt(StatusCode.ERROR);
			} catch (IOException e) {
				Logger.error(e);
			}
			stop();
		}
		try {
			netTransfer.sendInt(StatusCode.DONE);
			netTransfer.sendObject(config.getIgnoreList());
			List<String> ignoreList = (List<String>) netTransfer.getObject();
			ignoreList.addAll(config.getIgnoreList());
			this.clientFileMap = FileParse.fileParse(config.getSyncDir(), ignoreList);
		} catch (IOException | BadPaddingException | IllegalBlockSizeException | ClassNotFoundException e) {
			Logger.error(e);
			stop();
		}
		Logger.out("等待服务器...");
		try {
			this.serverFileMap = (HashMap<String, String>) netTransfer.getObject();
			Logger.info("收到响应");
		} catch (IOException | BadPaddingException | IllegalBlockSizeException | ClassNotFoundException e) {
			Logger.error(e);
			stop();
		}
		Logger.info("开始差量分析");
		int addCount = 0;
		int delCount = 0;
		int changeCount = 0;
		int noChangeCount = 0;
		if (this.mode == DOWNLOAD_MODE) {
			for (Map.Entry<String, String> c : clientFileMap.entrySet()) {
				Logger.out("分析 " + c.getKey());
				if (serverFileMap.get(c.getKey()) == null) {
					fileDelList.add(c);
					delCount++;
				}
			}
			for (Map.Entry<String, String> s : serverFileMap.entrySet()) {
				Logger.out("分析 " + s.getKey());
				String v = clientFileMap.get(s.getKey());
				if (v == null) {
					fileAddList.add(s);
					addCount++;
				} else if (v.equals(s.getValue())) {
					noChangeCount++;
				} else {
					fileDelList.add(s);
					fileAddList.add(s);
					changeCount++;
				}
			}
		} else if (this.mode == UPLOAD_MODE) {
			for (Map.Entry<String, String> s : serverFileMap.entrySet()) {
				Logger.out("分析 " + s.getKey());
				if (clientFileMap.get(s.getKey()) == null) {
					fileDelList.add(s);
					delCount++;
				}
			}
			for (Map.Entry<String, String> c : clientFileMap.entrySet()) {
				Logger.out("分析 " + c.getKey());
				String v = serverFileMap.get(c.getKey());
				if (v == null) {
					fileAddList.add(c);
					addCount++;
				} else if (v.equals(c.getValue())) {
					noChangeCount++;
				} else {
					fileDelList.add(c);
					fileAddList.add(c);
					changeCount++;
				}
			}
		} else {
			Logger.error("错误的模式 " + this.mode);
			stop();
		}
		Logger.info("分析完毕");
		System.out.println("忽略 " + FileParse.getIgnoreCount());
		System.out.println("新增 " + addCount);
		System.out.println("删除 " + delCount);
		System.out.println("修改 " + changeCount);
		System.out.println("未变动 " + noChangeCount);
		System.out.print("是否继续操作?(yes/no)");
		input = scanner.nextLine();
		if (YES.equalsIgnoreCase(input.trim())) {
			if (this.mode == DOWNLOAD_MODE) {
				downloadFiles();
			} else if (this.mode == UPLOAD_MODE) {
				uploadFiles();
			} else {
				Logger.error("错误的模式 " + this.mode);
				stop();
			}
		} else {
			try {
				netTransfer.sendInt(StatusCode.CANCEL);
			} catch (IOException e) {
				Logger.error(e);
			}
			Logger.info("取消传输");
			stop();
		}
		try {
			netTransfer.sendInt(StatusCode.DONE);
		} catch (IOException e) {
			Logger.error(e);
			stop();
		}
		Logger.info("完毕");
		stop();
	}

	private void downloadFiles() {
		for (Map.Entry<String, String> fileEntry : fileDelList) {
			File file = new File(this.config.getSyncDir(), fileEntry.getKey());
			if (file.exists()) {
				if (file.isDirectory()) {
					Logger.info("删除目录 " + fileEntry.getKey());
				} else {
					Logger.info("删除文件 " + fileEntry.getKey());
				}
				FileParse.deleteFile(file);
			}
		}
		try {
			for (Map.Entry<String, String> fileEntry : fileAddList) {
				if (fileEntry.getValue().equalsIgnoreCase(FileParse.DIR)) {
					Logger.info("创建目录 " + fileEntry.getKey());
					new File(this.config.getSyncDir(), fileEntry.getKey()).mkdirs();
				} else {
					Logger.info("获取文件 " + fileEntry.getKey());
					netTransfer.sendInt(StatusCode.GET);
					netTransfer.sendString(fileEntry.getKey());
					netTransfer.getFile(new File(this.config.getSyncDir(), fileEntry.getKey()));
				}
			}
		} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
			Logger.error(e);
			stop();
		}
	}

	private void uploadFiles() {
		for (Map.Entry<String, String> fileEntry : fileDelList) {
			File file = new File(this.config.getSyncDir(), fileEntry.getKey());
			if (fileEntry.getValue().equalsIgnoreCase(FileParse.DIR)) {
				Logger.info("删除服务端目录 " + fileEntry.getKey());
			} else {
				Logger.info("删除服务端文件 " + fileEntry.getKey());
			}
			try {
				netTransfer.sendInt(StatusCode.DELETE);
				netTransfer.sendString(fileEntry.getKey());
			} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
				Logger.error(e);
				stop();
			}
		}
		try {
			for (Map.Entry<String, String> fileEntry : fileAddList) {
				if (fileEntry.getValue().equalsIgnoreCase(FileParse.DIR)) {
					Logger.info("创建服务端目录 " + fileEntry.getKey());
					netTransfer.sendInt(StatusCode.DIR);
					netTransfer.sendString(fileEntry.getKey());
				} else {
					Logger.info("上传文件 " + fileEntry.getKey());
					netTransfer.sendInt(StatusCode.PUT);
					netTransfer.sendString(fileEntry.getKey());
					netTransfer.sendFile(new File(this.config.getSyncDir(), fileEntry.getKey()));
				}
			}
		} catch (IOException | BadPaddingException | IllegalBlockSizeException e) {
			Logger.error(e);
			stop();
		}
	}
}
