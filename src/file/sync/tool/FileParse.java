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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * 文件解析器
 *
 * @author shouchen
 * DateTime: 2021-02-16 13:21
 */
public class FileParse {
	private static final int PROGRESS_BAR_LEN = 25;
	public static final String DIR = "dir";
	private static HashMap<String, String> fileMap;
	private static List<String> ignoreFileList;
	private static int ignoreCount;
	private static int dirPathLen;

	public static synchronized HashMap<String, String> fileParse(String dirPath, List<String> ignoreList) {
		Logger.info("开始分析目录 " + dirPath);
		File dir = new File(dirPath);
		ignoreFileList = new ArrayList<>();
		for (String fileName : ignoreList) {
			ignoreFileList.add(new File(dirPath, fileName).getAbsolutePath());
		}
		ignoreCount = 0;
		FileParse.dirPathLen = dir.getAbsolutePath().length() + 1;
		fileMap = new HashMap<>(16);
		File[] list = dir.listFiles();
		if (list == null) {
			return fileMap;
		}
		for (File file : list) {
			if (file.isDirectory()) {
				parseDir(file);
			} else {
				parseFile(file);
			}
		}
		Logger.info("分析完毕，已分析 " + fileMap.size() + " 个文件");
		return fileMap;
	}

	private static void parseDir(File dir) {
		String dirName = dir.getAbsolutePath().substring(dirPathLen);
		if (isIgnore(dir)) {
			Logger.info("忽略目录 " + dirName);
			ignoreCount++;
			return;
		}
		Logger.info("开始扫描目录 " + dirName);
		fileMap.put(dirName, DIR);
		File[] list = dir.listFiles();
		if (list == null) {
			return;
		}
		for (File file : list) {
			if (file.isDirectory()) {
				parseDir(file);
			} else {
				parseFile(file);
			}
		}
	}

	private static void parseFile(File file) {
		String fileName = file.getAbsolutePath().substring(dirPathLen);
		if (isIgnore(file)) {
			Logger.info("忽略文件 " + fileName);
			ignoreCount++;
			return;
		}
		Logger.info("开始分析文件 " + fileName);
		long maxLen = file.length();
		long stepLen = maxLen / 1000;
		long parseLen = 0;
		byte[] buf = new byte[1024 * 1024];
		int bufLen;
		int addLen = 0;
		double parseRot;
		String sha256 = "";
		DecimalFormat decimalFormat = new DecimalFormat("0.0%");
		try (FileInputStream fileInputStream = new FileInputStream(file);
			 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			while ((bufLen = bufferedInputStream.read(buf)) != -1) {
				parseLen += bufLen;
				addLen += bufLen;
				parseRot = (double) parseLen / maxLen;
				if (addLen >= stepLen) {
					addLen = 0;
					int barLen = (int) (PROGRESS_BAR_LEN * parseRot);
					StringBuilder outBuilder = new StringBuilder("解析中...[");
					for (int i = 0; i < barLen; i++) {
						outBuilder.append('=');
					}
					for (int i = barLen; i < PROGRESS_BAR_LEN; i++) {
						outBuilder.append('-');
					}
					outBuilder.append("] ");
					outBuilder.append(parseLen);
					outBuilder.append('/');
					outBuilder.append(maxLen);
					outBuilder.append(" (");
					outBuilder.append(decimalFormat.format(parseRot));
					outBuilder.append(')');
					Logger.out(outBuilder);
				}
				messageDigest.update(buf, 0, bufLen);
			}
			sha256 = Base64.getEncoder().encodeToString(messageDigest.digest());
		} catch (NoSuchAlgorithmException | IOException e) {
			Logger.warn(e);
		}
		fileMap.put(fileName, sha256);
		Logger.info("分析结果 " + sha256);
	}

	public static int getIgnoreCount() {
		return ignoreCount;
	}

	public static void deleteFile(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					deleteFile(f);
				}
			}
		}
		file.delete();
	}

	private static boolean isIgnore(File file) {
		for (String f : ignoreFileList) {
			if (f.equals(file.getAbsolutePath())) {
				return true;
			}
		}
		return false;
	}
}
