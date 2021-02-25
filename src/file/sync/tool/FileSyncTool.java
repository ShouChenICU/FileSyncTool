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

/**
 * 一款命令行下好用的文件同步工具
 *
 * @author shouchen
 */
public class FileSyncTool {
	private static final int MIN_ARGS_QUANTITY = 2;
	private static final String SERVER_MODE = "server";
	private static final String CLIENT_MODE = "client";

	public static String helpString() {
		return "[使用帮助]\n" +
				"  -启动参数-\n" +
				"    用作服务端：server <配置编号>\n" +
				"    用作客户端：client <配置编号>\n" +
				"  -服务端配置(FileSyncConfig.json)-\n" +
				"    配置编号：id\n" +
				"    服务地址：serverHost\n" +
				"    访问端口：serverPort\n" +
				"    同步目录：syncDir\n" +
				"    忽略文件列表：ignoreList\n" +
				"    安全密钥：secretKey\n";
	}

	public static void main(String[] args) {
		FileSyncConfig config;
		FileSyncConfig.initConfig();
		if (args.length < MIN_ARGS_QUANTITY) {
			System.out.println(helpString());
			System.exit(-1);
		}
		if (!args[0].equals(SERVER_MODE) && !args[0].equals(CLIENT_MODE)) {
			System.out.println(helpString());
			System.exit(-2);
		}
		config = FileSyncConfig.getConfig(args[1]);
		if (config == null) {
			Logger.warn("找不到配置 " + args[1]);
			Logger.out("生成配置中...");
			FileSyncConfig.addConfig(args[1]);
			Logger.info("成功生成配置 " + args[1]);
			Logger.info("请在配置文件中修改此配置");
			System.exit(-3);

		}
		try {
			config.checkConfig();
		} catch (Exception e) {
			Logger.error(e);
			System.exit(-4);
		}
		if (args[0].equals(SERVER_MODE)) {
			FileSyncServer.startServer(config);
		} else if (args[0].equals(CLIENT_MODE)) {
			FileSyncClient.startClient(config);
		}
	}
}
