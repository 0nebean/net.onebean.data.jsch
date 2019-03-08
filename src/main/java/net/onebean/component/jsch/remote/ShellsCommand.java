package net.onebean.component.jsch.remote;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import net.onebean.common.exception.BusinessException;

/**
 * 操作远程linux
 *
 * @author 0neBean
 * @since 1.0.0
 */
public class ShellsCommand {

    private JschsFactory.JschConfig config;

    public JschsFactory.JschConfig getConfig() {
        return config;
    }

    public void setConfig(JschsFactory.JschConfig config) {
        this.config = config;
    }

    /**
     * 执行shell命令
     *
     * @param cmd
     * @return
     * @author 0neBean
     * @throws BusinessException 
     * @since 1.0.0
     */
    public void exec(String cmd) throws BusinessException {
        JschsFactory.logger.info("executing command: " + cmd);
        Session session = null;
        ChannelExec execChannel = null;
        try {
            session = JschsFactory.initSession(this.config);
            execChannel = JschsFactory.initChannelExec(session);
            JschsFactory.exec(execChannel, cmd);
        } catch (Exception e) {
            String message = "执行命令[" + cmd + "]失败";
            JschsFactory.logger.error(message, e);
            throw new BusinessException("999", message+" e = "+e.getMessage()+e.getCause());
        } finally {
            JschsFactory.closeChannel(execChannel);
            JschsFactory.closeSession(session);
        }
    }

    /**
     * 远程拷贝文件，可以拷贝文件夹
     *
     * @param sPath 源路径
     * @param dPath 目的路径
     * @author 0neBean
     * @since 1.0.0
     */
    public void scp(String sPath, String dPath) {
        Session session = null;
        ChannelSftp sftpChannel = null;
        ChannelExec execChannel = null;
        try {
            session = JschsFactory.initSession(this.config);
            sftpChannel = JschsFactory.initChannelSftp(session);
            execChannel = JschsFactory.initChannelExec(session);
            JschsFactory.put(sftpChannel, execChannel, sPath, dPath);
        } catch (Exception e) {
            String message = "拷贝[" + sPath + "]到[" + dPath + "]失败";
            JschsFactory.logger.error(message, e);
            throw new BusinessException("999", message+" e = "+e.getMessage()+e.getCause());
        } finally {
            JschsFactory.closeChannel(execChannel);
            JschsFactory.closeChannel(sftpChannel);
            JschsFactory.closeSession(session);
        }
    }

}
