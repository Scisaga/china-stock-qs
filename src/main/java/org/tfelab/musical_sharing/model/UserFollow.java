package org.tfelab.musical_sharing.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.common.db.DBName;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.common.json.JSON;
import org.tfelab.common.json.JSONable;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

@DatabaseTable(tableName = "user_follows")
@DBName(value = "musical_sharing")
public class UserFollow implements JSONable {

	@DatabaseField(dataType = DataType.STRING, width = 36, canBeNull = false, id = true)
	public String id;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long uid;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String user_name;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long to_uid;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String to_user_name;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date create_time = new Date();

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	public UserFollow() {}

	/**
	 * Insert user record
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao<UserFollow, String> dao = OrmLiteDaoManager.getDao(UserFollow.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 *
	 * @param ufs
	 * @throws Exception
	 */
	public static void insertBatch(List<UserFollow> ufs) throws Exception {

		Dao<UserFollow, String> dao = OrmLiteDaoManager.getDao(UserFollow.class);

		dao.callBatchTasks(new Callable() {
			@Override
			public Object call() {
				for(UserFollow uf : ufs) {
					try {
						dao.createIfNotExists(uf);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		});

		/*TransactionManager.callInTransaction(
				OrmLiteDaoManager.getDao(UserFollow.class).getConnectionSource(),
				new Callable() {
					@Override
					public Object call() throws Exception {
						for(UserFollow uf : ufs) {
							OrmLiteDaoManager.getDao(UserFollow.class).createIfNotExists(uf);
						}
						return null;
					}
				}
		);*/
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
