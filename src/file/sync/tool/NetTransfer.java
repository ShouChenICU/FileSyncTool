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

import javax.crypto.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author shouchen
 * DateTime: 2021-02-18 10:18
 */
public class NetTransfer {
	private InputStream inputStream;
	private OutputStream outputStream;
	private Cipher encodeCipher;
	private Cipher decodeCipher;

	public NetTransfer(InputStream inputStream, OutputStream outputStream, String aesCode) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		SecretKey aesKey = SecretKeyUtils.parseAesKey(aesCode);
		try {
			encodeCipher = Cipher.getInstance(SecretKeyUtils.ALGORITHM);
			encodeCipher.init(Cipher.ENCRYPT_MODE, aesKey);
			decodeCipher = Cipher.getInstance(SecretKeyUtils.ALGORITHM);
			decodeCipher.init(Cipher.DECRYPT_MODE, aesKey);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			Logger.warn(e);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	public void sendIdentity() throws BadPaddingException, IllegalBlockSizeException, IOException {
		byte[] buf;
		try {
			buf = InetAddress.getLocalHost().getHostName().getBytes(StandardCharsets.UTF_8);
		} catch (UnknownHostException e) {
			Logger.warn(e);
			buf = "unknownHost".getBytes(StandardCharsets.UTF_8);
		}
		buf = encodeCipher.doFinal(buf);
		sendInt(buf.length);
		outputStream.write(buf);
		outputStream.flush();
	}

	public String checkIdentity() throws IOException, BadPaddingException, IllegalBlockSizeException {
		int len = getInt();
		byte[] buf = new byte[len];
		for (int i = 0; i < len; i++) {
			buf[i] = (byte) inputStream.read();
		}
		buf = decodeCipher.doFinal(buf);
		return new String(buf, StandardCharsets.UTF_8);
	}

	public void sendObject(Object object) throws IOException, BadPaddingException, IllegalBlockSizeException {
		byte[] buf;
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			 ObjectOutputStream objectOutputStream = new ObjectOutputStream(buffer)) {
			objectOutputStream.writeObject(object);
			objectOutputStream.flush();
			buf = buffer.toByteArray();
		}
		buf = encodeCipher.doFinal(buf);
		sendInt(buf.length);
		outputStream.write(buf);
		outputStream.flush();
	}

	public Object getObject() throws IOException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
		int len = getInt();
		byte[] buf = new byte[len];
		for (int i = 0; i < len; i++) {
			buf[i] = (byte) inputStream.read();
		}
		buf = decodeCipher.doFinal(buf);
		try (ByteArrayInputStream input = new ByteArrayInputStream(buf);
			 ObjectInputStream objectInputStream = new ObjectInputStream(input)) {
			return objectInputStream.readObject();
		}
	}

	public void sendString(String str) throws BadPaddingException, IllegalBlockSizeException, IOException {
		byte[] buf = str.getBytes(StandardCharsets.UTF_8);
		buf = encodeCipher.doFinal(buf);
		sendInt(buf.length);
		outputStream.write(buf);
		outputStream.flush();
	}

	public String getString() throws IOException, BadPaddingException, IllegalBlockSizeException {
		int len = getInt();
		byte[] buf = new byte[len];
		for (int i = 0; i < len; i++) {
			buf[i] = (byte) inputStream.read();
		}
		buf = decodeCipher.doFinal(buf);
		return new String(buf, StandardCharsets.UTF_8);
	}

	public void sendFile(File inputFile) throws IOException, BadPaddingException, IllegalBlockSizeException {
		byte[] buf;
		int lenCount = 0;
		try (FileInputStream fileInputStream = new FileInputStream(inputFile);
			 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
			byte[] bytes = new byte[1024 * 1024];
			int len;
			Logger.out("已处理 " + lenCount);
			while ((len = bufferedInputStream.read(bytes)) != -1) {
				buf = encodeCipher.doFinal(bytes, 0, len);
				sendInt(buf.length);
				outputStream.write(buf);
				outputStream.flush();
				lenCount += len;
				Logger.out("已处理 " + lenCount);
			}
			sendInt(StatusCode.DONE);
			Logger.out("完成");
		}
	}

	public void getFile(File outputFile) throws IOException, BadPaddingException, IllegalBlockSizeException {
		File parentDir = outputFile.getParentFile();
		if (!parentDir.exists()) {
			if (!outputFile.getParentFile().mkdirs()) {
				throw new IOException("目录创建失败 " + outputFile.getParent());
			}
		}
		try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
			int len;
			int lenCount = 0;
			Logger.out("已处理 " + lenCount);
			while ((len = getInt()) != StatusCode.DONE) {
				byte[] buf = new byte[len];
				for (int i = 0; i < len; i++) {
					buf[i] = (byte) inputStream.read();
				}
				buf = decodeCipher.doFinal(buf);
				bufferedOutputStream.write(buf);
				lenCount += len;
				Logger.out("已处理 " + lenCount);
			}
			bufferedOutputStream.flush();
			Logger.out("完成");
		}
	}

	public void sendInt(int n) throws IOException {
		outputStream.write((n >> 24) & 0xff);
		outputStream.write((n >> 16) & 0xff);
		outputStream.write((n >> 8) & 0xff);
		outputStream.write(n & 0xff);
		outputStream.flush();
	}

	public int getInt() throws IOException {
		int n = inputStream.read() << 24;
		n |= inputStream.read() << 16;
		n |= inputStream.read() << 8;
		n |= inputStream.read();
		return n;
	}
}
