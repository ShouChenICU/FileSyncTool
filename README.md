# 一款命令行下的文件同步工具

## 启动参数

- 用作服务端：server <配置编号>

- 用作客户端：client <配置编号>

## 配置文件(FileSyncConfig.json)

请使用参数`server <配置编号>`或`client <配置编号>`来新建并初始化一个配置项

默认配置的内容大致如下

`[
	{
		"id":"example",
		"ignoreList":[
			"这里填相对路径",
			"file.txt (忽略的文件名)",
			"path (忽略的路径名)"
		],
		"secretKey":"sJZnrybnd7p2fedwZHCFQU5kAAnb9U0fNU418/5WxSg=",
		"serverHost":"服务端可省略，客户端请填写服务端访问地址",
		"serverPort":41152,
		"syncDir":"要同步的目录，请填写完整路径"
	}
]`

**忽略文件列表将采用服务端和客户端配置的并集**

**请保持客户端和服务端的密钥`secretKey`和端口`serverPort`相同**