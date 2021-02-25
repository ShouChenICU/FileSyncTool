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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 简易日志输出
 *
 * @author shouchen
 */
public class Logger {
	public static void debug(Object log) {
		System.out.print("\r\033[37m[" + date() + "\033[37m] [\033[;35;1mDEBUG\033[;37;22m] -> ");
		if (log instanceof Exception) {
			((Exception) log).printStackTrace(System.out);
		} else {
			System.out.println(log + "\033[0m");
		}
	}

	public static void info(Object log) {
		System.out.print("\r\033[37m[" + date() + "\033[37m] [\033[;34;1mINFO\033[;37;22m] -> ");
		if (log instanceof Exception) {
			((Exception) log).printStackTrace(System.out);
		} else {
			System.out.println(log + "\033[0m");
		}
	}

	public static void warn(Object log) {
		System.out.print("\r\033[37m[" + date() + "\033[37m] [\033[;33;1mWARN\033[;37;22m] -> ");
		if (log instanceof Exception) {
			((Exception) log).printStackTrace(System.out);
		} else {
			System.out.println(log + "\033[0m");
		}
	}

	public static void error(Object log) {
		System.out.print("\r\033[37m[" + date() + "\033[37m] [\033[;31;1mERROR\033[;37;22m] -> ");
		if (log instanceof Exception) {
			((Exception) log).printStackTrace(System.out);
		} else {
			System.out.println(log + "\033[0m");
		}
	}

	public static void out(Object log) {
		System.out.print("\r\033[K\033[33m" + log + "\033[0m");
	}

	private static String date() {
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
		Date d = new Date();
		return "\033[32m" + date.format(d) + " \033[36m" + time.format(d);
	}
}
