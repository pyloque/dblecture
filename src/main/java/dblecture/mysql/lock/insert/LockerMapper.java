package dblecture.mysql.lock.insert;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface LockerMapper {

	@Update("create table if not exists locker(id varchar(128) primary key not null, expire_time bigint not null) engine=innodb")
	public void createTable();

	@Update("drop table if exists locker")
	public void dropTable();

	@Insert("insert into locker(id, expire_time) values(#{l.id}, #{l.expireTime})")
	public void insertLocker(@Param("l") Locker locker);

	@Results(@Result(property = "expireTime", column = "expire_time"))
	@Select("select id, expire_time from locker where id=#{id}")
	public Locker getLocker(@Param("id") String id);

	@Delete("delete from locker where id=#{l.id} and expire_time=#{l.expireTime}")
	public int deleteLocker(@Param("l") Locker locker);

	@Update("select 1 from locker where id=#{id} for update")
	public void forUpdate(@Param("id") String id);

}
