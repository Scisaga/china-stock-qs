package org.tfelab.musical_sharing.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.common.db.DBName;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.common.db.PooledDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

@DatabaseTable(tableName = "access_confidentials")
@DBName(value = "musical_sharing")
public class AccessConfidential {

	@DatabaseField(dataType = DataType.STRING, width = 36, canBeNull = false, id = true)
	public String x_request_id;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String x_request_info5;

	@DatabaseField(dataType = DataType.STRING, width = 64, canBeNull = false)
	public String x_request_sign5;

	@DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
	public String auth;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String build;

	@DatabaseField(dataType = DataType.LONG, canBeNull = false)
	public long uid;

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false)
	public int use_cnt = 0;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean occupied = false;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean failed = false;

	@DatabaseField(dataType = DataType.DATE)
	public Date failure_time;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date insert_time = new Date();

	public AccessConfidential() {}

	public AccessConfidential(String x_request_id, String x_request_info5, String x_request_sign5, String auth, String build, long uid) {
		this.x_request_id = x_request_id;
		this.x_request_info5 = x_request_info5;
		this.x_request_sign5 = x_request_sign5;
		this.auth = auth;
		this.build = build;
		this.uid = uid;
	}

	/*public static AccessConfidential getValidAccessConfidential() throws Exception {

		Connection conn = PooledDataSource.getDataSource("musical_sharing").getConnection();
		conn.setAutoCommit(false);

		String sql = "SET @xid = (SELECT x_request_id FROM access_confidentials ORDER BY use_cnt ASC LIMIT 1 FOR UPDATE);";
		conn.prepareStatement(sql).execute();
		conn.prepareStatement("UPDATE access_confidentials SET use_cnt = use_cnt + 1 WHERE x_request_id = @xid;").execute();
		ResultSet rs = conn.prepareStatement("SELECT @xid;").executeQuery();

		conn.commit();

		String xid = null;

		if(rs.next()) {
			xid = rs.getString(1);
		}

		conn.close();

		Dao<AccessConfidential, String> dao = OrmLiteDaoManager.getDao(AccessConfidential.class);

		if (dao.queryForId(xid) == null) {
			throw new Exception("AccessConfidential not available.");
		}
		return dao.queryForId(xid);
	}*/

	public static AccessConfidential getValidAccessConfidential() throws Exception {

		Dao<AccessConfidential, String> dao = OrmLiteDaoManager.getDao(AccessConfidential.class);

		QueryBuilder<AccessConfidential, String> queryBuilder = dao.queryBuilder();
		AccessConfidential ac = queryBuilder.limit(1).orderBy("use_cnt", true).queryForFirst();

		if (ac == null) {
			throw new Exception("AccessConfidential not available.");
		} else {
			ac.use_cnt ++;
			dao.update(ac);
			return ac;
		}
	}

	public boolean insert() throws Exception{

		Dao<AccessConfidential, String> dao = OrmLiteDaoManager.getDao(AccessConfidential.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/*public boolean update() throws Exception{

		Dao<AccessConfidential, String> dao = OrmLiteDaoManager.getDao(AccessConfidential.class);

		if (dao.update(this) == 1) {
			return true;
		}

		return false;
	}*/

	public static void main(String[] args) throws Exception {
		System.err.println(getValidAccessConfidential());
	}

}
