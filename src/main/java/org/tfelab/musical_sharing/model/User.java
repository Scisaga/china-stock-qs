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

import java.util.Date;
import java.util.List;

@DatabaseTable(tableName = "users")
@DBName(value = "musical_sharing")
public class User implements JSONable {

	@DatabaseField(dataType = DataType.STRING, width = 36, canBeNull = false, id = true)
	public String id;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, index = true)
	public long uid;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String user_name;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String at_user_name;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long follow_cnt = 0;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long fan_cnt = 0;

	/* like others count */
	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long to_like_cnt = 0;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long video_cnt = 0;

	/* received likes count */
	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long from_like_cnt = 0;

	@DatabaseField(dataType = DataType.STRING, canBeNull = false, defaultValue = "0")
	public String fans_anchor = "0";

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false, defaultValue = "false")
	public boolean proc_done = false;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false, defaultValue = "false")
	public boolean is_private = false;

	@DatabaseField(dataType = DataType.LONG_STRING, columnDefinition="MEDIUMTEXT")
	public String src;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	public User() {}

	/**
	 * Insert user record
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao<User, String> dao = OrmLiteDaoManager.getDao(User.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	public boolean update() throws Exception{

		Dao<User, String> dao = OrmLiteDaoManager.getDao(User.class);

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}

	/**
	 *
	 * @param uid
	 * @return
	 * @throws Exception
	 */
	public static User getUserByUid(String uid) throws Exception{

		Dao<User, String> dao = OrmLiteDaoManager.getDao(User.class);

		List<User> users = dao.queryForEq("uid", uid);

		if(users != null && users.size() > 0) {
			return users.get(0);
		} else {
			return null;
		}
	}
}
