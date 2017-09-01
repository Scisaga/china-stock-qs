package org.tfelab.musical_sharing.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.common.db.DBName;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.common.json.JSON;
import org.tfelab.common.json.JSONable;
import org.tfelab.util.StringUtil;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

@DatabaseTable(tableName = "user_videos")
@DBName(value = "musical_sharing")
public class UserVideo implements JSONable {

	@DatabaseField(dataType = DataType.STRING, width = 36, canBeNull = false, id = true)
	public String id;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long uid;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String caption;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String label;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long video_id;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String user_name;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long view_cnt = 0;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long comment_cnt = 0;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long like_cnt = 0;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date create_time = new Date();

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	public UserVideo() {}

	/**
	 * Parse user info from src string
	 * TODO
	 */
	public static void parse_user_videos_string(String src) throws Exception {


		User user = new User();
		user.id = StringUtil.MD5(user.uid + " " + user.user_name);
		user.insert();

	}

	/**
	 * Insert user record
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao<UserVideo, String> dao = OrmLiteDaoManager.getDao(UserVideo.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	public static void insertBatch(List<UserVideo> uvs) throws Exception {

		Dao<UserVideo, String> dao = OrmLiteDaoManager.getDao(UserVideo.class);

		dao.callBatchTasks(new Callable() {
			@Override
			public Object call() {
				for (UserVideo uv : uvs) {
					try {
						dao.createIfNotExists(uv);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		});

	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}