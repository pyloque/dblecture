package dblecture.mysql.queue;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TaskMapper {

	@Update("create table if not exists task_${name}(id varchar(128) primary key not null, value text not null, run_time bigint not null, index(run_time)) engine=innodb")
	public void createTable(@Param("name") String name);

	@Update("drop table if exists task_${name}")
	public void dropTable(@Param("name") String name);

	@Insert("insert into task_${name}(id, value, run_time) values(#{t.id}, #{t.value}, #{t.runTime})")
	public void insertTask(@Param("name") String name, @Param("t") TaskItem task);

	@Results(@Result(property = "runTime", column = "run_time"))
	@Select("select id, value, run_time from task_${name} where run_time < #{now} order by run_time desc limit 1")
	public TaskItem pollTask(@Param("name") String name, @Param("now") long ts);

	@Delete("delete from task_${name} where id=#{t.id}")
	public int deleteTask(@Param("name") String name, @Param("t") TaskItem task);

	@Select("select count(1) from task_${name}")
	public int size(@Param("name") String name);

}
