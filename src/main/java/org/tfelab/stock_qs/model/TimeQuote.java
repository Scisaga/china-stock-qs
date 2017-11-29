package org.tfelab.stock_qs.model;

import com.google.gson.*;
import java.lang.reflect.Type;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.db.DBName;
import org.tfelab.db.OrmLiteDaoManager;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.io.requester.chrome.action.ChromeDriverAction;
import org.tfelab.io.requester.proxy.ProxyWrapper;
import org.tfelab.json.InterfaceAdapter;
import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@DatabaseTable(tableName = "time_quotes")
@DBName(value = "china_stock_qs")
public class TimeQuote implements JSONable<TimeQuote> {

	private static final Logger logger = LogManager.getLogger(TimeQuote.class.getName());

	static {
		JSON.gb.registerTypeAdapter(TimeQuote.class, new Serializer()).setDateFormat(DateFormat.LONG);
		JSON.gson = JSON.gb.create();
		JSON._gson = JSON.gb.setPrettyPrinting().create();
	}

	static class Serializer implements JsonSerializer<TimeQuote> {

		public JsonElement serialize(TimeQuote src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray array = new JsonArray();
			array.add(src.code);
			array.add(src.price);
			array.add(src.change_value);
			array.add(src.volume);
			return array;
		}
	}

	@DatabaseField(generatedId = true)
	private transient Long id;

	@DatabaseField(dataType = DataType.STRING, width = 16, canBeNull = false, uniqueCombo = true)
	public String code;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false, defaultValue = "0")
	public float price;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false, defaultValue = "0")
	public float change_value;

	// 成交量 手数 * 100
	@DatabaseField(dataType = DataType.LONG, canBeNull = false, defaultValue = "0")
	public long volume;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false, uniqueCombo = true)
	public Date time;

	public TimeQuote () {}

	public TimeQuote(String code, Date time, float price, float change_value, long volume) {
		this.code = code;
		this.price = price;
		this.time = time;
		this.change_value = change_value;
		this.volume = volume;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao<TimeQuote, String> dao = OrmLiteDaoManager.getDao(TimeQuote.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	public static boolean existRecords(String code, String dateStr) {
		try {
			Dao<TimeQuote, String> dao = OrmLiteDaoManager.getDao(TimeQuote.class);

			if(
					dao.queryRaw("SELECT * FROM time_quotes WHERE code = ? AND DATE(time) = ? LIMIT 1;", code, dateStr)
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
	 * @param items
	 * @throws Exception
	 */
	public static void insertBatch(Collection<TimeQuote> items) throws Exception {

		Dao<TimeQuote, String> dao = OrmLiteDaoManager.getDao(TimeQuote.class);

		dao.callBatchTasks(() -> {
			for(TimeQuote item : items) {
				try {
					dao.createIfNotExists(item);
				} catch (SQLException e) {
					//logger.warn(e);
				}
			}
			return null;
		});
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
