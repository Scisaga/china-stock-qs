package org.tfelab.io.requester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tfelab.util.FormatUtil;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuseCrawler {

	public static DateFormat format =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static String USER_FOLLOW_URL = "https://mus-api-prod.zhiliaoapp.com//rest/discover/user/uservo_followed/list?" +
			"anchor={{anchor}}" +
			"&limit={{limit}}" +
			"&target_user_id={{user_id}}&user_vo_relations=f";

	public static String USER_VIDEOS_URL = "https://mus-api-prod.zhiliaoapp.com//rest/discover/musical/owned_v2/list?" +
			"anchor={{anchor}}" +
			"&limit={{limit}}" +
			"&target_user_id={{user_id}}";

	public static String USER_URL = "https://share.musemuse.cn/h5/share/usr/{{user_id}}.html";

	public static Map<String, String> genTestHeaders() {
		
		String x_request_id = "125304b3-f3c8-463e-800a-5407d496f095";
		String x_request_info5 = "eyJvcyI6ImFuZHJvaWQgNi4wIiwidmVyc2lvbiI6IjUuOC4wIiwic2xpZGVyLXNob3ctY29va2llIjoiYjJjdFYzSXhRa2xNU0RGS2FWWnpObEJWZWxkc2VFWTFRbXB0WTE5M1pXTm9ZWFE2ZVRaMWFERkJXV2Q0T0d4aFlXUjNTelZZWmt0UFVUMDlPbUkyWldZeE1UWXdaRE5oTlRFMk5qY3lZV1V5WlRJMFlUTXhPVEUzTWpZMU9qSTFPRGd5TXpBeE1EYzRNRGs1TVRRNE9BIiwiWC1SZXF1ZXN0LUlEIjoiMTI1MzA0YjMtZjNjOC00NjNlLTgwMGEtNTQwN2Q0OTZmMDk1IiwibWV0aG9kIjoiR0VUIiwidXJsIjoiaHR0cHM6XC9cL2xpdmUuZGlyZWN0Lmx5XC9cL3Jlc3RcL2xpdmVzXC92MVwvZGlzY292ZXJ5XC9jaGFubmVsc1wvbGF0ZXN0P3ZpZXdlZElkPTkzNDUxNCYiLCJvc3R5cGUiOiJhbmRyb2lkIiwiZGV2aWNlaWQiOiJhMDk1ZmI4YWQxOTliYTRlZDA5NTcyODI3MTk5ZDc1OWVlMTAyMTAyIiwidGltZXN0YW1wIjoxNTAyMzI0MjA1OTY1fQ==";
		String x_request_sign5 = "01a6d7a11818c222eeb7fc9881876dfbd1105825646d";
		String auth = "M-TOKEN \"hash\"=\"MDJlN2E1NjMwMDAxMDA3MDRjYjE4ZGFhMzE0YzUwMDA4NTIzODE4NTA2ZjM2ZWI4NTI3ZDg2Nzg0NzFjMWU2YjI0YzAzMDg4MzI4ODBhZWRkMmEwZDUxOWQ5NmNlZmIyN2IxMmEzZWUyOTkwMmEwMGNkMzBiOGRkOTAyNGY4YTU3NzAyMDY0ZTViOTU4ZmNlNDBiOGY3ZjhlYTdlNDIxZDUwYTVlZTUxNWQ1ZTMwZmFmNzBhMTg1OTdjNDQyYTE3MDk3YzViODU3NTc2YWQ2YzhlOGU3YzZiMjRhYTJhZWZhM2Y1YmM5OTVkYzJlOGQ5MWY5OTFlMTBkZmM3NmZiNTFmZTUzMjMyZTRmMDZhMGVhZTc5MjYzNGUxMTNiNDYxMmM1ODYwY2YyODY4Y2NhNWI3ZTMyNjE0MTUzZjJjOWFlZWIwZmY5YTBkZTc5ODE2YmYxZTA3YzdmMWY4NjI0ZjBmOWE1ODc3ODYyNmU5ZDRkOTU0NzdkYzgxN2Y4NjVjNjg0ZTAxMTk2YTc4NWU3ZA==\"";
		return genHeaders(x_request_id, x_request_info5, x_request_sign5, auth);

	}

	public static Map<String, String> genHeaders(String x_request_id, String x_request_info5, String x_request_sign5, String auth) {

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("network", "WiFi");
		headers.put("X-Requested-With", "XMLHttpRequest");
		headers.put("build", "1498482131274");
		headers.put("country", "CN");
		headers.put("flavor-type", "muse");
		headers.put("mobile", "Genymotion Google Ne");
		headers.put("version", "5.8.0");
		headers.put("language", "en_US");
		headers.put("User-Agent", "Musical.ly/2017062601 (Android; Genymotion Google Nexus 5X - 6.0.0 - API 23 - 1080x1920 6.0;rv:23)");
		headers.put("Connection", "GMT-04:00");
		headers.put("os", "android 6.0");
		headers.put("X-Request-ID", x_request_id);
		headers.put("X-Request-Info5", x_request_info5);
		headers.put("X-Request-Sign5", x_request_sign5);
		headers.put("Authorization", auth);
		headers.put("Host", "mus-api-prod.zhiliaoapp.com");
		headers.put("Accept-Encoding", "gzip");

		return headers;
	}

	public static String getUserFollows(String userId, int limit) throws IOException, URISyntaxException, ParseException {

		String output = "";

		output += "userId\t" +
				"userIdBid\t" +
				"emailVerified\t" +
				"nickName\t" +
				"displayName\t" +
				"icon\t" +
				"isFeatured\t" +
				"isPrivateAccount\t" +
				"userDesc\t" +
				"disabled\t" +
				"handle\t" +
				"insertTime\r\n";

		String anchor = "0";

		for(int i=0; i<limit; i++) {

			String url = USER_FOLLOW_URL.replaceAll("\\{\\{user_id\\}\\}", userId);
			url = url.replaceAll("\\{\\{anchor\\}\\}", anchor);
			url = url.replaceAll("\\{\\{limit\\}\\}","20");

			Task t = new Task(url, genTestHeaders(), null, null, null);

			BasicRequester.getInstance().fetch(t, 30000);

			if(t.getException() != null){
				t.getException().printStackTrace();
			}

			String src = t.getResponse().getText().replaceAll("\n", "\\n");

			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(src);

			for(JsonNode subNode : json.get("result").get("list")) {

				String desc = subNode.get("userDesc")==null ? "" : subNode.get("userDesc").asText();

				output += "\"" + subNode.get("userId").asText() + "\"\t"
						+ subNode.get("userIdBid").asText() + "\t"
						+ subNode.get("emailVerified").asText() + "\t"
						+ subNode.get("nickName").asText() + "\t"
						+ subNode.get("displayName").asText() + "\t"
						+ subNode.get("icon").asText() + "\t"
						+ subNode.get("isFeatured").asText() + "\t"
						+ subNode.get("isPrivateAccount").asText() + "\t"
						+ "" + desc + "\t"
						+ subNode.get("disabled").asText() + "\t"
						+ subNode.get("handle").asText() + "\t"
						+ format.format(FormatUtil.parseTime(subNode.get("insertTime").asText())) + "\r\n";

				anchor = subNode.get("insertTime").asText();
			}

		}

		return output;

	}

	public static String getUserVideos(String userId, int limit) throws IOException, URISyntaxException, ParseException {

		String output = "";

		output += "musicalId\t" +
				"userId\t" +
				"caption\t" +
				"clientCreateTime\t" +
				"commentNum\t" +
				"completeViewNum\t" +
				"likedNum\t" +
				"status\t" +
				"bid\t" +
				"banned\t" +
				"featured\t" +
				"featuredTime\r\n";

		String anchor = "0";

		for(int i=0; i<limit; i++) {

			String url = USER_VIDEOS_URL.replaceAll("\\{\\{user_id\\}\\}", userId);
			url = url.replaceAll("\\{\\{anchor\\}\\}", anchor);
			url = url.replaceAll("\\{\\{limit\\}\\}","20");

			Task t = new Task(url, genTestHeaders(), null, null, null);

			BasicRequester.getInstance().fetch(t, 30000);

			if(t.getException() != null){
				t.getException().printStackTrace();
			}

			String src = t.getResponse().getText().replaceAll("\n", "\\n");

			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(src);

			for(JsonNode subNode : json.get("result").get("list")) {

				System.err.println(subNode);

				output += "\"" + subNode.get("musicalId").asText() + "\"\t"
						+ userId + "\t"
						+ subNode.get("caption").asText() + "\t"
						+ format.format(FormatUtil.parseTime(subNode.get("clientCreateTime").asText())) + "\t"
						+ subNode.get("commentNum").asText() + "\t"
						+ subNode.get("completeViewNum").asText() + "\t"
						+ subNode.get("likedNum").asText() + "\t"
						+ subNode.get("status").asText() + "\t"
						+ subNode.get("bid").asText() + "\t"
						+ subNode.get("banned").asText() + "\t"
						+ subNode.get("featured").asText() + "\t"
						+ format.format(FormatUtil.parseTime(subNode.get("featuredTime").asText())) + "\r\n";

				anchor = subNode.get("clientCreateTime").asText();
			}

		}

		return output;

	}

	public static String getUserInfo(String userId) throws IOException, URISyntaxException {

		String output = "";

		String url = USER_URL.replaceAll("\\{\\{user_id\\}\\}", userId);
		Task t = new Task(url, genTestHeaders(), null, null, null);
		BasicRequester.getInstance().fetch(t, 30000);

		if(t.getException() != null){
			t.getException().printStackTrace();
		}

		String src = t.getResponse().getText().replaceAll("\r?\n", " ");
		//System.err.println(src);

		Map<String, String> regMap = new TreeMap<String, String>();
		regMap.put("1title", "(?<=<title>).+?(?=</title>)");
		regMap.put("2desc", "(?i)(?<=<pre class=\"desc\">).+?(?=</pre>)");
		regMap.put("3follow", "(?<=<p><span>).{0,10}(?=</span><span>Follow)");
		regMap.put("4fans", "(?<=<p><span>).{0,10}(?=</span><span>Fans)");
		regMap.put("5likes", "(?<=<p><span>).{0,10}(?=</span><span>Likes)");
		regMap.put("6videos", "(?<=<span class=\"number\">).{0,10}(?=<span>musical.lys)");
		regMap.put("7hearts", "(?<=<span class=\"number\">).{0,10}(?=<span>Hearts)");

		output += userId + "\t";

		for(String key : regMap.keySet()) {
			Pattern pattern = Pattern.compile(regMap.get(key));
			Matcher matcher = pattern.matcher(src);
			if(matcher.find()) {
				output += matcher.group();
			}
			output += "\t";
		}

		output += "\r\n";

		System.err.println(output);

		return output;
	}

	public static boolean writeBytesToFile(byte[] fileBytes, String fileName) {

		try {

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
			bos.write(fileBytes);
			bos.flush();
			bos.close();
			return true;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void main(String[] args) throws IOException, URISyntaxException, ParseException {

//		String userId = "68616495085350913";
//		int limit = 10;
//
//		writeBytesToFile(getUserFollows(userId, limit).getBytes(), "user_follows.tsv");

//		getUserInfo("182196113477697536");

		String userId = "934514";
		int limit = 1;

		writeBytesToFile(getUserVideos(userId, limit).getBytes(), "user_follows.tsv");

	}


}
