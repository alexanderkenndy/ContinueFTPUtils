package com.yhd.ftp.breakpoint.uploads;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FTPUploadsThread implements Runnable {

	public FTPClient ftpClient = new FTPClient();

	/**
	 * �ϴ��ļ���FTP��������֧�ֶϵ�����
	 * 
	 * @param local
	 *            �����ļ����ƣ�����·��
	 * @param remote
	 *            Զ���ļ�·����ʹ��/home/directory1/subdirectory/file.ext����
	 *            http://www.guihua.org /subdirectory/file.ext
	 *            ����Linux�ϵ�·��ָ����ʽ��֧�ֶ༶Ŀ¼Ƕ�ף�֧�ֵݹ鴴�������ڵ�Ŀ¼�ṹ
	 * @return �ϴ����
	 * @throws IOException
	 */
	public FTPUploadsStatus upload(String local, String remote)
			throws IOException {
		// ����PassiveMode����
		ftpClient.enterLocalPassiveMode();
		// �����Զ��������ķ�ʽ����
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		ftpClient.setControlEncoding("GBK");
		FTPUploadsStatus result;
		// ��Զ��Ŀ¼�Ĵ���
		String remoteFileName = remote;
		if (remote.contains("/")) {
			remoteFileName = remote.substring(remote.lastIndexOf("/") + 1);
			// ����������Զ��Ŀ¼�ṹ������ʧ��ֱ�ӷ���
			if (createDirecroty(remote, ftpClient) == FTPUploadsStatus.Create_Directory_Fail) {
				return FTPUploadsStatus.Create_Directory_Fail;
			}
		}

		// ���Զ���Ƿ�����ļ�
		FTPFile[] files = ftpClient.listFiles(new String(remoteFileName
				.getBytes("GBK"), "iso-8859-1"));
		if (files.length == 1) {
			long remoteSize = files[0].getSize();
			File f = new File(local);
			long localSize = f.length();
			if (remoteSize == localSize) {
				return FTPUploadsStatus.File_Exits;
			} else if (remoteSize > localSize) {
				return FTPUploadsStatus.Remote_isBiggerThan_Local;
			}

			// �����ƶ��ļ��ڶ�ȡָ��,ʵ�ֶϵ�����
			result = uploadFile(remoteFileName, f, ftpClient, remoteSize);

			// ����ϵ�����û�гɹ�����ɾ�����������ļ��������ϴ�
			if (result == FTPUploadsStatus.Upload_From_Break_Failed) {
				if (!ftpClient.deleteFile(remoteFileName)) {
					return FTPUploadsStatus.Delete_Remote_Faild;
				}
				result = uploadFile(remoteFileName, f, ftpClient, 0);
			}
		} else {
			result = uploadFile(remoteFileName, new File(local), ftpClient, 0);
		}
		return result;
	}

	public FTPUploadsStatus uploadFile(String remoteFile, File localFile,
			FTPClient ftpClient, long remoteSize) throws IOException {
		FTPUploadsStatus status;
		// ��ʾ���ȵ��ϴ�
		long step = localFile.length() / 100;
		long process = 0;
		long localreadbytes = 0L;
		RandomAccessFile raf = new RandomAccessFile(localFile, "r");
		OutputStream out = ftpClient.appendFileStream(new String(remoteFile
				.getBytes("GBK"), "iso-8859-1"));
		// �ϵ�����
		if (remoteSize > 0) {
			ftpClient.setRestartOffset(remoteSize);
			process = remoteSize / step;
			raf.seek(remoteSize);
			localreadbytes = remoteSize;
		}
		byte[] bytes = new byte[1024];
		int c;
		while ((c = raf.read(bytes)) != -1) {
			out.write(bytes, 0, c);
			localreadbytes += c;
			if (localreadbytes / step != process) {
				process = localreadbytes / step;
				System.out.println("�ϴ�����:" + process);
				// TODO �㱨�ϴ�״̬
			}
		}
		out.flush();
		raf.close();
		out.close();
		boolean result = ftpClient.completePendingCommand();
		if (remoteSize > 0) {
			status = result ? FTPUploadsStatus.Upload_From_Break_Success
					: FTPUploadsStatus.Upload_From_Break_Failed;
		} else {
			status = result ? FTPUploadsStatus.Upload_New_File_Success
					: FTPUploadsStatus.Upload_New_File_Failed;
		}
		return status;
	}

	/** */
	/**
	 * �ݹ鴴��Զ�̷�����Ŀ¼
	 * 
	 * @param remote
	 *            Զ�̷������ļ�����·��
	 * @param ftpClient
	 *            FTPClient ����
	 * @return Ŀ¼�����Ƿ�ɹ�
	 * @throws IOException
	 */
	public FTPUploadsStatus createDirecroty(String remote, FTPClient ftpClient)
			throws IOException {
		FTPUploadsStatus status = FTPUploadsStatus.Create_Directory_Success;
		String directory = remote.substring(0, remote.lastIndexOf("/") + 1);
		if (!directory.equalsIgnoreCase("/")
				&& !ftpClient.changeWorkingDirectory(new String(directory
						.getBytes("GBK"), "iso-8859-1"))) {
			// ���Զ��Ŀ¼�����ڣ���ݹ鴴��Զ�̷�����Ŀ¼
			int start = 0;
			int end = 0;
			if (directory.startsWith("/")) {
				start = 1;
			} else {
				start = 0;
			}
			end = directory.indexOf("/", start);
			while (true) {
				String subDirectory = new String(remote.substring(start, end)
						.getBytes("GBK"), "iso-8859-1");
				if (!ftpClient.changeWorkingDirectory(subDirectory)) {
					if (ftpClient.makeDirectory(subDirectory)) {
						ftpClient.changeWorkingDirectory(subDirectory);
					} else {
						System.out.println("����Ŀ¼ʧ��");
						return FTPUploadsStatus.Create_Directory_Fail;
					}
				}

				start = end + 1;
				end = directory.indexOf("/", start);

				// �������Ŀ¼�Ƿ񴴽����
				if (end <= start) {
					break;
				}
			}
		}
		return status;
	}

	@Override
	public void run() {

	}
}
