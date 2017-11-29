package org.tfelab.stock_qs.model;

import com.google.gson.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RSet;
import org.tfelab.db.DBName;
import org.tfelab.db.OrmLiteDaoManager;
import org.tfelab.db.PooledDataSource;
import org.tfelab.db.RedissonAdapter;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.io.requester.chrome.action.ChromeDriverAction;
import org.tfelab.io.requester.proxy.ProxyWrapper;
import org.tfelab.json.InterfaceAdapter;
import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;
import org.tfelab.txt.DateFormatUtil;
import org.tfelab.txt.StringUtil;

import java.lang.reflect.Type;
import java.sql.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

@DatabaseTable(tableName = "transactions")
@DBName(value = "china_stock_qs")
public class Transaction implements JSONable {

	private static final Logger logger = LogManager.getLogger(Transaction.class.getName());

	public static final RSet<String> hashset = RedissonAdapter.redisson.getSet("transaction-hashset");


	static {
		JSON.gb.registerTypeAdapter(Transaction.class, new Transaction.Serializer()).setDateFormat(DateFormat.LONG);
		JSON.gson = JSON.gb.create();
		JSON._gson = JSON.gb.setPrettyPrinting().create();
	}
	static class Serializer implements JsonSerializer<Transaction> {

		public JsonElement serialize(Transaction src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray array = new JsonArray();
			array.add(src.code);
			array.add(src.price);
			array.add(src.change_value);
			array.add(src.volume);
			array.add(src.turnover);
			array.add(src.type);
			return array;
		}
	}

	@DatabaseField(generatedId = true)
	private transient Long id;

	private transient String hash;

	@DatabaseField(dataType = DataType.STRING, width = 16, canBeNull = false, indexName = "code-time")
	public String code;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false, defaultValue = "0")
	public float price;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false, defaultValue = "0")
	public float change_value;

	// 成交量 手数 * 100
	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long volume;

	// 成交额 单位元
	@DatabaseField(dataType = DataType.DOUBLE, canBeNull = false, defaultValue = "0")
	public double turnover;

	// 买盘 / 卖盘
	@DatabaseField(dataType = DataType.STRING, width = 16, canBeNull = false)
	public String type;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false, indexName = "code-time")
	public Date time;

	public Transaction() {

	}

	/**
	 *
	 * @param code
	 * @param time
	 * @param price
	 * @param change_value
	 * @param volume
	 * @param turnover
	 * @param type
	 */
	public Transaction(String code, Date time, float price, float change_value, long volume, double turnover, String type) {

		this.code = code;
		this.time = time;
		this.price = price;
		this.change_value = change_value;
		this.volume = volume;
		this.turnover = turnover;
		this.type = type;
		this.hash = StringUtil.MD5(this.toJSON());
	}

	public String getHash() {
		if (hash == null) {
			hash = StringUtil.MD5(this.toJSON());
		}
		return this.hash;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao<Transaction, String> dao = OrmLiteDaoManager.getDao(Transaction.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	/**
	 *
	 * @param items
	 * @throws Exception
	 */
	public static void insertBatch(List<Transaction> items) throws Exception {

		Connection conn = PooledDataSource.getDataSource("china_stock_qs").getConnection();

		String sql = "INSERT IGNORE INTO transactions (`code`, `price`, `change_value`, `volume`, `turnover`, `type`, `time`) values ";

		/*PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO transactions (`id`, `code`, `price`, `change_value`, `volume`, `turnover`, `type`, `time`) " +
				"values (?, ?, ?, ?, ?, ?, ?, ?)");*/

		int count = 0;
		for(Transaction item : items) {

			if (!hashset.contains(item.getHash())) {
				sql += "('"+item.code+"', "+item.price+", "+item.change_value+", "+item.volume+", "+item.turnover+", '"+item.type+"', '"+ DateFormatUtil.dff.print(item.time.getTime())+"'), ";
				count ++;
				hashset.add(item.getHash());
			}

			/*ps.setString(1, item.id);
			ps.setString(2, item.code);
			ps.setFloat(3, item.price);
			ps.setFloat(4, item.change_value);
			ps.setLong(5, item.volume);
			ps.setDouble(6, item.turnover);
			ps.setString(7, item.type);
			ps.setTimestamp(8, new Timestamp(item.time.getTime()));

			ps.addBatch();*/
		}

		sql = sql.substring(0, sql.length() - 2);

		//System.err.println(sql);
		if(count > 0) {
			Statement stmt = conn.createStatement();

			try {
				stmt.execute(sql);
			} catch (Exception e) {
				logger.error(sql, e);
			}

			stmt.close();
		}

		/*ps.executeBatch();

		ps.close();*/
		conn.close();

		/*Dao<Transaction, String> dao = OrmLiteDaoManager.getDao(Transaction.class);

		dao.callBatchTasks(() -> {
			for(Transaction item : items) {
				try {
					dao.create(item);
				} catch (SQLException e) {
				}
			}
			return null;
		});*/
	}

	/**
	 * 是否已经采集到当日数据
	 * @param code
	 * @param dateStr
	 * @return
	 */
	public static boolean existRecords(String code, String dateStr) {

		try {
			Dao<Transaction, String> dao = OrmLiteDaoManager.getDao(Transaction.class);

			if(
					dao.queryRaw("SELECT * FROM transactions WHERE code = ? AND time <= ? AND time > ? LIMIT 1;", code, dateStr + " 09:30:00", dateStr + " 00:30:00")
							.getFirstResult() != null ) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 *
	 * @param code
	 * @param dateStr
	 * @return
	 */
	public static boolean existRecordsFull(String code, String dateStr) {

		try {
			Dao<Transaction, String> dao = OrmLiteDaoManager.getDao(Transaction.class);

			if(
					dao.queryRaw("SELECT * FROM transactions WHERE code = ? AND time <= ? AND time > ? LIMIT 1;", code, dateStr + " 15:00:00", dateStr + " 09:30:00")
							.getFirstResult() != null ) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public String toJSON() {

		return JSON.toJson(this);
	}
}
