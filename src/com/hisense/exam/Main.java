package com.hisense.exam;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		System.out.println("输入examID,按回车继续...");
		String args1 = new BufferedReader(new InputStreamReader(System.in)).readLine();
		int examID = -1;
		try {
			examID = Integer.parseInt(args1);
		} catch (Exception e) {
			// TODO: handle exception
			System.err.println("examID必须为整型数字");
			return;
		}
		System.out.println("输入accessToken,按回车继续...");
		String accessToken = new BufferedReader(new InputStreamReader(System.in)).readLine();

		final String path = System.getProperty("user.dir");
		QuestionBankGetter getter = new QuestionBankGetter();
		QuestionAndAnswer result = getter.getQuestionBankData(examID, accessToken);
		
		// 生成题库
		String targetFilePath = path + File.separator + result.filename;
		getter.generateFile(result.json, targetFilePath);
		System.out.println("任务完成,按回车退出程序...");
		new BufferedReader(new InputStreamReader(System.in)).readLine();
	}

}
