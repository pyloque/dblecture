package dblecture.mysql.lock.insert;

public class Locker implements Comparable<Locker> {

	private String id;
	private long expireTime;

	public Locker() {
	}

	public Locker(String id, long expireTime) {
		this.id = id;
		this.expireTime = expireTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public int compareTo(Locker o) {
		return this.id.compareTo(o.id);
	}

}
