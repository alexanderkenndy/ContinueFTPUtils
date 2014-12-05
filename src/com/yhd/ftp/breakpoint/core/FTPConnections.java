package com.yhd.ftp.breakpoint.core;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FTPConnections {
	
	private FTPClient ftpClient;
	
	public FTPConnections(FTPClient ftpClient){
		this.ftpClient = ftpClient;
	}

	/**
	 * ���ӵ�FTP������
	 * 
	 * @param hostname
	 *            ������
	 * @param port
	 *            �˿�
	 * @param username
	 *            �û���
	 * @param password
	 *            ����
	 * @return �Ƿ����ӳɹ�
	 * @throws IOException
	 */
	public boolean connect(String hostname, int port, String username,
			String password) throws IOException {
		ftpClient.connect(hostname, port);
		ftpClient.setControlEncoding("GBK");
		if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
			if (ftpClient.login(username, password)) {
				return true;
			}
		}
		disconnect();
		return false;
	}

	/**
	 * �Ͽ���Զ�̷�����������
	 * 
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		if (ftpClient.isConnected()) {
			ftpClient.disconnect();
		}
	}
}
