package org.tfelab.stock_qs.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.db.DBName;
import org.tfelab.db.OrmLiteDaoManager;
import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;

import java.util.Date;
import java.util.List;

@DatabaseTable(tableName = "stocks")
@DBName(value = "china_stock_qs")
public class Stock implements JSONable {

	@DatabaseField(dataType = DataType.STRING, width = 16, canBeNull = false, id = true)
	public String code;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String market;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = false)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String short_name;

	@DatabaseField(dataType = DataType.STRING, width = 128, canBeNull = false)
	public String name_en;

	// 发行价格
	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float issue_price;

	// 机构类型
	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String org_type;

	// 组织形式
	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String org_form;

	// 注册资本
	@DatabaseField(dataType = DataType.DOUBLE, canBeNull = false)
	public double registered_capital;

	// 成立日期
	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date establishment_date;

	// 上市日期
	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date listing_date;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public transient Date insert_time = new Date();

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public transient Date update_time = new Date();

	public Stock () {}

	public Stock (String code) {
		this.code = code;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception{

		Dao<Stock, String> dao = OrmLiteDaoManager.getDao(Stock.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

	public static Stock getStockByCode(String code) throws Exception {
		Dao<Stock, String> dao = OrmLiteDaoManager.getDao(Stock.class);
		return dao.queryBuilder().where().eq("code", code).queryForFirst();
	}

	public static List<Stock> getAllStocks() throws Exception {
		Dao<Stock, String> dao = OrmLiteDaoManager.getDao(Stock.class);
		return dao.queryForAll();
	}


	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
