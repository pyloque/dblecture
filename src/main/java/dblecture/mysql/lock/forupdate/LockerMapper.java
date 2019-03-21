package dblecture.mysql.lock.forupdate;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface LockerMapper {

	@Update("create table if not exists locker(id varchar(128) primary key not null, update_time bigint not null) engine=innodb")
	public void createTable();

	@Update("drop table if exists locker")
	public void dropTable();

	@Insert("insert into locker(id, update_time) values(#{l.id}, #{l.updateTime})")
	public void insertLocker(@Param("l") Locker locker);

	@Results(@Result(property = "updateTime", column = "update_time"))
	@Select("select id, update_time from locker where id=#{id}")
	public Locker getLocker(@Param("id") String id);

	@Delete("delete from locker where id=#{l.id}")
	public int deleteLocker(@Param("l") Locker locker);

	@Update("select 1 from locker where id=#{id} for update")
	public void forUpdate(@Param("id") String id);
	
	@Update("update locker set update_time=#{l.updateTime} where id=#{l.id}")
	public void updateLocker(@Param("l") Locker locker);
}
