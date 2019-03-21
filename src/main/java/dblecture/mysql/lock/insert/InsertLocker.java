package dblecture.mysql.lock.insert;

import java.sql.SQLIntegrityConstraintViolationException;

import org.apache.ibatis.exceptions.PersistenceException;

import dblecture.mysql.Holder;
import dblecture.mysql.MySQLStore;

public class InsertLocker {

	private MySQLStore mysql;

	public InsertLocker(MySQLStore mysql) {
		this.mysql = mysql;
	}

	public void createTable() {
		this.mysql.executeWithMapper(LockerMapper.class, mapper -> {
			mapper.createTable();
		});
	}

	public void dropTable() {
		this.mysql.executeWithMapper(LockerMapper.class, mapper -> {
			mapper.dropTable();
		});
	}

	public Locker lock(String id, int seconds) {
		Holder<Locker> holder = new Holder<>();
		this.mysql.executeWithMapper(LockerMapper.class, mapper -> {
			// 重试次数
			int maxTimes = 2;
			while (maxTimes-- > 0) {
				long expireTime = System.currentTimeMillis() + seconds * 1000;
				Locker locker = new Locker(id, expireTime);
				try {
					mapper.insertLocker(locker);
					holder.value(locker);
					return;
				} catch (PersistenceException e) {
					// 其它非主键冲突异常，直接往上传递
					if (!(e.getCause() instanceof SQLIntegrityConstraintViolationException)) {
						throw e;
					}
				}
				// 加锁失败，瞄一下当前的锁是否有效
				locker = mapper.getLocker(id);
				if (locker == null) {
					// 锁被别人释放了，现在是自由身，再去申请一次
					continue;
				}
				// 5s 为宽限时间
				// 5s 前你就该过期了消失了，结果你还在那里，说明前人加了锁没有释放（翻车了）
				// 所以我来代他释放
				if (locker.getExpireTime() > System.currentTimeMillis() - 5) {
					// 条件删除锁
					mapper.deleteLocker(locker);
					// 重新尝试加锁
					continue;
				}
				// 锁还是有效的，那就说明加锁失败
				break;
			}
		});
		return holder.value();
	}

	public void unlock(Locker locker) {
		// 条件删除锁
		this.mysql.executeWithMapper(LockerMapper.class, mapper -> {
			mapper.deleteLocker(locker);
		});
	}

	public boolean  with(String id, int seconds, Runnable op) {
		Locker locker = this.lock(id, seconds);
		if (locker == null) {
			return false;
		}
		try {
			op.run();
		} finally {
			this.unlock(locker);
		}
		return true;
	}
}