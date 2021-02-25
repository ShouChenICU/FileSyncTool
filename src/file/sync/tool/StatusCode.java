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
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author shouchen
 * DateTime: 2021-02-20 10:44
 */
public final class StatusCode {
	/**
	 * 完成
	 */
	public static final int DONE = 0;
	/**
	 * 出错
	 */
	public static final int ERROR = 1;
	/**
	 * 删除
	 */
	public static final int DELETE = 2;
	/**
	 * 上传
	 */
	public static final int PUT = 5;
	/**
	 * 下载
	 */
	public static final int GET = 10;
	/**
	 * 创建目录
	 */
	public static final int DIR = 21;
	/**
	 * 取消
	 */
	public static final int CANCEL = 42;
}
