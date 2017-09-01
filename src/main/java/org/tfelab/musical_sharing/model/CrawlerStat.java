package org.tfelab.musical_sharing.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.tfelab.common.db.DBName;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.musical_sharing.Crawler;

import java.util.Date;

@DatabaseTable(tableName = "crawler_stats")
@DBName(value = "musical_sharing")
public class CrawlerStat {

	@DatabaseField(dataType = DataType.DATE, canBeNull = false, id = true)
	public Date insert_time = new Date();

	@DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
	public String ip = Crawler.local_ip;

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false, defaultValue = "0")
	public int request_count = 0;

	public CrawlerStat() {}

	public CrawlerStat(int request_count) {
		this.request_count = request_count;
	}

	public boolean insert() throws Exception{

		Dao<CrawlerStat, String> dao = OrmLiteDaoManager.getDao(CrawlerStat.class);

		if (dao.create(this) == 1) {
			return true;
		}

		return false;
	}

}
