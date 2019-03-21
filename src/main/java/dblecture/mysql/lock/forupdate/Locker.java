package dblecture.mysql.lock.forupdate;

public class Locker implements Comparable<Locker> {

	private String id;
	private long updateTime;

	public Locker() {
	}

	public Locker(String id, long updateTime) {
		this.id = id;
		this.updateTime = updateTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public int compareTo(Locker o) {
		return this.id.compareTo(o.id);
	}

}
