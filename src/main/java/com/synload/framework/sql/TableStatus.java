package com.synload.framework.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.synload.framework.SynloadFramework;

public class TableStatus extends Model {
	
	public String Name;
	public String Engine;
	public long Version;
	public String Row_format;
	public long Rows;
	public long Avg_row_length;
	public long Data_length;
	public long Max_data_length;
	public long Index_length;
	public long Data_free;
	public String Auto_increment;
	public String Create_time;
	public String Update_time;
	public String Check_time;
	public String Collation;
	public String Checksum;
	public String Create_options;
	public String Comment;
	
	public TableStatus(ResultSet rs){
		super(rs);
	}
	
	@SuppressWarnings("rawtypes")
	public static TableStatus get(Class c){
		try {
			PreparedStatement ps = SynloadFramework.sql.prepareStatement("SHOW TABLE STATUS LIKE ?");
			ps.setString(1, c.getSimpleName().toLowerCase()+"s");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				TableStatus out = new TableStatus(rs);
				rs.close();
				ps.close();
				return out;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getEngine() {
		return Engine;
	}
	public void setEngine(String engine) {
		Engine = engine;
	}
	public long getVersion() {
		return Version;
	}
	public void setVersion(long version) {
		Version = version;
	}
	public String getRow_format() {
		return Row_format;
	}
	public void setRow_format(String row_format) {
		Row_format = row_format;
	}
	public long getRows() {
		return Rows;
	}
	public void setRows(long rows) {
		Rows = rows;
	}
	public long getAvg_row_length() {
		return Avg_row_length;
	}
	public void setAvg_row_length(long avg_row_length) {
		Avg_row_length = avg_row_length;
	}
	public long getData_length() {
		return Data_length;
	}
	public void setData_length(long data_length) {
		Data_length = data_length;
	}
	public long getMax_data_length() {
		return Max_data_length;
	}
	public void setMax_data_length(long max_data_length) {
		Max_data_length = max_data_length;
	}
	public long getIndex_length() {
		return Index_length;
	}
	public void setIndex_length(long index_length) {
		Index_length = index_length;
	}
	public long getData_free() {
		return Data_free;
	}
	public void setData_free(long data_free) {
		Data_free = data_free;
	}
	public String getAuto_increment() {
		return Auto_increment;
	}

	public void setAuto_increment(String auto_increment) {
		Auto_increment = auto_increment;
	}

	public String getCreate_time() {
		return Create_time;
	}
	public void setCreate_time(String create_time) {
		Create_time = create_time;
	}
	public String getUpdate_time() {
		return Update_time;
	}
	public void setUpdate_time(String update_time) {
		Update_time = update_time;
	}
	public String getCheck_time() {
		return Check_time;
	}
	public void setCheck_time(String check_time) {
		Check_time = check_time;
	}
	public String getCollation() {
		return Collation;
	}
	public void setCollation(String collation) {
		Collation = collation;
	}
	public String getChecksum() {
		return Checksum;
	}
	public void setChecksum(String checksum) {
		Checksum = checksum;
	}
	public String getCreate_options() {
		return Create_options;
	}
	public void setCreate_options(String create_options) {
		Create_options = create_options;
	}
	public String getComment() {
		return Comment;
	}
	public void setComment(String comment) {
		Comment = comment;
	}
}
