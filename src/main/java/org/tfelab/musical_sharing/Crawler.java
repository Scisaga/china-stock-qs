package org.tfelab.musical_sharing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RSet;
import org.redisson.client.RedisTimeoutException;
import org.tfelab.common.config.Configs;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.common.db.PooledDataSource;
import org.tfelab.common.db.RedissonAdapter;
import org.tfelab.common.json.JSON;
import org.tfelab.io.requester.BasicRequester;
import org.tfelab.io.requester.Task;
import org.tfelab.musical_sharing.model.*;
import org.tfelab.proxy.ProxyWrapperImpl;
import org.tfelab.util.FormatUtil;
import org.tfelab.util.NetworkUtil;
import org.tfelab.util.StringUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class Crawler extends Thread {

	private static final Logger logger = LogManager.getLogger(Crawler.class.getName());

	public static final boolean ONLY_USER_INFO = true;
	public static final boolean USE_PROXY = false;

	// 本机IP表示
	public static String local_ip = NetworkUtil.getLocalIp();

	// 已采集用户ID列表
	public static RSet<String> userIds;

	// 待采集用户ID列表
	public static RBlockingQueue<String> userIdQueue;
	public static RSet<String> userIdQueueSet;

	/*public static RBlockingQueue<String> tasks;*/

	public static RBlockingQueue<String> user_tasks;
	public static RBlockingQueue<String> user_follow_tasks;
	public static RBlockingQueue<String> user_video_tasks;

	// 记录全局上次请求时间 跨服务器
	static RAtomicLong last_request_time;

	// 每秒请求数上限
	static int REQUEST_PER_SECOND_LIMIT = 20;

	// 单次请求TIMEOUT
	static int CONNECT_TIMEOUT = 30000;

	static String proxyUser = "";
	static String proxyPasswd = "";

	// 初始化配置参数
	static {

		userIds = RedissonAdapter.redisson.getSet("user-ids");
		userIdQueue = RedissonAdapter.redisson.getBlockingQueue("user-id-queue");
		userIdQueueSet = RedissonAdapter.redisson.getSet("user-id-queue-set");

		/*tasks = RedissonAdapter.redisson.getBlockingQueue("task-queue");*/

		user_tasks = RedissonAdapter.redisson.getBlockingQueue("user-task-queue");
		user_follow_tasks = RedissonAdapter.redisson.getBlockingQueue("user-follow-task-queue");
		user_video_tasks = RedissonAdapter.redisson.getBlockingQueue("user-video-task-queue");

		try {
			REQUEST_PER_SECOND_LIMIT = Configs.dev.getInt("io.requestPerSecondLimit");
		} catch (Exception e) {
			logger.error(e);
		}

		try {
			proxyUser = Configs.dev.getString("abuyun.user");
			proxyPasswd = Configs.dev.getString("abuyun.passwd");
		} catch (Exception e) {
			logger.error(e);
		}

		last_request_time = RedissonAdapter.redisson.getAtomicLong(proxyUser + "-" + proxyPasswd + "-last-request-time");
		last_request_time.set(System.currentTimeMillis());
	}

	// URL模板
	public static String USER_URL = "https://share.musemuse.cn/h5/share/usr/{{user_id}}.html";

	public static String USER_FOLLOW_URL = "https://mus-api-prod.zhiliaoapp.com//rest/discover/user/uservo_followed/list?" +
			"anchor={{anchor}}" +
			"&limit={{limit}}" +
			"&target_user_id={{user_id}}&user_vo_relations=f";

	public static String USER_VIDEOS_URL = "https://mus-api-prod.zhiliaoapp.com//rest/discover/musical/owned_v2/list?" +
			"anchor={{anchor}}" +
			"&limit={{limit}}" +
			"&target_user_id={{user_id}}";

	public static Long FOLLOW_PAGE_LIMIT = 1000000L;

	// 终止线程标志量
	public volatile boolean done = false;

	/**
	 * 初始化Distributor线程
	 */
	public Crawler() {

		/*ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("Distributor-%d").setPriority(7).build();

		Executor executor_ = Executors.newSingleThreadExecutor(namedThreadFactory);
		executor_.execute(new Distributor_("user-task-queue", GetUserInfoTask.class));*/

		Thread d1 = new Distributor_("user-task-queue", GetUserInfoTask.class);
		d1.setPriority(7);
		d1.start();

		Thread d2 = new Distributor_("user-follow-task-queue", GetUserFollowsTask.class);
		d2.setPriority(Thread.NORM_PRIORITY);
		d2.start();
	}

	/**
	 * Header
	 * @param x_request_id
	 * @param x_request_info5
	 * @param x_request_sign5
	 * @param auth
	 * @return
	 */
	public static Map<String, String> genHeaders(String x_request_id, String x_request_info5, String x_request_sign5, String auth, String build) {

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("network", "WiFi");
		headers.put("X-Requested-With", "XMLHttpRequest");
		headers.put("build", build);
		headers.put("country", "CN");
		headers.put("flavor-type", "muse");
		headers.put("mobile", "Genymotion Google Ne");
		headers.put("version", "5.8.0");
		headers.put("language", "en_US");
		headers.put("User-Agent", " Musical.ly/2017062601 (Android; Genymotion Google Nexus 5X - 6.0.0 - API 23 - 1080x1920 6.0;rv:23)");
		headers.put("Connection", "GMT+08:00");
		headers.put("os", "android 6.0");
		headers.put("X-Request-ID", x_request_id);
		headers.put("X-Request-Info5", x_request_info5);
		headers.put("X-Request-Sign5", x_request_sign5);
		headers.put("Authorization", auth);
		headers.put("Host", "mus-api-prod.zhiliaoapp.com");
		headers.put("Accept-Encoding", "gzip");

		return headers;
	}

	/**
	 * 获取阿布云代理
	 * @return
	 */
	public static ProxyWrapperImpl getProxy() {

		if(USE_PROXY) {

			String host = "http-dyn.abuyun.com";
			int port = 9020;
			String user = proxyUser;
			String passwd = proxyPasswd;

		/*String host = "127.0.0.1";
		int port = 8888;

		String user = null;
		String passwd = null;*/

			ProxyWrapperImpl proxy = new ProxyWrapperImpl(host, port, user, passwd);
			return proxy;
		} else {
			return null;
		}
	}

	/**
	 * 更新请求信息
	 * @param ac
	 */
	/*public void updateAccessConfidential(AccessConfidential ac) {
		ac.occupied = false;
		try {
			ac.update();
		} catch (Exception e) {
			logger.info("Can't update AccessConfidential, ", e);
		}
	}*/

	/**
	 * 获取当前线程的休眠修正时间
	 * @return
	 */
	static void waits() {

		long wait_time = last_request_time.get() + (long) Math.ceil(1000D / (double) REQUEST_PER_SECOND_LIMIT) - System.currentTimeMillis();

		if(wait_time > 0) {
			logger.info("Wait {} ms.", wait_time);
			try {
				Thread.sleep(wait_time);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}

		last_request_time.set(System.currentTimeMillis());
	}

	/**
	 * 向队列中添加采集任务
	 * @param t
	 * @throws InterruptedException
	 */
	public static void addTask(Task t) {

		if(t instanceof GetUserInfoTask) {
			user_tasks.offer(t.toJSON());
		} else {
			user_follow_tasks.offer(t.toJSON());
		}
	}

	/**
	 * 处理用户id 生成后续任务
	 * @param uid
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public void procUser(String uid) {

		// 先尝试从数据库获取用户记录
		User user = null;
		try {
			user = User.getUserByUid(uid);
		} catch (Exception e) {
			logger.error(e);
		}

		// 如果数据库中没有相关user记录 重新获取该user记录
		if(user == null) {


			String url = USER_URL.replaceAll("\\{\\{user_id\\}\\}", uid);;
			try {

				AccessConfidential ac = AccessConfidential.getValidAccessConfidential();

				Task t = new GetUserInfoTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid);

				addTask(t);

			} catch (Exception e) {
				logger.error("Generate GetUserInfoTask error. {}, ", url, e);
			}

		}
		// 其他情况
		else if(user.proc_done) {
			return;
		}
		// 如果存在相关用户记录, 且用户关系链没获取完
		// 继续获取用户关系链数据
		else if(user.fans_anchor != null && user.fans_anchor.length() > 0 && !ONLY_USER_INFO){

			String url = USER_FOLLOW_URL.replaceAll("\\{\\{user_id\\}\\}", String.valueOf(user.uid));
			url = url.replaceAll("\\{\\{anchor\\}\\}", user.fans_anchor);
			url = url.replaceAll("\\{\\{limit\\}\\}", "20");

			try {

				AccessConfidential ac = AccessConfidential.getValidAccessConfidential();

				Task t = new GetUserFollowsTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid, 0);

				addTask(t);

			} catch (Exception e) {
				logger.error("Generate GetUserFollowsTask error. {}, ", url, e);
			}
		}
	}

	/**
	 * 从队列中获取采集任务
	 * 并动态多线程执行
	 */
	class Distributor extends Thread {

		public volatile boolean done = false;

		public ThreadPoolExecutor executor =  new ThreadPoolExecutor(3 * REQUEST_PER_SECOND_LIMIT, 3 * REQUEST_PER_SECOND_LIMIT, 0, TimeUnit.MICROSECONDS,
	            new ArrayBlockingQueue<Runnable>(10000));

		public Distributor () {
			executor.setThreadFactory(new ThreadFactoryBuilder()
					.setNameFormat("Operator-Worker-%d").build());
		}

		public void run() {

			logger.info("Distributor started.");

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}

			while(!done) {

				Task t = null;
				try {

					String json;
					Random r = new Random();
					if (r.nextDouble() < 0) {
						json = user_tasks.poll();
					} else {
						json = user_follow_tasks.poll();
					}

					if(json == null) {
						Thread.sleep(500);
						continue;
					}

					if(json.contains("https://share.musemuse.cn/h5/share/usr/")) {
						t = JSON.fromJson(json, GetUserInfoTask.class);
					} else {
						t = JSON.fromJson(json, GetUserFollowsTask.class);
					}

					t.setProxy(getProxy());
					Operator o = new Operator(t);
					waits();

					if(executor.getQueue().size() > 1000) {
						logger.warn("Wait for executor 10s.");
						Thread.sleep(10000);
					}

					executor.submit(o);

					logger.info("Executor task queue active: {}, queue: {} ", executor.getActiveCount(), executor.getQueue().size());

				} catch (InterruptedException e) {
					logger.error(e);
				} catch (RedisTimeoutException e) {
					logger.error(e);
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
	}

	static class Distributor_ extends Thread {

		RBlockingQueue<String> taskQueue;

		public volatile boolean done = false;

		static ThreadPoolExecutor executor =  new ThreadPoolExecutor(3 * REQUEST_PER_SECOND_LIMIT, 3 * REQUEST_PER_SECOND_LIMIT, 0, TimeUnit.MICROSECONDS,
				new ArrayBlockingQueue<Runnable>(10000));

		Class<? extends Task> clazz;

		public Distributor_ (String taskQueueName, Class<? extends Task> clazz) {

			taskQueue = RedissonAdapter.redisson.getBlockingQueue(taskQueueName);

			executor.setThreadFactory(new ThreadFactoryBuilder()
					.setNameFormat(taskQueueName + "Operator-Worker-%d").build());

			this.clazz = clazz;

			this.setName(this.getClass().getSimpleName() + "-" + taskQueueName);
		}

		public void run() {

			logger.info("Distributor {} started.", this.getName());

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}

			while(!done) {

				Task t = null;
				try {

					String json = taskQueue.take();

					t = JSON.fromJson(json, clazz);
					t.setProxy(getProxy());

					Operator o = new Operator(t);
					waits();

					if(executor.getQueue().size() > 6 * REQUEST_PER_SECOND_LIMIT) {
						logger.warn("Wait for executor 1s.");
						Thread.sleep(1000);
					}

					executor.submit(o);

					logger.info("Executor task queue active: {}, queue: {} ", executor.getActiveCount(), executor.getQueue().size());

				} catch (InterruptedException e) {
					logger.error(e);
				} catch (RedisTimeoutException e) {
					logger.error(e);
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
	}

	/**
	 * 采集任务Wrapper
	 * 线程池调用
	 * 同时间 3*每秒请求上线 线程数
	 */
	static class Operator extends Thread {

		Task t;

		public Operator(Task t) {
			this.t = t;
		}

		public void run() {

			t.setResponse();

			BasicRequester.getInstance().fetch(t, CONNECT_TIMEOUT);

			StatManager.getInstance().count();

			if (t.getException() != null) {

				logger.error("Proxy / AccessConfidential / Server issue.", t.getException());

				if(t.getRetryCount() < 2) {
					t.addRetryCount();
					t.setProxy(null);

					addTask(t);
				}

				return;
			}

			t.postProc();

			logger.info("{} duration: {}", t.getUrl(), t.getDuration());

		}
	}

	/**
	 * 获取用户信息采集任务
	 */
	static class GetUserInfoTask extends Task {

		public String uid;

		public GetUserInfoTask(String url, Map<String, String> headers, String postData, String cookies, String ref, String uid) throws MalformedURLException, URISyntaxException {
			super(url, headers, postData, cookies, ref);
			this.uid = uid;
		}

		public void postProc() {

			try {

				String src = getResponse().getText().replaceAll("\r?\n", " ");

				// 从源代码中截取用户信息, 并记录到数据库
				User user = new User();
				user.src = src;

				user.uid = Long.valueOf(uid);

				Pattern user_name_pattern = Pattern.compile("(?<=<title>).+?(?=</title>)");
				Matcher user_name_matcher = user_name_pattern.matcher(src);
				if (user_name_matcher.find()) {
					user.user_name = user_name_matcher.group();
				}

				Pattern at_user_name_pattern = Pattern.compile("@.+?(?=</p>)");
				Matcher at_user_name_matcher = at_user_name_pattern.matcher(src);
				if (at_user_name_matcher.find()) {
					user.at_user_name = at_user_name_matcher.group();
				}
					/*regMap.put("desc", "(?i)(?<=<pre class=\"desc\">).+?(?=</pre>)");*/

				Pattern follow_cnt_pattern = Pattern.compile("<span>(?<T>.{0,10})</span><span[^>]*?>Follow");
				Matcher follow_cnt_matcher = follow_cnt_pattern.matcher(src);
				if (follow_cnt_matcher.find()) {
					user.follow_cnt = FormatUtil.parseInt(follow_cnt_matcher.group("T"));
				}

				Pattern fan_cnt_pattern = Pattern.compile("<span>(?<T>.{0,10})</span><span[^>]*?>Fans");
				Matcher fan_cnt_matcher = fan_cnt_pattern.matcher(src);
				if (fan_cnt_matcher.find()) {
					user.fan_cnt = FormatUtil.parseInt(fan_cnt_matcher.group("T"));
				}

				Pattern to_like_cnt_pattern = Pattern.compile("<span>(?<T>.{0,10})</span><span[^>]*?>Likes");
				Matcher to_like_cnt_matcher = to_like_cnt_pattern.matcher(src);
				if (to_like_cnt_matcher.find()) {
					user.to_like_cnt = FormatUtil.parseInt(to_like_cnt_matcher.group("T"));
				}

				Pattern video_cnt_pattern = Pattern.compile("<span class=\"number\">(?<T>.{0,10})(</span>)?<span[^>]*?>musical.lys");
				Matcher video_cnt_matcher = video_cnt_pattern.matcher(src);
				if (video_cnt_matcher.find()) {
					user.video_cnt = FormatUtil.parseInt(video_cnt_matcher.group("T"));
				}

				Pattern from_like_cnt_pattern = Pattern.compile("<span class=\"number\">(?<T>.{0,10})(</span>)?<span[^>]*?>Hearts");
				Matcher from_like_cnt_matcher = from_like_cnt_pattern.matcher(src);
				if (from_like_cnt_matcher.find()) {
					user.from_like_cnt = FormatUtil.parseInt(from_like_cnt_matcher.group("T"));
				}

				Pattern is_private_pattern = Pattern.compile("私密帐号|Account.+?Private");
				Matcher is_private_matcher = is_private_pattern.matcher(src);
				if (is_private_matcher.find()) {
					user.is_private = true;
				}

				user.id = StringUtil.MD5(user.uid + " " + user.user_name);

				try {

					user.insert();

					if(!userIds.contains(String.valueOf(user.uid)))
						userIds.add(String.valueOf(user.uid));

				} catch (Exception ex) {
					logger.info("Can't insert user.", ex);
				}

				if(!ONLY_USER_INFO) {

					try {
						// Generate User Follows Task here
						String url = USER_FOLLOW_URL.replaceAll("\\{\\{user_id\\}\\}", String.valueOf(user.uid));
						url = url.replaceAll("\\{\\{anchor\\}\\}", "0");
						url = url.replaceAll("\\{\\{limit\\}\\}", "20");

						AccessConfidential ac = AccessConfidential.getValidAccessConfidential();

						Task t = new GetUserFollowsTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid, 0);

						user_follow_tasks.offer(t.toJSON());

					} catch (Exception ex) {
						logger.info("Generate next task error. ", ex);
					}
				}

			} catch (Exception e) {
				logger.error("Can't get src. {}, ", getUrl(), e);
			}
		}
	}

	/**
	 * 获取用户关系链采集任务
	 * 支持自动翻页
	 */
	static class GetUserFollowsTask extends Task {

		public String uid;

		public long pageCount;

		public GetUserFollowsTask(String url, Map<String, String> headers, String postData, String cookies, String ref, String uid, long pageCount) throws MalformedURLException, URISyntaxException {
			super(url, headers, postData, cookies, ref);
			this.uid = uid;
			this.pageCount = pageCount;
		}

		public void postProc() {

			User user = null;
			try {
				user = User.getUserByUid(uid);
				if(user == null) {
					return;
				}
			} catch (Exception e) {
				logger.error(e);
				return;
			}

			String nextAnchor = null;

			try {

				String src = new String(getResponse().getSrc(), "UTF-8");

				if (src == "" || src == null) {

					user.proc_done = true;
					user.update();
					return;
				}

				/**
				 * 解析下一页用户 anchor
				 */
				Pattern anchorPattern = Pattern.compile("(?<=\"next\":\\{\"url\":\"/rest/discover/user/uservo_followed/list\\?anchor=)\\d+");
				Matcher matcher = anchorPattern.matcher(src);
				if(matcher.find()) {
					nextAnchor = matcher.group();
					user.fans_anchor = nextAnchor;
					user.update();
				} else {
					nextAnchor = null;
				}

				/**
				 * 解析JSON
				 */
				ObjectMapper mapper = new ObjectMapper();
				JsonNode json = null;
				json = mapper.readTree(src);

				List<UserFollow> ufs = new LinkedList<>();

				/**
				 * 解析用户关注关系，保存用户关注关系
				 */
				for(JsonNode subNode : json.get("result").get("list")) {

					UserFollow uf = new UserFollow();

					uf.uid = Long.valueOf(subNode.get("userId").asText());

					uf.user_name = subNode.get("displayName").asText();

					uf.to_uid = user.uid;

					uf.to_user_name = user.user_name;

					try {
						uf.create_time = FormatUtil.parseTime(subNode.get("insertTime").asText());
					} catch (ParseException ex) {
						logger.error(ex.getMessage());
					}

					uf.id = StringUtil.MD5(uf.uid + " " + uf.to_uid);

					ufs.add(uf);

					if(!userIds.contains(String.valueOf(uf.uid)) && !userIdQueueSet.contains(String.valueOf(uf.uid))) {
						userIdQueue.put(String.valueOf(uf.uid));
						userIdQueueSet.add(String.valueOf(uf.uid));
					}
				}

				UserFollow.insertBatch(ufs);

				if(nextAnchor == null || pageCount >= FOLLOW_PAGE_LIMIT) {

					logger.info("User:{} UserFollows proc done.", uid);
					user.proc_done = true;
					user.update();
					return;

				} else {

					String url = USER_FOLLOW_URL.replaceAll("\\{\\{user_id\\}\\}", String.valueOf(user.uid));
					url = url.replaceAll("\\{\\{anchor\\}\\}", nextAnchor);
					url = url.replaceAll("\\{\\{limit\\}\\}","20");

					AccessConfidential ac = AccessConfidential.getValidAccessConfidential();

					Task t = new GetUserFollowsTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid, pageCount++);

					user_follow_tasks.offer(t.toJSON());
				}

			} catch (Exception e) {
				logger.error("Can't get src. {}, ", getUrl(), e);
			}

		}
	}

	/**
	 *
	 */
	static class GetUserVideosTask extends Task {

		public String uid;

		public long pageCount;

		public GetUserVideosTask(String url, Map<String, String> headers, String postData, String cookies, String ref, String uid, long pageCount) throws MalformedURLException, URISyntaxException {
			super(url, headers, postData, cookies, ref);
			this.uid = uid;
			this.pageCount = pageCount;
		}

		public void postProc() {

			UserVideoAnchor uva = null;
			try {
				uva = UserVideoAnchor.getUserVideoAnchorByUid(uid);
				if(uva == null) {
					uva = new UserVideoAnchor(Long.valueOf(uid));
					uva.insert();
				}
			} catch (Exception e) {
				logger.error(e);
				return;
			}

			String nextAnchor = null;

			try {

				String src = new String(getResponse().getSrc(), "UTF-8");

				if (src == "" || src == null) {

					uva.proc_done = true;
					uva.update();
					return;
				}

				/**
				 * 解析下一页用户 anchor
				 */
				Pattern anchorPattern = Pattern.compile("(?<=\"next\":\\{\"url\":\"/rest/discover/musical/owned_v2/list\\?anchor=)\\d+");
				Matcher matcher = anchorPattern.matcher(src);
				if(matcher.find()) {
					nextAnchor = matcher.group();
					uva.video_anchor = nextAnchor;
					uva.update();
				} else {
					nextAnchor = null;
				}

				/**
				 * 解析JSON
				 */
				ObjectMapper mapper = new ObjectMapper();
				JsonNode json = null;
				json = mapper.readTree(src);

				List<UserVideo> uvs = new LinkedList<>();

				/**
				 * 解析用户发布视频信息, 并批量保存
				 */
				for(JsonNode subNode : json.get("result").get("list")) {

					UserVideo uv = new UserVideo();

					uv.uid = Long.valueOf(subNode.get("userId").asText());
					if(subNode.get("caption") != null)
						uv.caption = subNode.get("caption").asText();

					// LABEL

					uv.video_id = subNode.get("musicalId").asLong();

					uv.user_name = subNode.get("displayName").asText();

					uv.view_cnt = subNode.get("completeViewNum").asLong();
					uv.comment_cnt = subNode.get("commentNum").asLong();
					uv.like_cnt = subNode.get("likedNum").asLong();



					String flag = "";
					if(subNode.get("partyFeaturedFlag") != null && subNode.get("partyFeaturedFlag").asBoolean()) {
						flag += "partyFeaturedFlag ";
					}

					if(subNode.get("officialFlag") != null && subNode.get("officialFlag").asBoolean()) {
						flag += "officialFlag ";
					}

					if(subNode.get("featured") != null && subNode.get("featured").asBoolean()) {
						flag += "featured ";
					}
					if(flag.length() > 0) {
						uv.label = flag;
					}


					try {
						uv.create_time = FormatUtil.parseTime(subNode.get("clientCreateTime").asText());
					} catch (ParseException ex) {
						logger.error(ex.getMessage());
					}

					uv.id = StringUtil.MD5(uv.uid + " " + uv.video_id);

					uvs.add(uv);
				}

				UserVideo.insertBatch(uvs);

				if(nextAnchor == null || pageCount >= FOLLOW_PAGE_LIMIT) {

					logger.info("User:{} UserFollows proc done.", uid);
					uva.proc_done = true;
					uva.update();
					return;

				} else {

					String url = USER_FOLLOW_URL.replaceAll("\\{\\{user_id\\}\\}", String.valueOf(uva.uid));
					url = url.replaceAll("\\{\\{anchor\\}\\}", nextAnchor);
					url = url.replaceAll("\\{\\{limit\\}\\}","20");

					AccessConfidential ac = AccessConfidential.getValidAccessConfidential();

					Task t = new GetUserFollowsTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid, pageCount++);

					user_video_tasks.offer(t.toJSON());
				}

			} catch (Exception e) {
				logger.error("Can't get src. {}, ", getUrl(), e);
			}

		}
	}


	/**
	 * 运行
	 * 其实用更紧凑的方式实现
	 */
	public void run() {

		while (!this.done) {

			String uid = null;

			try {

				while (user_tasks.size() > 1000 || user_follow_tasks.size() > 1000) {
					Thread.sleep(6000);
					logger.warn("Sleep 6s.");
				}

				uid = userIdQueue.take();
				userIdQueueSet.remove(uid);
				procUser(uid);

			} catch (InterruptedException e) {
				logger.error(e);
			}

		}
	}

	/**
	 * 已经采集用户记录 社交关系补全
	 * @throws Exception
	 */
	public static void genUserFollowTasks() throws Exception {


		Dao<AccessConfidential, String> dao = OrmLiteDaoManager.getDao(AccessConfidential.class);

		List<AccessConfidential> acs = dao.queryForAll();

		Random r = new Random();

		//

		List<String> tasks_json = new LinkedList<>();

		Connection conn = PooledDataSource.getDataSource("musical_sharing").getConnection();

		String sql = "SELECT uid, fans_anchor FROM users WHERE proc_done = 0;";

		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

		stmt.setFetchSize(Integer.MIN_VALUE);

		String uid = null;
		String anchor = "";
		int i = 0;

		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {

			uid = rs.getString(1);
			anchor = rs.getString(2);

			String url = USER_FOLLOW_URL.replaceAll("\\{\\{user_id\\}\\}", uid);
			url = url.replaceAll("\\{\\{anchor\\}\\}", anchor);
			url = url.replaceAll("\\{\\{limit\\}\\}", "20");

			try {

				AccessConfidential ac = acs.get(r.nextInt(acs.size()));

				Task t = new Crawler.GetUserFollowsTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid, 0);

				tasks_json.add(t.toJSON());
				i++;

			} catch (Exception e) {
				logger.error("Generate GetUserFollowsTask error. {}, ", url, e);
			}


			if(i % 100 == 0) {

				System.err.println(i);

				while (user_follow_tasks.size() > 10000) {
					Thread.sleep(5000);
					logger.warn("Sleep 5s.");
				}

				user_follow_tasks.addAll(tasks_json);
				tasks_json.clear();
			}
		}

		conn.close();

	}

	/**
	 * 从已有的用户表数据, 批量生成用户视频信息采集任务
	 * @throws Exception
	 */
	public static void genUserVideoTasks() throws Exception {

		Dao<AccessConfidential, String> dao = OrmLiteDaoManager.getDao(AccessConfidential.class);

		List<AccessConfidential> acs = dao.queryForAll();

		Random r = new Random();

		//

		List<String> tasks_json = new LinkedList<>();

		Connection conn = PooledDataSource.getDataSource("musical_sharing").getConnection();

		// 第一次运行, 要从users表中读取uid 生成任务
		String sql = "SELECT uid FROM users;";
		// 后续运行时, 直接从user_video_anchors 读取揭露

		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

		stmt.setFetchSize(Integer.MIN_VALUE);

		int i = 0;
		List<UserVideoAnchor> uvas = new ArrayList<>();

		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {

			String uid = rs.getString(1);
			String anchor = "0";

			String url = USER_VIDEOS_URL.replaceAll("\\{\\{user_id\\}\\}", uid);
			url = url.replaceAll("\\{\\{anchor\\}\\}", anchor);
			url = url.replaceAll("\\{\\{limit\\}\\}", "20");

			UserVideoAnchor uva = new UserVideoAnchor(Long.valueOf(uid));
			uvas.add(uva);

			try {

				AccessConfidential ac = acs.get(r.nextInt(acs.size()));

				Task t = new Crawler.GetUserVideosTask(url, genHeaders(ac.x_request_id, ac.x_request_info5, ac.x_request_sign5, ac.auth, ac.build), null, null, null, uid, 0);

				tasks_json.add(t.toJSON());
				i++;

			} catch (Exception e) {
				logger.error("Generate GetUserVideosTask error. {}, ", url, e);
			}


			if(i % 100 == 0) {

				System.err.println(i);

				while (user_follow_tasks.size() > 10000) {
					Thread.sleep(5000);
					logger.warn("Sleep 5s.");
				}

				user_video_tasks.addAll(tasks_json);
				tasks_json.clear();
				UserVideoAnchor.insertBatch(uvas);
				uvas.clear();
			}
		}

		conn.close();

	}

	/**
	 * 程序入口
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		//genUserFollowTasks();

		Crawler c = new Crawler();

		Executor executor = Executors.newSingleThreadExecutor(
				new ThreadFactoryBuilder()
						.setNameFormat("Crawler-%d").build());

		executor.execute(c);

//		new Crawler().procUser("120612309810479104");
	}
}
