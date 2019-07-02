
#### 该段代码展示了如何设置连接 获取连接的代码应该设置为单列,避免重复创建连接
```java
    public ShellsCommand getShellsCommand(NginxNodeSyncVo nginxNodeSyncVo) {
        /*初始化配置 设置目标IP地址和登录用户*/
        JschsFactory.JschConfig config = new JschsFactory.JschConfig();
        config.setHost(nginxNodeSyncVo.getIpAddress());
        config.setUser(nginxNodeSyncVo.getAccessUser());
        /*根据授权方式选择公私钥模式或密码*/
        String accessAuthType = Optional.of(nginxNodeSyncVo).map(NginxNodeSyncVo::getAccessAuthType).orElse("");
        if (StringUtils.isEmpty(accessAuthType)) {
            throw new BusinessException(ErrorCodesEnum.GET_DATE_ERR.code(), ErrorCodesEnum.GET_DATE_ERR.msg() + " filed of accessAuthType is empty");
        }
        /*根据自己情况选择证书登录或账号密码登录*/
        if (accessAuthType.equals(AccessAuthTypeEnum.RSA.getKey())) {
            config.setRsaPath(nginxNodeSyncVo.getAccessRsaPath());
        } else {
            config.setPassword(nginxNodeSyncVo.getAccessPassword());
        }
        /*设置目标端口号和连接超时时间*/
        config.setPort(Parse.toInt(nginxNodeSyncVo.getAccessPort()));
        config.setTimeout(CONNECT_LINUX_TIME_OUT);
        ShellsCommand shellsCommand = new ShellsCommand();
        shellsCommand.setConfig(config);
        return shellsCommand;
    }
```

#### 创建完连接后 直接通过shellsCommand执行shell 命令即可,例如下:
```java
    shellsCommand.exec("rm -rf *");
```

#### 这就是个例子哦,哈哈哈哈
