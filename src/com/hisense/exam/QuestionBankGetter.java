package com.hisense.exam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * His学堂题库抓取工具
 * 
 * @author Administrator
 *
 */
public class QuestionBankGetter {

	public static void main(String[] args) throws IOException {
		String accessToken = "1ePCwMAITRXUQSwZ-vt6JCxk59oT3MCJZDYpgyv6i5K00E0WYPuQlBqYo_9fyPJWol8Fk5ZuA1FxKBtKbv7ZoNeCpabkL2Ro81tHRUu56zZrvthDUgx3wk88--KbsXrtT21d1YCCm6KNgty6N-k_TfbeVN_VqfLBmrUaq_hlmV1vN8MW2H1YiqTCtYAnq2Pxu5vmTdr_nz";

		final String path = System.getProperty("user.dir");
		QuestionBankGetter getter = new QuestionBankGetter();

//		QuestionAndAnswer result = getter.getQuestionBankDataByPaperId(8607, accessToken);
//		
//		if (result != null) {
//			// 生成题库
//			String targetFilePath = path + File.separator + result.filename;
//			getter.generateFile(result.json, targetFilePath);
//		}

		// 6074
		for (int i = 6000; i < 6074; i++) {
			// 得到题库原始数据
			QuestionAndAnswer result = getter.getQuestionBankDataByPaperId(i, accessToken);
			if (result != null) {
				// 生成题库
				String targetFilePath = path + File.separator + result.filename;
				getter.generateFile(result.json, targetFilePath);
			}
		}

	}

	private String mBankName = "";
	private CloseableHttpClient mHttpClient;
	private String mAccessToken = "1exP8MAIIUfx5vfsvMGBUVyFhNBudbbBEcCRw3rvumHcq_clHuG1ttwyOqkd1SOorql-lFMp7YuyWsfwLezownn8H93li89_I26XI_cpjO5Wm2OKHBgy0sYC9_fDEeS_9Uj53WMSG93BT5tv41_Xe2emBBIFw5sSW_wCaRde4X1sxOGxtRySek3y8TNU1d_aIsauKQNkNi";
	private static final int LIMIT = 1000;// 题库题数上限

	public QuestionBankGetter() {
		mHttpClient = HttpClients.createDefault();
	}

	/**
	 * 获取题库JSON数据
	 * 
	 * @param examId 考试Id
	 * @param token  接口token
	 * @return
	 */
	public QuestionAndAnswer getQuestionBankData(int examId, String token) {
		int paperId = getPaperId(examId, token);
		System.out.println("paperId is " + paperId);
		if (paperId < 0) {
			System.err.println("未检索到试卷!");
			return null;
		}

		Bank bank = getBankId(paperId, token);
		if (bank == null) {
			System.err.println("未检索到题库!");
			return null;
		}
		System.out.println("bankId is " + bank.id);

		int bankId = bank.id;
		// 获取题库原始数据
		String orignJson = getQuestionOriginList(bankId, token);

		// 获取处理后的问题列表
		String questionList = getQuestionList(orignJson);

		// 获取答案，生成题库JSON
		String bankJson = getAllAnswerFromServer(questionList, token);

		QuestionAndAnswer ret = new QuestionAndAnswer();
		ret.filename = examId + "-" + bank.name + ".txt";
		ret.json = bankJson;
		return ret;

	}

	/**
	 * 获取题库JSON数据
	 * 
	 * @param examId 考试Id
	 * @param token  接口token
	 * @return
	 */
	public QuestionAndAnswer getQuestionBankDataByPaperId(int paperId, String token) {
		Bank bank = getBankId(paperId, token);
		if (bank == null) {
			System.err.println("未检索到题库!");
			return null;
		}
		System.out.println("bankId is " + bank.id);

		int bankId = bank.id;
		// 获取题库原始数据
		String orignJson = getQuestionOriginList(bankId, token);
		if (orignJson == null) {
			System.err.println("接口异常，未获取到原始数据!");
			return null;
		}

		// 获取处理后的问题列表
		String questionList = getQuestionList(orignJson);

		// 获取答案，生成题库JSON
		String bankJson = getAllAnswerFromServer(questionList, token);

		QuestionAndAnswer ret = new QuestionAndAnswer();
		ret.filename = paperId + "-" + bank.name + ".txt";
		ret.json = bankJson;
		return ret;

	}

	/**
	 * 获取题库JSON数据
	 * 
	 * @param examId 考试Id
	 * @param token  接口token
	 * @return
	 */
	public QuestionAndAnswer getQuestionBankDataByBankId(int bankId, String token) {

		// 获取题库原始数据
		String orignJson = getQuestionOriginList(bankId, token);

		// 获取处理后的问题列表
		String questionList = getQuestionList(orignJson);

		// 获取答案，生成题库JSON
		String bankJson = getAllAnswerFromServer(questionList, token);

		QuestionAndAnswer ret = new QuestionAndAnswer();
		ret.filename = bankId + "-" + ".txt";
		ret.json = bankJson;
		return ret;

	}

	/**
	 * 获取题库id
	 * 
	 * @param paperId 试卷id
	 * @param token
	 * @return
	 */
	public Bank getBankId(int paperId, String token) {
		Bank ret = null;
		long timestamp = System.currentTimeMillis();
		String url = String.format(
				"https://api-hedu-mobi.hismarttv.com/heduopapi/papers/getRandomRules?accessToken=%s&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_rj_nxcp_nxrj&paperId=%d&_=%s",
				token, paperId, String.valueOf(timestamp));
		String responseStr = httpGet(url);
		if (responseStr == null) {
			System.err.println("getBankId error");
		}
		try {
			//System.out.println(responseStr);
			JSONObject object = new JSONObject(responseStr);
			JSONObject data = object.getJSONObject("data");
			JSONArray questionBanks = data.getJSONArray("questionBanks");
			JSONObject temp = questionBanks.getJSONObject(0);
			String bankName = temp.getString("name");
			bankName = bankName.replaceAll("/", "").replaceAll("\\\\", "");
			System.out.println("题库名称:" + bankName);

			JSONArray questionRulesBanks = temp.getJSONArray("questionRulesBanks");
			int bankId = questionRulesBanks.getJSONObject(0).getInt("questionBankId");
			ret = new Bank();
			ret.name = bankName;
			ret.id = bankId;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * 获取试卷id
	 * 
	 * @param examId
	 * @return
	 */
	public int getPaperId(int examId, String token) {
		long timestamp = System.currentTimeMillis();
		String url = String.format(
				"https://api-hedu-mobi.hismarttv.com/heduapi/v1.0/webapi/getExamDetail?accessToken=%s&terminalType=2&roleCodes=user&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_hw_rj_hw&examId=%d&_=%s",
				token, examId, String.valueOf(timestamp));
		String responseStr = httpGet(url);
		// System.out.println("getPaperId:" + responseStr);
		if (responseStr == null) {
			System.err.println("getPaperId error");
		}
		try {
			// System.out.println(responseStr);
			JSONObject object = new JSONObject(responseStr);
			int resultCode = object.getInt("resultCode");
			if (resultCode == 0) {
				JSONObject data = object.getJSONObject("data");
				JSONObject paperInfo = data.getJSONObject("paperInfo");
				int paperId = paperInfo.getInt("paperId");
				return paperId;
			} else if (resultCode == 1) {
				String errorDesc = object.getString("errorDesc");
				System.err.println("getPaperId调用失败,errorDesc:" + errorDesc);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {

		}
		return -1;
	}

	public String getQuestionList(String json) {

		JSONArray singleList = new JSONArray();
		JSONArray multiList = new JSONArray();
		JSONArray judgeList = new JSONArray();

		JSONArray otherList = new JSONArray();
		JSONArray blankList = new JSONArray();
		JSONArray questionList = new JSONArray();
		try {
			// System.out.println(json);
			JSONObject object = new JSONObject(json);
			JSONArray data = object.getJSONArray("data");
			int length = data.length();
			System.out.println("题库题目总数量:" + length);
			//System.out.println(json);
			for (int i = 0; i < data.length(); i++) {
				JSONObject question = data.getJSONObject(i);
				int id = question.getInt("id");
				String name = question.getString("name");
				int type = question.getInt("type");
				JSONObject item = new JSONObject();
				item.put("id", id);
				item.put("name", name);
				item.put("type", type);
				// type 3-单选 5-多选 7-判断 10-填空 14-简答
				switch (type) {
				case 3:
					singleList.put(item);
					break;
				case 5:
					multiList.put(item);
					break;
				case 7:
					judgeList.put(item);
					break;
				case 10:
					blankList.put(item);
					break;
				case 14:
					questionList.put(item);
					break;
				default:
					otherList.put(item);
					break;
				}
			}
			JSONObject ret = new JSONObject();
			ret.put("single", singleList);
			ret.put("multi", multiList);
			ret.put("judge", judgeList);
			ret.put("blank", blankList);
			ret.put("question", questionList);
			ret.put("other", otherList);
			if (otherList.length() > 0) {
				System.out.println("其它类型题目数量:" + length);
			}
			return ret.toString();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getQuestionOriginList(int bankId, String token) {
		long timestamp = System.currentTimeMillis();
		String url = String.format(
				"https://api-hedu-mobi.hismarttv.com/heduopapi/questions/getList?accessToken=%s&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_rj_nxcp_nxrj&questionBankId=%d&start=1&limit=%d&_=%s",
				token, bankId, LIMIT, String.valueOf(timestamp));
		System.out.println("正在获取题库题目列表...");
		String responseStr = httpGet(url);
		if (responseStr == null) {
			System.err.println("getQuestionList error");
		}
		return responseStr;
	}

	public String getAllAnswerFromServer(String json, String token) {
		try {
			JSONArray singleList_answer = new JSONArray();
			JSONArray multiList_answer = new JSONArray();
			JSONArray judgeList_answer = new JSONArray();
			JSONObject object = new JSONObject(json);
			JSONArray singleList = object.getJSONArray("single");
			JSONArray multiList = object.getJSONArray("multi");
			JSONArray judgeList = object.getJSONArray("judge");
			JSONArray blankList = object.getJSONArray("blank");
			JSONArray questionList = object.getJSONArray("question");
			JSONArray otherList = object.getJSONArray("other");

			// System.out.println("单选题答案获取中...");
			String alert1 = "";
			for (int i = 0; i < singleList.length(); i++) {
				JSONObject item = singleList.getJSONObject(i);
				int id = item.getInt("id");
				String answerStr = getAnswer(id, token);
				if (answerStr != null) {
					JSONArray answerArray = new JSONArray(answerStr);
					item.put("answer", answerArray);
				}
				singleList_answer.put(item);
				showProgress("单选题答案获取中...", singleList.length(), i + 1);
				Thread.sleep(100);
			}
			System.out.println("\r单选题答案获取完成");

			// System.out.println("多选题答案获取中...");
			for (int i = 0; i < multiList.length(); i++) {
				JSONObject item = multiList.getJSONObject(i);
				int id = item.getInt("id");
				String answerStr = getAnswer(id, token);
				if (answerStr != null) {
					JSONArray answerArray = new JSONArray(answerStr);
					item.put("answer", answerArray);
				}
				multiList_answer.put(item);
				showProgress("多选题答案获取中...", multiList.length(), i + 1);
				Thread.sleep(100);
			}
			System.out.println("\r多选题答案获取完成");

			// System.out.println("判断题答案获取中...");
			String alert3 = "";
			for (int i = 0; i < judgeList.length(); i++) {
				JSONObject item = judgeList.getJSONObject(i);
				int id = item.getInt("id");
				String answerStr = getAnswer(id, token);
				if (answerStr != null) {
					JSONArray answerArray = new JSONArray(answerStr);
					item.put("answer", answerArray);
				}
				judgeList_answer.put(item);
				showProgress("判断题答案获取中...", judgeList.length(), i + 1);
				Thread.sleep(100);
			}
			System.out.println("\r判断题答案获取完成");

			JSONObject ret = new JSONObject();
			ret.put("single", singleList_answer);
			ret.put("multi", multiList_answer);
			ret.put("judge", judgeList_answer);
			ret.put("blank", blankList);
			ret.put("question", questionList);
			ret.put("other", otherList);

			return ret.toString();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}

	public void generateFile(String json, String targetFilePath) {
		StringBuffer sb = new StringBuffer();
		try {
			JSONObject object = new JSONObject(json);
			JSONArray singleList = object.getJSONArray("single");
			JSONArray multiList = object.getJSONArray("multi");
			JSONArray judgeList = object.getJSONArray("judge");
			JSONArray blankList = object.getJSONArray("blank");
			JSONArray questionList = object.getJSONArray("question");
			JSONArray otherList = object.getJSONArray("other");

			sb.append("单选题:").append("\r\n");
			for (int i = 0; i < singleList.length(); i++) {
				JSONObject item = singleList.getJSONObject(i);
				String name = item.getString("name");
				// 题目
				sb.append(String.format("%d.%s \n", i + 1, name));
				JSONArray answers = item.getJSONArray("answer");
				for (int j = 0; j < answers.length(); j++) {
					JSONObject option = answers.getJSONObject(j);
					String optionName = option.getString("optionValue");
					boolean isRight = option.getBoolean("isRight");
					String flag = isRight ? "*" : "";
					// 选项
					sb.append(String.format("%s%s. %s\n", flag, getOptionLetter(j), optionName));
				}
				sb.append("\r\n");
			}

			sb.append("多选题:").append("\r\n");
			for (int i = 0; i < multiList.length(); i++) {
				JSONObject item = multiList.getJSONObject(i);
				String name = item.getString("name");
				// 题目
				sb.append(String.format("%d.%s \n", i + 1, name));
				JSONArray answers = item.getJSONArray("answer");
				for (int j = 0; j < answers.length(); j++) {
					JSONObject option = answers.getJSONObject(j);
					String optionName = option.getString("optionValue");
					boolean isRight = option.getBoolean("isRight");
					String flag = isRight ? "*" : "";
					// 选项
					sb.append(String.format("%s%s. %s\n", flag, getOptionLetter(j), optionName));
				}
				sb.append("\r\n");
			}

			sb.append("判断题:").append("\r\n");
			for (int i = 0; i < judgeList.length(); i++) {
				JSONObject item = judgeList.getJSONObject(i);
				String name = item.getString("name");
				// 题目
				sb.append(String.format("%d.%s \n", i + 1, name));
				JSONArray answers = item.getJSONArray("answer");
				for (int j = 0; j < answers.length(); j++) {
					JSONObject option = answers.getJSONObject(j);
					String optionName = option.getString("optionValue");
					boolean isRight = option.getBoolean("isRight");
					String flag = isRight ? "*" : "";
					// 选项
					sb.append(String.format("%s%s. %s\n", flag, getOptionLetter(j), optionName));
				}
				sb.append("\r\n");
			}

			if (blankList.length() > 0) {
				sb.append("填空题:").append("\r\n");
				for (int i = 0; i < blankList.length(); i++) {
					JSONObject item = blankList.getJSONObject(i);
					String name = item.getString("name");
					// 题目
					sb.append(String.format("%d.%s \n", i + 1, name));
					// todo-目前没有答案
					sb.append("\r\n");
				}
			}

			if (questionList.length() > 0) {
				sb.append("简答题:").append("\r\n");
				for (int i = 0; i < questionList.length(); i++) {
					JSONObject item = questionList.getJSONObject(i);
					String name = item.getString("name");
					// 题目
					sb.append(String.format("%d.%s \n", i + 1, name));
					// todo-目前没有答案
					sb.append("\r\n");
				}
			}

			if (otherList.length() > 0) {
				sb.append("其它:").append("\r\n");
				for (int i = 0; i < otherList.length(); i++) {
					JSONObject item = otherList.getJSONObject(i);
					String name = item.getString("name");
					// 题目
					sb.append(String.format("%d.%s \n", i + 1, name));
					// todo-目前没有答案
					sb.append("\r\n");
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(new File(targetFilePath));
			IOUtils.write(sb, os);
			System.out.println("题库生成成功!!!,路径为" + targetFilePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(os);
		}

	}

	protected void showProgress(String msg, int total, int finish) {
		StringBuilder str = new StringBuilder();
		str.append(String.format("\r%s[", msg));
		for (int i = 0; i < total; i++) {
			if (i > finish) {
				str.append(" ");
			} else if (i == finish) {
				str.append(">");
			} else {
				str.append("=");
			}
		}
		int percent = (100 * finish) / total;
		str.append("]" + percent + "%");
		System.out.print(str.toString());
	}

	protected String getOptionLetter(int i) {
		int index = Math.abs(i) % 26;
		char a = 'A';
		int value = (int) a + index;
		char letter = (char) value;
		return String.valueOf(letter);
	}

	/**
	 * https://api-hedu-mobi.hismarttv.com/heduopapi/questions/getList?accessToken=1exP8MAIIUfx5vfsvMGBUVyFhNBudbbBEcCRw3rvumHcq_clHuG1ttwyOqkd1SOorql-lFMp7YuyWsfwLezownn8H93li89_I26XI_cpjO5Wm2OKHBgy0sYC9_fDEeS_9Uj53WMSG93BT5tv41_Xe2emBBIFw5sSW_wCaRde4X1sxOGxtRySek3y8TNU1d_aIsauKQNkNi&roleCodes=user%2Cdepartment%2CE8410719F26D4DD8B1BD42EE4DF66905&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_rj_nxcp_nxrj&questionId=308870&_=1642649303268
	 * 
	 * @param id
	 * @return
	 */
	public String getAnswer(int id, String token) {
		long timestamp = System.currentTimeMillis();
		String url = String.format(
				"https://api-hedu-mobi.hismarttv.com/heduopapi/questions/getList?accessToken=%s&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_rj_nxcp_nxrj&questionId=%d&_=%s",
				token, id, String.valueOf(timestamp));
		String responseStr = httpGet(url);
		if (responseStr == null) {
			System.err.println("getAnswer response is null");
		}
		try {
			JSONObject object = new JSONObject(responseStr);
			JSONObject data = object.getJSONObject("data");
			JSONArray questionAnswers = data.getJSONArray("questionAnswers");
			JSONArray questionOptions = data.getJSONArray("questionOptions");

			JSONObject answersObj = questionAnswers.getJSONObject(0);
			String answerStr = answersObj.getString("answer");

			JSONArray array = new JSONArray();
			for (int i = 0; i < questionOptions.length(); i++) {
				JSONObject item = questionOptions.getJSONObject(i);
				int optionid = item.getInt("id");
				String optionValue = item.getString("optionValue");
				boolean isRight = false;
				if (answerStr.contains(String.valueOf(optionid))) {
					isRight = true;
				}
				JSONObject anwser = new JSONObject();
				anwser.put("id", optionid);
				anwser.put("optionValue", optionValue);
				anwser.put("isRight", isRight);

				array.put(anwser);
			}

			return array.toString();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}

	protected String httpGet(String url) {
		CloseableHttpResponse response = null;
		try {
			HttpGet request = new HttpGet(url);

			response = mHttpClient.execute(request);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				// return it as a String
				/**
				 * {"data":{"dirResourceName":"
				 * Java语言+视像","question":"软件的生存期分为：","type":5,"testingPoints":[{"name":"软件工程","id":16027}],"questionBankId":5244,"difficulty":2,"isRef":0,"createdBy":"1752592162","questionAnswers":[{"questionId":308870,"answer":"1012316,1012317,1012318"}],"optionsType":1,"questionBankName":"
				 * Java语言+视像3级-23软件开发子类题库","name":"软件的生存期分为：","questionOptions":[{"questionId":308870,"optionValue":"软件定义","id":1012316},{"questionId":308870,"optionValue":"软件开发","id":1012317},{"questionId":308870,"optionValue":"软件维护","id":1012318},{"questionId":308870,"optionValue":"软件分析","id":1012319}],"id":308870,"optionsContent":1,"status":2,"questionAnalysis":"软件工程"},"resultCode":0,"totalCount":1,"signatureServer":
				 * "WJh5pXIMUOVSRQfkJIH+eFqxtiMYGYLdbKLiiOyEIyZvPVhEvkHBIycuHqXzGXvzvWTo+Hz5lhwn1+SvRPtaGQ=="}
				 */
				String result = EntityUtils.toString(entity);
				// System.out.println(result);
				return result;
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return null;

	}

	/**
	 * 获取试卷id
	 * 
	 * @param examId
	 * @return
	 */
	public int getPaperId(int examId) {
		long timestamp = System.currentTimeMillis();
		String url = String.format(
				"https://api-hedu-mobi.hismarttv.com/heduapi/v1.0/webapi/getExamDetail?accessToken=%s&terminalType=2&roleCodes=user&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_hw_rj_hw&examId=%d&_=%s",
				mAccessToken, examId, String.valueOf(timestamp));
		String responseStr = httpGet(url);
		if (responseStr == null) {
			System.err.println("getPaperId error");
		}
		try {
			// System.out.println(responseStr);
			JSONObject object = new JSONObject(responseStr);
			int resultCode = object.getInt("resultCode");
			if (resultCode == 0) {
				JSONObject data = object.getJSONObject("data");
				JSONObject paperInfo = data.getJSONObject("paperInfo");
				int paperId = paperInfo.getInt("paperId");
				return paperId;
			} else if (resultCode == 1) {
				String errorDesc = object.getString("errorDesc");
				System.err.println("接口调用失败,errorDesc:" + errorDesc);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {

		}
		return -1;
	}

	public int getBankId(int paperId) {
		long timestamp = System.currentTimeMillis();
		String url = String.format(
				"https://api-hedu-mobi.hismarttv.com/heduopapi/papers/getRandomRules?accessToken=%s&autonomousOrgCode=dmt_yf&userDeptNumber=dmt_yf_rj_nxcp_nxrj&paperId=%d&_=%s",
				mAccessToken, paperId, String.valueOf(timestamp));
		String responseStr = httpGet(url);
		if (responseStr == null) {
			System.err.println("getBankId error");
		}
		try {
			JSONObject object = new JSONObject(responseStr);
			JSONObject data = object.getJSONObject("data");
			JSONArray questionBanks = data.getJSONArray("questionBanks");
			JSONObject temp = questionBanks.getJSONObject(0);
			String bankName = temp.getString("name");
			System.out.println("题库名称:" + bankName);
			mBankName = bankName;
			JSONArray questionRulesBanks = temp.getJSONArray("questionRulesBanks");
			int bankId = questionRulesBanks.getJSONObject(0).getInt("questionBankId");
			return bankId;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return -1;
	}

}
