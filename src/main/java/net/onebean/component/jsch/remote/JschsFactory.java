package net.onebean.component.jsch.remote;

import com.google.common.base.Charsets;
import com.jcraft.jsch.*;
import net.onebean.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class JschsFactory {


    public final static Logger logger = LoggerFactory.getLogger(JschsFactory.class);

    /**
     * 出事话session
     *
     * @param config
     * @return
     * @throws JSchException
     * @author 0neBean
     * @since 1.0.0
     */
    public static Session initSession(JschConfig config) throws JSchException {
        JSch jsch = new JSch();

        Session session = jsch.getSession(config.getUser(), config.getHost(), config.getPort());
        if (isNotBlank(config.getPassword())) {
            session.setPassword(config.getPassword());
        }
        if (isNotBlank(config.getRsaPath())) {
            session.setPassword(config.getRsaPath());
        }
        if (isNotBlank(config.getStrictHostKeyChecking())) {
            session.setConfig("StrictHostKeyChecking", config.getStrictHostKeyChecking());
        } else {
            session.setConfig("StrictHostKeyChecking", "no");
        }
        if (config.getTimeout() != 0) {
            session.setTimeout(config.getTimeout());
        }

        session.connect();

        return session;
    }

    /**
     * 获取ChannelSftp对象
     *
     * @param session
     * @return
     * @throws JSchException
     * @author 0neBean
     * @since 1.0.0
     */
    public static ChannelSftp initChannelSftp(Session session) throws JSchException {
        Channel channel = session.openChannel("sftp");
        return (ChannelSftp) channel;
    }

    /**
     * 创建ChannelExec对象
     *
     * @param session
     * @return
     * @throws JSchException
     * @author 0neBean
     * @since 1.0.0
     */
    public static ChannelExec initChannelExec(Session session) throws JSchException {
        Channel channel = session.openChannel("exec");
        return (ChannelExec) channel;
    }

    /**
     * 上传文件，支持文件夹上传
     *
     * @param sftpChannel
     * @param sPath       源路径
     * @param dPath       目标路径
     * @throws SftpException
     * @throws JSchException
     * @throws InterruptedException
     * @throws IOException
     * @author 0neBean
     * @throws BusinessException 
     * @since 1.0.0
     */
    public static void put(ChannelSftp sftpChannel, ChannelExec execChannel, String sPath, String dPath)
            throws SftpException, JSchException, IOException, InterruptedException, BusinessException {
        try {
            sftpChannel.connect();
            sftpChannel.cd(dPath);
        } catch (SftpException e) {
            //sftpChannel.mkdir(dPath);
            // 可以创建多级目录
            exec(execChannel, "mkdir -p " + dPath);
            sftpChannel.cd(dPath);
        }
        File sFile = new File(sPath);
        copyFile(sftpChannel, sFile, sftpChannel.pwd());
    }

    /**
     * 拷贝文件递归方法
     *
     * @param sftpChannel
     * @param sFile
     * @param pwd
     * @throws SftpException
     * @throws FileNotFoundException
     * @author 0neBean
     * @since 1.0.0
     */
    private static void copyFile(ChannelSftp sftpChannel, File sFile, String pwd) throws SftpException, FileNotFoundException {
        if (sFile.isDirectory()) {
            File[] list = sFile.listFiles();
            String dirName = sFile.getName();
            sftpChannel.cd(pwd);
            String nextPwdDirName = pwd + "/" + dirName;
            logger.debug("正在创建目录:" + nextPwdDirName);
            SftpATTRS attrs = null;
            try {
                attrs = sftpChannel.stat(dirName);
            } catch (Exception e) {
                // 目录不存在，不需要处理
            }
            if (null != attrs && attrs.isDir()) {
                logger.debug("目录已经存在:" + nextPwdDirName);
            } else {
                sftpChannel.mkdir(dirName);
                logger.debug("目录创建成功:" + nextPwdDirName);
            }
            pwd = nextPwdDirName;
            sftpChannel.cd(dirName);
            for (File aList : list) {
                copyFile(sftpChannel, aList, pwd);
            }
        } else {
            sftpChannel.cd(pwd);
            logger.debug("正在复制文件:" + sFile.getAbsolutePath());
            try (InputStream instream = new FileInputStream(sFile)) {
                sftpChannel.put(instream, sFile.getName());
                logger.info("复制文件成功");
            } catch (IOException e) {
                logger.error("复制文件失败", e);
            }
        }
    }

    /**
     * 若发生错误则直接抛出非运行时异常
     * 
     * @author 0neBean
     * @since 1.0.0
     * @param execChannel
     * @param cmd
     * @throws JSchException
     * @throws IOException
     * @throws InterruptedException
     * @throws BusinessException 
     */
    public static void exec(ChannelExec execChannel, String cmd)
            throws JSchException, IOException, InterruptedException, BusinessException {
        execChannel.setCommand(cmd);
        execChannel.setInputStream(null);
        execChannel.setErrStream(System.err);
        InputStream err = execChannel.getErrStream();
        execChannel.connect();

        String returnMsg = "";
        boolean isSuccess = false;
        byte[] tmp = new byte[1024];
        try {
            while (true) {
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    returnMsg = new String(tmp, 0, i, Charsets.UTF_8);
                    logger.info(returnMsg);
                }
                if (execChannel.isClosed()) {
                    if (err.available() > 0) {
                        continue;
                    }

                    int exitStatus = execChannel.getExitStatus();
                    if (exitStatus == 0) {
                        isSuccess = true;
                    }
                    break;
                }

                Thread.sleep(100);
            }
        } finally {
            if (err != null) {
                err.close();
            }
        }

        if (!isSuccess) {
            //TODO,这里抛出异常，需要针对场景处理异常，然后外层需要捕获异常进行处理（删除操作如果报文件不存在，是认为正确的）
            throw new BusinessException("999","host[" + execChannel.getSession().getHost() + "],命令[" + cmd + "]执行失败，失败信息为[" + returnMsg + "]");
        }
    }

    /**
     * 关闭session
     *
     * @param session
     * @author 0neBean
     * @since 1.0.0
     */
    public static void closeSession(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * 关闭channel
     *
     * @param channel
     * @author 0neBean
     * @since 1.0.0
     */
    public static void closeChannel(Channel channel) {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
    }

    /**
     * 关闭channel
     *
     * @author 0neBean
     * @since 1.0.0
     */
    public static void closeSftpChannel(ChannelSftp sftpChannel) {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.quit();
            sftpChannel.disconnect();
        }
    }


    /**
     * jsch配置对象
     *
     * @author 0neBean
     * @since 1.0.0
     */
    public static class JschConfig {

        private String host;
        private String rsaPath;
        private String user;
        private String password;
        private int port = 22;
        private String strictHostKeyChecking = "no";
        private int timeout = 0;

        public String getRsaPath() {
            return rsaPath;
        }

        public void setRsaPath(String rsaPath) {
            this.rsaPath = rsaPath;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getStrictHostKeyChecking() {
            return strictHostKeyChecking;
        }

        public void setStrictHostKeyChecking(String strictHostKeyChecking) {
            this.strictHostKeyChecking = strictHostKeyChecking;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

    }

    private static boolean isNotBlank(String str) {
        return !(str == null || str.trim().length() == 0);
    }

}
