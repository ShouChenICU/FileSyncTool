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

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * 配置文件
 *
 * @author shouchen
 */
public class FileSyncConfig implements Serializable {
	private static final String CONF_FILE = "FileSyncConfig.json";
	private static final int MIN_PORT = 0;
	private static final int MAX_PORT = 65535;
	private static final int SECRET_KEY_LENGTH = 44;
	private static List<FileSyncConfig> configs;

	/**
	 * 配置编号
	 */
	private String id;
	/**
	 * 服务地址
	 */
	private String serverHost;
	/**
	 * 端口
	 */
	private Integer serverPort;
	/**
	 * 同步目录
	 */
	private String syncDir;
	/**
	 * 忽略文件列表
	 */
	private List<String> ignoreList;
	/**
	 * 安全密钥
	 */
	private String secretKey;

	/**
	 * 初始化配置
	 */
	public static void initConfig() {
		File configFile = new File(CONF_FILE);
		if (configFile.exists()) {
			Logger.info("加载配置中...");
			StringBuilder jsonStrBuilder = new StringBuilder();
			try (Scanner scanner = new Scanner(configFile, "UTF-8")) {
				while (scanner.hasNextLine()) {
					jsonStrBuilder.append(scanner.nextLine());
				}
				configs = JSON.parseArray(jsonStrBuilder.toString(), FileSyncConfig.class);
				Logger.info("配置加载完毕");
			} catch (FileNotFoundException e) {
				Logger.error(e);
			}
		} else {
			configs = new ArrayList<>();
			addConfig("example");
		}
	}

	/**
	 * 保存配置
	 */
	public static void saveConfig() {
		if (configs == null) {
			return;
		}
		String jsonStr = JSON.toJSONString(configs, true);
		try (FileOutputStream fileOutputStream = new FileOutputStream(CONF_FILE);
			 OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
			writer.write(jsonStr);
			writer.flush();
		} catch (IOException e) {
			Logger.error(e);
		}
	}

	/**
	 * 生成配置试例
	 *
	 * @param id 配置编号
	 * @return 配置对象
	 */
	public static FileSyncConfig genExample(String id) {
		FileSyncConfig config = new FileSyncConfig();
		config.setId(id);
		config.setSyncDir("要同步的目录，请填写完整路径");
		config.setServerHost("服务端可省略，客户端请填写服务端访问地址");
		config.setServerPort(41152).setIgnoreList(new ArrayList<>());
		config.getIgnoreList().add("这里填相对路径");
		config.getIgnoreList().add("file.txt (忽略的文件名)");
		config.getIgnoreList().add("path (忽略的路径名)");
		config.setSecretKey(SecretKeyUtils.genAesKey());
		return config;
	}

	/**
	 * 获取全部配置列表
	 *
	 * @return 配置列表
	 */
	public static List<FileSyncConfig> getConfigList() {
		return configs;
	}

	/**
	 * 获取指定编号的配置
	 *
	 * @param id 配置编号
	 * @return 配置对象
	 */
	public static FileSyncConfig getConfig(String id) {
		for (FileSyncConfig config : configs) {
			if (config.getId().equals(id)) {
				return config;
			}
		}
		return null;
	}

	/**
	 * 生成并保存配置
	 *
	 * @param id 配置编号
	 */
	public static void addConfig(String id) {
		configs.add(genExample(id));
		saveConfig();
	}

	/**
	 * 检验配置
	 *
	 * @throws Exception 异常提示
	 */
	public void checkConfig() throws Exception {
		if (this.serverHost == null) {
			throw new NullPointerException("访问地址配置缺失");
		}
		if (this.serverPort == null) {
			throw new NullPointerException("端口配置缺失");
		}
		if (this.syncDir == null) {
			throw new NullPointerException("同步目录配置缺失");
		}
		if (this.ignoreList == null) {
			throw new NullPointerException("忽略文件列表配置缺失");
		}
		if (this.secretKey == null) {
			throw new NullPointerException("密钥配置缺失");
		}
		if (this.serverPort < MIN_PORT || this.serverPort > MAX_PORT) {
			throw new Exception("端口访问错误");
		}
		File syncFile = new File(this.syncDir);
		if (!syncFile.exists()) {
			throw new Exception("同步目录不存在");
		}
		if (syncFile.isFile()) {
			throw new Exception("同步路径不是一个目录");
		}
		if (!syncFile.isAbsolute()) {
			throw new Exception("同步目录不是绝对路径");
		}
		if (this.secretKey.length() != SECRET_KEY_LENGTH) {
			throw new Exception("安全密钥长度错误");
		}
	}


	public FileSyncConfig() {
		this.id = null;
		this.serverHost = null;
		this.serverPort = null;
		this.syncDir = null;
		this.ignoreList = null;
	}

	public String getId() {
		return id;
	}

	public FileSyncConfig setId(String id) {
		this.id = id;
		return this;
	}

	public String getServerHost() {
		return serverHost;
	}

	public FileSyncConfig setServerHost(String serverHost) {
		this.serverHost = serverHost;
		return this;
	}

	public Integer getServerPort() {
		return serverPort;
	}

	public FileSyncConfig setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
		return this;
	}

	public String getSyncDir() {
		return syncDir;
	}

	public FileSyncConfig setSyncDir(String syncDir) {
		this.syncDir = syncDir;
		return this;
	}

	public List<String> getIgnoreList() {
		return ignoreList;
	}

	public FileSyncConfig setIgnoreList(List<String> ignoreList) {
		this.ignoreList = ignoreList;
		return this;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public FileSyncConfig setSecretKey(String secretKey) {
		this.secretKey = secretKey;
		return this;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FileSyncConfig that = (FileSyncConfig) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "FileSyncConfig{" +
				"id='" + id + '\'' +
				", serverHost='" + serverHost + '\'' +
				", serverPort=" + serverPort +
				", syncDir='" + syncDir + '\'' +
				", ignoreList=" + ignoreList +
				", secretKey='" + secretKey + '\'' +
				'}';
	}
}
