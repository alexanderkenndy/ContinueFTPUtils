package com.yhd.ftp.breakpoint.downloads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import com.yhd.ftp.breakpoint.core.FTPConnections;

public class FTPDownloadsThread implements Runnable {

	public FTPClient ftpClient = new FTPClient();
	FTPConnections ftpConn = new FTPConnections(ftpClient);
	private String host;
	private String username;
	private String passwd;
	private String port;
	private String sourceFile;
	private String targetFile;

	public FTPDownloadsThread(String host, String username, String passwd,
			String port, String sourceFile, String targetFile) {
		// ���ý�������ʹ�õ����������������̨
		this.host = host;
		this.username = username;
		this.passwd = passwd;
		this.port = port;
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.ftpClient
				.addProtocolCommandListener(new ProtocolCommandListener() {

					@Override
					public void protocolReplyReceived(ProtocolCommandEvent arg0) {
					}

					@Override
					public void protocolCommandSent(ProtocolCommandEvent arg0) {
					}
				});
	}

	/** */
	/**
	 * ��FTP�������������ļ�,֧�ֶϵ��������ϴ��ٷֱȻ㱨
	 * 
	 * @param remote
	 *            Զ���ļ�·��
	 * @param local
	 *            �����ļ�·��
	 * @return �ϴ���״̬
	 * @throws IOException
	 */
	public FTPDownloadsStatus download(String remote, String local)
			throws IOException {
		// ���ñ���ģʽ
		ftpClient.enterLocalPassiveMode();
		// �����Զ����Ʒ�ʽ����
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		FTPDownloadsStatus result;

		// ���Զ���ļ��Ƿ����
		FTPFile[] files = ftpClient.listFiles(new String(
				remote.getBytes("GBK"), "iso-8859-1"));
		if (files.length != 1) {
			System.out.println("Remote file doesn't exists");
			return FTPDownloadsStatus.Remote_File_Not_Exists;
		}

		long lRemoteSize = files[0].getSize();
		File f = new File(local);
		// ���ش����ļ������жϵ�����
		if (f.exists()) {
			long localSize = f.length();
			// �жϱ����ļ���С�Ƿ����Զ���ļ���С
			if (localSize >= lRemoteSize) {
				System.out.println("Local file is bigger than remote file, download stop");
				return FTPDownloadsStatus.Local_isBiggerThan_Remote;
			}

			// ���жϵ�����������¼״̬
			FileOutputStream out = new FileOutputStream(f, true);
			ftpClient.setRestartOffset(localSize);
			InputStream in = ftpClient.retrieveFileStream(new String(remote
					.getBytes("GBK"), "iso-8859-1"));
			byte[] bytes = new byte[1024];
			long step = lRemoteSize / 100;
			long process = localSize / step;
			int c;
			while ((c = in.read(bytes)) != -1) {
				out.write(bytes, 0, c);
				localSize += c;
				long nowProcess = localSize / step;
				if (nowProcess > process) {
					process = nowProcess;
					if (process % 10 == 0)
						System.out.println("Download process :" + process);
				}
			}
			in.close();
			out.close();
			boolean isDo = ftpClient.completePendingCommand();
			if (isDo) {
				result = FTPDownloadsStatus.Download_From_Break_Success;
			} else {
				result = FTPDownloadsStatus.Download_From_Break_Failed;
			}
		} else {
			OutputStream out = new FileOutputStream(f);
			InputStream in = ftpClient.retrieveFileStream(new String(remote
					.getBytes("GBK"), "iso-8859-1"));
			byte[] bytes = new byte[1024];
			long step = lRemoteSize / 100;
			long process = 0;
			long localSize = 0L;
			int c;
			while ((c = in.read(bytes)) != -1) {
				out.write(bytes, 0, c);
				localSize += c;
				long nowProcess = localSize / step;
				if (nowProcess > process) {
					process = nowProcess;
					if (process % 10 == 0){
						System.out.println("Download process :" + process);
					}
				}
			}
			
			in.close();
			out.close();
			boolean upNewStatus = ftpClient.completePendingCommand();
			if (upNewStatus) {
				result = FTPDownloadsStatus.Download_New_Success;
			} else {
				result = FTPDownloadsStatus.Download_New_Failed;
			}
		}
		return result;
	}

	@Override
	public void run() {
		try {
			ftpConn.connect(host, new Integer(port), username, passwd);
			this.download(sourceFile, targetFile);
			ftpConn.disconnect();
		} catch (IOException e) {
			System.out.println("Connect to FTP [" + host + ":"+ port + "] error: " + e.getMessage());
			System.exit(-1);
		}
	}
}
