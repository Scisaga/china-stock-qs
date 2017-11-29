package org.tfelab.stock_qs.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.db.DBName;
import java.util.Date;

@DatabaseTable(tableName = "market_open_days")
@DBName(value = "china_stock_qs")
public class MarketOpenDay {

	@DatabaseField(generatedId = true)
	private transient Long id;

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String market;

	@DatabaseField(dataType = DataType.DATE, canBeNull = false)
	public Date date;
}
