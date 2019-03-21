package dblecture.mysql.queue;

import com.alibaba.fastjson.annotation.JSONField;

public class TaskItem {

	private String id;
	private String value;
	@JSONField(name = "run_time")
	private long runTime;

	public TaskItem() {
	}

	public TaskItem(String id, String value, long runTime) {
		this.id = id;
		this.value = value;
		this.runTime = runTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

}
