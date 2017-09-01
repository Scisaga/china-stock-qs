package org.tfelab.musical_sharing.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.common.db.DBName;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.common.json.JSON;
import org.tfelab.common.json.JSONable;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

@DatabaseTable(tableName = "user_video_anchors")
@DBName(value = "musical_sharing")
public class UserVideoAnchor implements JSONable {

	@DatabaseField(dataType = DataType.LONG, canBeNull = false, id = true)
	public long uid;

	@DatabaseField(dataType = DataType.STRING, canBeNull = false, defaultValue = "0")
	public String video_anchor = "0";

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false, defaultValue = "false")
	public boolean proc_done = false;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	public UserVideoAnchor() {}

	public UserVideoAnchor(long uid) {
		this.uid = uid;
	}

	public boolean insert() throws Exception{

		Dao<UserVideoAnchor, String> dao = OrmLiteDaoManager.getDao(UserVideoAnchor.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	public boolean update() throws Exception{

		Dao<UserVideoAnchor, String> dao = OrmLiteDaoManager.getDao(UserVideoAnchor.class);

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}

	public static void insertBatch(List<UserVideoAnchor> userVideoAnchors) throws Exception {

		Dao<UserVideoAnchor, String> dao = OrmLiteDaoManager.getDao(UserVideoAnchor.class);

		dao.callBatchTasks(new Callable() {
			@Override
			public Object call() {
				for(UserVideoAnchor userVideoAnchor : userVideoAnchors) {
					try {
						dao.createIfNotExists(userVideoAnchor);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		});
	}

	/**
	 *
	 * @param uid
	 * @return
	 * @throws Exception
	 */
	public static UserVideoAnchor getUserVideoAnchorByUid(String uid) throws Exception{

		Dao<UserVideoAnchor, String> dao = OrmLiteDaoManager.getDao(UserVideoAnchor.class);

		List<UserVideoAnchor> uvas = dao.queryForEq("uid", uid);

		if(uvas != null && uvas.size() > 0) {
			return uvas.get(0);
		} else {
			return null;
		}
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}