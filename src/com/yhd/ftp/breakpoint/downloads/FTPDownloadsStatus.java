package com.yhd.ftp.breakpoint.downloads;

public enum FTPDownloadsStatus {
	Remote_File_Not_Exists, //Զ���ļ������� 
	Local_isBiggerThan_Remote, //�����ļ�����Զ���ļ� 
	Download_From_Break_Success, //�ϵ������ļ��ɹ� 
	Download_From_Break_Failed,   //�ϵ������ļ�ʧ�� 
	Download_New_Success,    //ȫ�������ļ��ɹ� 
	Download_New_Failed;    //ȫ�������ļ�ʧ��
}
