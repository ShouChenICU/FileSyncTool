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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密钥工具
 *
 * @author shouchen
 */
public class SecretKeyUtils {
	public static final String ALGORITHM = "AES";

	/**
	 * 生成AES密钥
	 *
	 * @return 密钥的Base64编码字符串
	 */
	public static String genAesKey() {
		try {
			KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
			generator.init(256, new SecureRandom());
			SecretKey key = generator.generateKey();
			return Base64.getEncoder().encodeToString(key.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			Logger.warn(e);
		}
		return null;
	}

	/**
	 * 解析AES密钥
	 *
	 * @param keyStr Base64编码的密钥字符串
	 * @return AES密钥对象
	 */
	public static SecretKey parseAesKey(String keyStr) {
		byte[] keyData = Base64.getDecoder().decode(keyStr);
		return new SecretKeySpec(keyData, ALGORITHM);
	}
}
